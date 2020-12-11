package com.template.contracts;

import com.template.states.UICProjectState;
import com.template.states.WellState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.List;

// ************
// * Contract *
// ************
public class UICRequestContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.template.contracts.UICRequestContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {

        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigner = command.getSigners();
        CommandData commandType = command.getValue();

        if(commandType instanceof Commands.Approve) {
            // Shape Constraints
            if (tx.getInputStates().size() != 1) {
                throw new IllegalArgumentException("Approve must have 1 input.");
            }

            if (tx.getOutputStates().size() != 2) {
                throw new IllegalArgumentException("Approve must have 2 outputs.");
            }

            // Content Constraints
            WellState output1 = (WellState) tx.getOutput(0);
            UICProjectState output2 = (UICProjectState) tx.getOutput(1);

            // UIC specific constraints
            if (output2.getUICProjectNumber() == null)
                throw new IllegalArgumentException("UIC project number must be provided");

            // Required Signers constraints;
            Party operator = output1.getOperator();
            Party calGem = output1.getCalGem();
            PublicKey operatorKey = operator.getOwningKey();
            if (!(requiredSigner.contains(operatorKey))) {
                throw new IllegalArgumentException("Well Operator must sign the approved transaction");
            }
            if (!(requiredSigner.contains(calGem.getOwningKey()))) {
                throw new IllegalArgumentException("CalGEM must sign the approved transaction");
            }
        }
        else if (commandType instanceof Commands.Create) {
            if (tx.getInputStates().size() != 0) { throw new IllegalArgumentException("Approve must have 0 input."); }
            if (tx.getOutputStates().size() != 1) { throw new IllegalArgumentException("Approve must have 1 outputs."); }
            UICProjectState output1 = (UICProjectState) tx.getOutput(0);
            if (!(output1.getUICProjectNumber().equals("NONE")))
                throw new IllegalArgumentException("UIC project number must be NONE");
        }
        else if (commandType instanceof Commands.Update) {

        }

    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        class Approve implements Commands {}
        class Create implements Commands {}
        class Request implements Commands {}
        class Update implements Commands {}
    }
}