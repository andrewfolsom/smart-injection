package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.WellContract;
import com.template.states.WellState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class AddRemoveWellFlow extends FlowLogic<SignedTransaction> {

    private final ProgressTracker progressTracker = new ProgressTracker();
    @Override
    public ProgressTracker getProgressTracker() { return progressTracker; }

    private final List<String> externalIdList;
    private final List<String> updateList;


    public AddRemoveWellFlow(List<String> externalIds, List<String> updates) {
        this.externalIdList = new ArrayList<>(externalIds);
        this.updateList = new ArrayList<>(updates);
    }

    public WellState copyState(String update, WellState d) {
        return new WellState(d.getLinearId(), d.getStatus(), d.getWellName(), d.getOwner(),
                d.getOperator(), d.getCalGem(), d.getLease(), d.getLocationType(), d.getLocation(),
                d.getSpudDate(), d.getAPI(), d.getUICProjectNumber(), d.getPermit(), d.getPermitExpiration(),
                d.getParticipants(), update);
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        //Make sure lists of well and lists of updates have the same number.
        if (externalIdList.size() != updateList.size())
            throw new FlowException("Number of wells needs to match number of updates.");

        QueryCriteria criteria;
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        final TransactionBuilder builder = new TransactionBuilder(notary);
        final Party operator = getOurIdentity();

        for (int i = 0; i < externalIdList.size(); i++) {
            //Set criteria
            criteria = new QueryCriteria.LinearStateQueryCriteria(null,null,
                    Collections.singletonList(externalIdList.get(i)), Vault.StateStatus.UNCONSUMED);

            //Grab well, this will be the input
            StateAndRef<WellState> input = getServiceHub().getVaultService()
                    .queryBy(WellState.class, criteria).getStates().get(0);
            //add input to builder
            builder.addInputState(input);
            //create output with modified UICProjectNum
            WellState output = copyState(updateList.get(i), input.getState().getData());
            //add output to builder
            builder.addOutputState(output, WellContract.ID);
        }

//        StateAndRef<WellState> input = getServiceHub().getVaultService()
//                .queryBy(WellState.class, criteria).getStates().get(0);
//
//        //this info is not changed by this flow so just copies it from the previous state.
//        final UniqueIdentifier linearId = input.getState().getData().getLinearId();
//        final String status = input.getState().getData().getStatus();
//        final String wellName = input.getState().getData().getWellName();
//        final Party owner = input.getState().getData().getOwner();
//        final Party operator = input.getState().getData().getOperator();
//        final Party calGem = input.getState().getData().getCalGem();
//        //this info is only changeable by calGEM so is copied from previous state.
//        final String API = input.getState().getData().getAPI();
//        final String UICProjectNumber = input.getState().getData().getUICProjectNumber();
//        final String permit = input.getState().getData().getPermit();
//        final LocalDate permitExpiration = input.getState().getData().getPermitExpiration();
//        final List<AbstractParty> participants = input.getState().getData().getParticipants();
//
//        WellState output = new WellState(linearId, status, wellName, owner, operator, calGem, lease, locationType, location,
//                spudDate, API, UICProjectNumber, permit, permitExpiration, participants);

//        builder.addInputState(input);
//        builder.addOutputState(output, WellContract.ID);
//        builder.addCommand(new WellContract.Commands.Update(), Collections.singletonList(owner.getOwningKey()));
        builder.addCommand(new WellContract.Commands.Update(), Collections.singletonList(operator.getOwningKey()));
        builder.verify(getServiceHub());

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(builder);
        List<FlowSession> session = Collections.emptyList();

        return subFlow(new FinalityFlow(signedTransaction, session));
    }
}

