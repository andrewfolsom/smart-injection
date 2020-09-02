package com.template.contracts;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

public class WellContract implements Contract {

    public static final String ID = "com.template.contracts.WellContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {
        //In our hello-world app, We will only have one command.
        class Propose implements Commands {}
        class Update implements Commands {}
    }
}
