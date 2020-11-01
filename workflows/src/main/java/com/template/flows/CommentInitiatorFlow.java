package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.CommentContract;
import com.template.states.CommentState;
import net.corda.core.contracts.Command;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class CommentInitiatorFlow extends FlowLogic<SignedTransaction> {
    private final String wellName;
    private final String comment;
    private final Boolean acknowledged;
    private final Party receivingParty;

    public CommentInitiatorFlow(String wellName, String comment, Party receivingParty) {
        this.wellName = wellName;
        this.comment = comment;
        this.acknowledged = false;
        this.receivingParty = receivingParty;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        final Party sender = getOurIdentity();
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        // Generate an unsigned transaction.
        CommentState newCommentState = new CommentState(wellName, comment, acknowledged, sender, receivingParty);
        final Command<CommentContract.Commands.Create> txCommand = new Command<>(
                new CommentContract.Commands.Create(),
                Arrays.asList(sender.getOwningKey(), receivingParty.getOwningKey())
        );

        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addOutputState(newCommentState, CommentContract.ID)
                .addCommand(txCommand);

        txBuilder.verify(getServiceHub());

        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        FlowSession otherPartySession = initiateFlow(receivingParty);
        final SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partSignedTx, Collections.singletonList(otherPartySession))
        );

        List<FlowSession> sessionList = Collections.singletonList(otherPartySession);
        // Notarise and record the transaction in both parties' vaults.
        return subFlow(new FinalityFlow(fullySignedTx, sessionList));
    }
}
