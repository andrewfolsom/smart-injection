package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.WellContract;
import com.template.states.WellState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
/*
Version: 1.1
This Flow takes basic information and produces a WellState with the "PROPOSAL" status.
This is the first step, most information will be provided by the flow as most fields are empty.
All dates default to 1999-9-9.
@input: wellName(String), lease(string), location(List<Float>), locationType(String)
@defaults: status("PROPOSAL"), spudDate(1999,9,9), API("NONE"), UICProjectNumber("NONE"), permit("NONE"),
           permitExpiration(1999,9,9):
@output: WellState

Example run Command:
start ProposeWellFlow wellName: "My well", lease: "Your Field", location: [27.777, 39.11], locationType: "NAT27"
*/

@InitiatingFlow
@StartableByRPC
public class ProposeWellFlow extends FlowLogic<SignedTransaction> {

    //progress tracker created, but not used here.
    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    //PROPERTIES
    private final String wellName;
    private final String lease;
    private final Party calGem;
    private final List<Float> location;
    private final String locationType;
    private final SecureHash.SHA256 wellBoreDiagram;

    //CONSTRUCTOR
    public ProposeWellFlow(String wellName, String lease, Party calGem, List<Float> location, String locationType,
                           SecureHash.SHA256 docs) {
        this.wellName = wellName;
        this.lease = lease;
        this.calGem = calGem;
        this.location = location;
        this.locationType = locationType;
        this.wellBoreDiagram = docs;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        //LOCAL PROPERTIES
        final Party owner = getOurIdentity();
        final Party operator = getOurIdentity();
        final String API = "N/A";
        final String status = "Proposed";
        final String UICProjectNumber = "N/A";
        final String permit = "N/A";
        final LocalDate spudDate = LocalDate.of(1999,9,9);
        final LocalDate permitExpiration = LocalDate.of(1999,9,9);
        final String projectName = "N/A";

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        //generate output state
        WellState output = new WellState(status, wellName, owner, operator, calGem, lease, locationType, location,
                spudDate, API, UICProjectNumber, permit, permitExpiration, wellBoreDiagram, projectName);

        //Create and build the builder
        final TransactionBuilder builder = new TransactionBuilder(notary);
        builder.addAttachment(wellBoreDiagram);
        builder.addOutputState(output, WellContract.ID);
        builder.addCommand(new WellContract.Commands.Propose(), Collections.singletonList(owner.getOwningKey()));
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
