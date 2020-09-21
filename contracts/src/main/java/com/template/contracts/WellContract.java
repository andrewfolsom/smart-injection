package com.template.contracts;


import com.template.states.UICProjectState;
import com.template.states.WellState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.List;

// ************
// * Contract *
// ************
public class WellContract implements Contract {

    public static String ID = "com.template.contracts.WellContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        if (tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");
        Command command = tx.getCommand(0);
        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if (commandType instanceof Commands.Propose) {
            // Status: Proposal - Well operator is propose to create a well

            // "Shape" constraints (Numbers of input/output).
            if (tx.getInputStates().size() != 0)
                throw new IllegalArgumentException("Propose process must have no input.");
            if (tx.getOutputStates().size() != 1)
                throw new IllegalArgumentException("Propose process must have one output.");

            // Contents constraints (Check for valid value/data type).
            ContractState outputState = tx.getOutput(0);
            WellState wellState = (WellState) outputState;

            // Required Signers constraints.
            Party operator = wellState.getOperator();
            PublicKey operatorKey = operator.getOwningKey();
            if (!(requiredSigners.contains(operatorKey)))
                throw new IllegalArgumentException("The operator of the well must sign the proposal.");

        } else if (commandType instanceof Commands.Update) {
            /* Status: Update - Well operator is making update to the well before submit for approval. */
            // "Shape" constraints
            if (tx.getInputStates().size() == 0)
                throw new IllegalArgumentException("Update process must have at least 1 input.");
            if (tx.getOutputStates().size() == 0)
                throw new IllegalArgumentException("Update process must not have empty output.");

            // Contents constraints (Check for valid value/data type).
            ContractState input = tx.getInput(0);
            ContractState output = tx.getOutput(0);

            WellState inputWell = (WellState) input;
            WellState outputWell = (WellState) output;

            if (!(input instanceof WellState))
                throw new IllegalArgumentException("Input must be a WellState.");
            if (!(output instanceof WellState))
                throw new IllegalArgumentException("Output must be a WellState.");

            if (inputWell.getStatus().equals("APPROVED")) {
                /* Checking the integrity of the WellState after approval update. */

                if (!(inputWell.getWellName().equals(outputWell.getWellName())))
                    throw new IllegalArgumentException("After the well creation approval process, the name of the well cannot be changed.");
                if (!(inputWell.getLease().equals(outputWell.getLease())))
                    throw new IllegalArgumentException("After the well creation approval process, the lease status of the well cannot be changed.");
                if (!(inputWell.getLocationType().equals(outputWell.getLocationType())))
                    throw new IllegalArgumentException("After the well creation approval process, the LocationType of the well cannot be changed.");
                if (!(inputWell.getLocation().equals(((WellState) output).getLocation())))
                    throw new IllegalArgumentException("After the well creation approval process, the location of the well cannot be changed.");
                if (!(inputWell.getAPI().equals(outputWell.getAPI())))
                    throw new IllegalArgumentException("After the well creation approval process, the API of the well cannot be changed.");
                if (!(inputWell.getPermit().equals(outputWell.getPermit())))
                    throw new IllegalArgumentException("After the well creation approval process, the permit of the well cannot be changed.");
                if (!(inputWell.getPermitExpiration().equals(outputWell.getPermitExpiration())))
                    throw new IllegalArgumentException("After the well creation approval process, the PermitExpiration of the well cannot be changed.");
                if (!(inputWell.getUICProjectNumber().equals(outputWell.getUICProjectNumber())))
                    throw new IllegalArgumentException("After the well creation approval process, the UIC Project Number of the well cannot be changed.");
            }

            // "Required Signers constraints.
            Party operator = outputWell.getOperator();
            PublicKey operatorKey = operator.getOwningKey();
            if (!(requiredSigners.contains(operatorKey)))
                throw new IllegalArgumentException("Well Operator must sign the update of the contract.");

        } else if (commandType instanceof Commands.Request) {
            // Well operator requesting UIC review.

            // Shape Constraints: 1 input, 1 output;
            if (tx.getInputStates().size() != 1) {
                throw new IllegalArgumentException("Must contain exactly 1 input.");
            }

            if (tx.getOutputStates().size() != 1) {
                throw new IllegalArgumentException("Must contain exactly 1 output.");
            }

            // Content Constraints
            // Check that all well fields, other than status, are unchanged.
            WellState inputWell = (WellState) tx.getInput(0);
            WellState outputWell = (WellState) tx.getOutput(0);

            if (!outputWell.sameAs(inputWell)) {
                throw new IllegalArgumentException("Unauthorized changes made to well data.");
            }

            // Check that the permit, Aquifer Exemption, and AoC fields are included
            if (inputWell.getPermit() == null) {
                throw new IllegalArgumentException("Permit must be included.");
            }

        } else if (commandType instanceof Commands.Deny) {
            // CalGEM denying a submitted UIC request.

            // Shape Constraints: 1 input, 1 output;
            if (tx.getInputStates().size() != 1) {
                throw new IllegalArgumentException("Must contain exactly 1 input.");
            }

            if (tx.getOutputStates().size() != 1) {
                throw new IllegalArgumentException("Must contain exactly 1 output.");
            }

            // Content Constraints
            // Check that all well fields, other than status, are unchanged.
            WellState inputWell = (WellState) tx.getInput(0);
            WellState outputWell = (WellState) tx.getOutput(0);

            if (!outputWell.sameAs(inputWell)) {
                throw new IllegalArgumentException("Unauthorized changes made to well data.");
            }

        } else if (commandType instanceof Commands.Approve) {

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

            // Well specific constraints
            if (output1.getAPI() == null)
                throw new IllegalArgumentException("API must be provided at approval.");
            if (output1.getPermit() == null)
                throw new IllegalArgumentException("Permit must be provided at approval.");
            if (output1.getPermitExpiration() == null)
                throw new IllegalArgumentException("Permit expiration must be provided at approval.");

            // Required Signers constraints;
            Party operator = output1.getOperator();
            PublicKey operatorKey = operator.getOwningKey();
            if (!(requiredSigners.contains(operatorKey))) {
                throw new IllegalArgumentException("Well Operator must sign the approved transaction");
            }

        } else {
            throw new IllegalArgumentException("Command type not recognized.");
        }
    }

    public interface Commands extends CommandData {
            class Propose implements Commands {}
            class Update implements Commands {}
            class Request implements Commands {}
            class Deny implements Commands {}
            class Approve implements Commands {}
    }

}

