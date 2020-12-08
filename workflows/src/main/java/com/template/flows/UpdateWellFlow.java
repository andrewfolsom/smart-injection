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
import net.corda.core.node.services.vault.QueryCriteria.LinearStateQueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/*
version 1.0
This flow allows a well operator to update an already created WellState.
@input: externalId(String), lease(String), location(List<Float>), locationType(String), spudDate(List<Integer>)
@defaults: wellName, status, API, UICProjectNumber, permit and permitExpiration are copied from the previous state.
@output: WellState

The externalId = well name.
Plan to allows only some fields to be input so the operator does not have to retype things they do not plan on changing.
 */

@InitiatingFlow
@StartableByRPC
public class UpdateWellFlow extends FlowLogic<SignedTransaction> {

    private final ProgressTracker progressTracker = new ProgressTracker();
    @Override
    public ProgressTracker getProgressTracker() { return progressTracker; }

    private final QueryCriteria criteria;
    private final String lease;
    private final List<Float> location;
    private final String locationType;
    private final LocalDate spudDate;

    public UpdateWellFlow(String externalId, String lease, List<Float> location, String locationType,
                          List<Integer> spudDate) {

        //Assume only one input. Create a singleton list of the externalId passed to the flow.
        //This sets up the search criteria for the vault query.
        List<String> externalIdList = new ArrayList<>(Collections.singleton(externalId));
        this.criteria = new LinearStateQueryCriteria(null,null, externalIdList,
                Vault.StateStatus.UNCONSUMED);
        this.lease = lease;
        this.location = location;
        this.locationType = locationType;
        this.spudDate = LocalDate.of(spudDate.get(0),spudDate.get(1),spudDate.get(2));
    }

    public WellState copyState(String lease, String locationType, List<Float> location, LocalDate spudDate,
                               WellState d) {
        return new WellState(d.getLinearId(), d.getStatus(), d.getWellName(), d.getOwner(),
                d.getOperator(), d.getCalGem(), lease, locationType, location,
                spudDate, d.getAPI(), d.getProjectName(), d.getPermit(), d.getPermitExpiration(),
                d.getParticipants(), d.getProjectName());
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        //grab the state from the vault.
        StateAndRef<WellState> input = getServiceHub().getVaultService()
                .queryBy(WellState.class, criteria).getStates().get(0);
        //grab owner
        final Party owner = input.getState().getData().getOwner();

        //notary
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //output state
        WellState output = copyState(lease, locationType, location, spudDate, input.getState().getData());

        //start builder
        final TransactionBuilder builder = new TransactionBuilder(notary);

        builder.addInputState(input);
        builder.addOutputState(output, WellContract.ID);
        builder.addCommand(new WellContract.Commands.Update(), Collections.singletonList(owner.getOwningKey()));
        builder.verify(getServiceHub());

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(builder);
        List<FlowSession> session = Collections.emptyList();

        return subFlow(new FinalityFlow(signedTransaction, session));
    }
}
