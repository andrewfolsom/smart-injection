package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.UICRequestContract;
import com.template.contracts.WellContract;
import com.template.states.UICProjectState;
import com.template.states.WellState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.time.LocalDate;
import java.util.*;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class ApproveInitiatorFlow extends FlowLogic<SignedTransaction> {
    private final String externalId;
    private final List<String> APIs;
    private final String uicProjectNumber;
    private final List<String> permits;
    private final List<String> permitExpirations;

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

    public ApproveInitiatorFlow(String externalId, List<String> APIs, String uicProjectNumber, List<String> permits,
                                List<String> permitExpirations) {
        this.externalId = externalId;
        this.APIs = new ArrayList<>(APIs);
        this.uicProjectNumber = uicProjectNumber;
        this.permits = new ArrayList<>(permits);
        this.permitExpirations = new ArrayList<>(permitExpirations);
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
        final Party me = getOurIdentity();

        // Stage 1.
        progressTracker.setCurrentStep(GENERATING_TRANSACTION);

        // Query the well that is to be approved/denied.
        List<String> externalIdList = new ArrayList<>(Collections.singleton(externalId));
        QueryCriteria criteria = new QueryCriteria.LinearStateQueryCriteria(null, null, externalIdList,
                Vault.StateStatus.UNCONSUMED);

        System.out.println(1);

        StateAndRef<UICProjectState> input = getServiceHub().getVaultService().queryBy(UICProjectState.class, criteria)
                .getStates().get(0);

        System.out.println(2);

        // Generate an unsigned transaction.
        UICProjectState oldUICState = input.getState().getData();
        UICProjectState newUICState = new UICProjectState("UIC Approved", uicProjectNumber, oldUICState.getParticipants(),
                oldUICState);

        System.out.println(3);

        criteria = new QueryCriteria.LinearStateQueryCriteria(null, newUICState.getWellIds(), Vault.StateStatus.UNCONSUMED, null);

        System.out.println(4);

        List<StateAndRef<WellState>> wellRefs = getServiceHub().getVaultService().queryBy(WellState.class, criteria)
                .getStates();

        System.out.println(5);

        List<WellState> oldWellStates = new ArrayList<>();
        List<WellState> newWellStates = new ArrayList<>();

        System.out.println(6);

        // Extract WellState data from references
        for(StateAndRef<WellState> well: wellRefs) {
            oldWellStates.add(well.getState().getData());
            System.out.println(7);
        }

        System.out.println(8);

        // Generate new well states with API, permit, permit expiration and UIC project number
        for(int i = 0; i < oldWellStates.size(); i++) {
            System.out.println(8.5);
            WellState newWell = new WellState(APIs.get(i), uicProjectNumber, permits.get(i), permitExpirations.get(i),
                    oldWellStates.get(i));
            System.out.println(9);
            newWellStates.add(newWell);
            System.out.println(10);
        }

        System.out.println(11);
        // Create Approve command for the UICProjectState
        final Command<UICRequestContract.Commands.Approve> uicTxCommand = new Command<>(
                new UICRequestContract.Commands.Approve(),
                Arrays.asList(me.getOwningKey(), newUICState.getParticipants().get(0).getOwningKey())
        );

        System.out.println(12);

        // Create Approve command for WellStates
        final Command<WellContract.Commands.Approve> wellTxCommand = new Command<>(
                new WellContract.Commands.Approve(),
                Arrays.asList(me.getOwningKey(), newUICState.getParticipants().get(0).getOwningKey())
        );

        System.out.println(13);

        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(input)
                .addOutputState(newUICState, UICRequestContract.ID)
                .addCommand(uicTxCommand)
                .addCommand(wellTxCommand);

        System.out.println(14);

        for(StateAndRef<WellState> ref: wellRefs) {
            txBuilder.addInputState(ref);
            System.out.println(15);
        }

        System.out.println(16);

        for(WellState well: newWellStates) {
            txBuilder.addOutputState(well, WellContract.ID);
            System.out.println(17);
        }

        System.out.println(18);

        // Stage 2.
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        // Verify transaction is valid.
        txBuilder.verify(getServiceHub());

        System.out.println(19);

        // Stage 3.
        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        // Sign the transaction.
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        System.out.println(20);

        // Stage 4.
        assert getProgressTracker() != null;
        getProgressTracker().setCurrentStep(GATHERING_SIGS);
        // Send the state to the counterparty, and receive it back with their signature.
        FlowSession otherPartySession = initiateFlow(newUICState.getParticipants().get(0));
        final SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partSignedTx, Collections.singletonList(otherPartySession))
        );

        System.out.println(21);

        // Stage 5.
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        List<FlowSession> sessionList = Collections.singletonList(otherPartySession);
        // Notarise and record the transaction in both parties' vaults.

        System.out.println(22);
        return subFlow(new FinalityFlow(fullySignedTx, sessionList));
    }
}
