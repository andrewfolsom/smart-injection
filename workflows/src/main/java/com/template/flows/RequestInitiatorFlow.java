package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.WellContract;
import com.template.states.WellState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class RequestInitiatorFlow extends FlowLogic<SignedTransaction> {
    private final Party calGem;
    private final String externalId;

    private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new Well.");
    private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
    private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with private key.");
    private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering counterparty signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    private final ProgressTracker progressTracker = new ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
    );

    public RequestInitiatorFlow(String externalId, Party calGem) {
        this.calGem = calGem;
        this.externalId = externalId;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Initiator flow logic goes here.

        final Party notary  = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Stage 1.
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);

        // Query the well that is to be approved/denied.
        List<String> externalIdList = new ArrayList<>(Collections.singleton(externalId));
        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(null, null, externalIdList,
                Vault.StateStatus.UNCONSUMED);

        StateAndRef<WellState> input = getServiceHub().getVaultService().queryBy(WellState.class, criteria)
                .getStates().get(0);

        // Generate an unsigned transaction.
        WellState oldState = input.getState().getData();
        List<AbstractParty> updatedParticipants = new ArrayList<>(oldState.getParticipants());
        updatedParticipants.add(calGem);
        WellState newState = new WellState(updatedParticipants,oldState);
        final Command<WellContract.Commands.Request> txCommand = new Command<>(
                new WellContract.Commands.Request(),
                Arrays.asList(newState.getOperator().getOwningKey(), newState.getCalGem().getOwningKey())
        );

        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(input)
                .addOutputState(newState, WellContract.ID)
                .addCommand(txCommand);

        // Stage 2.
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        // Verify transaction is valid.
        txBuilder.verify(getServiceHub());

        // Stage 3.
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        // Sign the transaction.
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Stage 4.
        assert getProgressTracker() != null;
        getProgressTracker().setCurrentStep(GATHERING_SIGS);
        // Send the state to the counterparty, and receive it back with their signature.
        FlowSession otherPartySession = initiateFlow(calGem);
        final SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partSignedTx, Collections.singletonList(otherPartySession)));

        // Stage 5.
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        // Notarise and record the transaction in both parties' vaults.
        return subFlow(new FinalityFlow(fullySignedTx, Collections.singletonList(otherPartySession)));
    }
}
