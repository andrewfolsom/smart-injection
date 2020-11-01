package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.states.CommentState;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatedBy(CommentInitiatorFlow.class)
public class CommentResponseFlow extends FlowLogic<SignedTransaction> {
    private final FlowSession counterpartySession;

    public CommentResponseFlow(FlowSession counterpartySession) { this.counterpartySession = counterpartySession; }
    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Responder flow logic goes here.
        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession counterpartyFlow) {
                super(counterpartyFlow);
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) {
                requireThat(require -> {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    require.using("This must be a Comment transaction.", output instanceof CommentState);
                    assert output instanceof CommentState;
                    return null;
                });
            }
        }

        final SignTxFlow signTxFlow = new SignTxFlow(counterpartySession);
        final SecureHash txId = subFlow(signTxFlow).getId();

        return subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
    }
}
