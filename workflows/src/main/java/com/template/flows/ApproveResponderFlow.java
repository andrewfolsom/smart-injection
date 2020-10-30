package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.states.WellState;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

// ******************
// * Responder flow *
// ******************
@InitiatedBy(ApproveInitiatorFlow.class)
public class ApproveResponderFlow extends FlowLogic<SignedTransaction> {
    private final FlowSession counterpartySession;

    public ApproveResponderFlow(FlowSession counterpartySession) { this.counterpartySession = counterpartySession; }
    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Responder flow logic goes here.
        class SignTxFlow extends SignTransactionFlow {
            private SignTxFlow(FlowSession counterpartyFlow, ProgressTracker progressTracker) {
                super(counterpartyFlow, progressTracker);
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) {
                requireThat(require -> {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    require.using("This must be a Well transaction.", output instanceof WellState);
                    assert output instanceof WellState;
                    return null;
                });
            }
        }

        final SignTxFlow signTxFlow = new SignTxFlow(counterpartySession, SignTransactionFlow.Companion.tracker());
        final SecureHash txId = subFlow(signTxFlow).getId();

        return subFlow(new ReceiveFinalityFlow(counterpartySession, txId));
    }
}
