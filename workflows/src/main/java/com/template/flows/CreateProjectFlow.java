package com.template.flows;


import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.UICRequestContract;
import com.template.states.UICProjectState;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Collections;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class CreateProjectFlow extends FlowLogic<SignedTransaction> {

    //progress tracker created, but not used here.
    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    //PROPERTIES
    private final String projectName;

    //CONSTRUCTOR
    public CreateProjectFlow(String projectName) {
        this.projectName = projectName;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        //LOCAL PROPERTIES
        final Party operator = getOurIdentity();
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //generate output state
        UICProjectState output = new UICProjectState(projectName, Collections.singletonList(operator));

        //Create and build the builder
        final TransactionBuilder builder = new TransactionBuilder(notary);
        builder.addOutputState(output, UICRequestContract.ID);
        builder.addCommand(new UICRequestContract.Commands.Create(), Collections.singletonList(operator.getOwningKey()));
        builder.verify(getServiceHub());

        //convert builder -> SignedTransaction
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(builder);
        //Finality flow needs a session list. This flow doesn't have other nodes that it needs to talk to
        //so we create an empty session list to pass.
        List<FlowSession> session = Collections.emptyList();
        //finalize, add to ledger.
        return subFlow(new FinalityFlow(signedTransaction, session));
    }
}

