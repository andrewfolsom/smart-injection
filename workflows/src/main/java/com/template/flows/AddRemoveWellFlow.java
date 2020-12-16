package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.template.contracts.UICRequestContract;
import com.template.contracts.WellContract;
import com.template.states.UICProjectState;
import com.template.states.WellState;
import net.corda.core.contracts.Command;
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
    private List<UniqueIdentifier> wellIds;
    private final String projectName;


    public AddRemoveWellFlow(String projectName, List<String> externalIds, List<String> updates) {
        this.externalIdList = new ArrayList<>(externalIds);
        this.updateList = new ArrayList<>(updates);
        this.projectName = projectName;
        this.wellIds = new ArrayList<>();
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

        WellContract.Commands.AddRemove commandOne = new WellContract.Commands.AddRemove();
        UICRequestContract.Commands.Update commandTwo = new UICRequestContract.Commands.Update();

        Collections.singletonList(operator.getOwningKey());
        List<PublicKey> requiredSigners = Collections.singletonList(operator.getOwningKey());
        Command<WellContract.Commands.AddRemove> wellCommand = new Command<>(commandOne, requiredSigners);
        Command<UICRequestContract.Commands.Update> uicCommand = new Command<>(commandTwo, requiredSigners);


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
            //builder.addCommand(wellCommand);
            if (!(updateList.get(i).equals("NONE"))) {
                wellIds.add(input.getState().getData().getLinearId());
            }
        }
        criteria = new QueryCriteria.LinearStateQueryCriteria(null,null,
                Collections.singletonList(projectName), Vault.StateStatus.UNCONSUMED);

        StateAndRef<UICProjectState> input = getServiceHub().getVaultService()
                .queryBy(UICProjectState.class, criteria).getStates().get(0);
        builder.addInputState(input);

        UICProjectState UICoutput = new UICProjectState(wellIds, input.getState().getData());
        builder.addOutputState(UICoutput);
        builder.addCommand(uicCommand);
        builder.addCommand(wellCommand);

//        builder.addCommand(commandOne, Collections.singletonList(operator.getOwningKey()));
//        builder.addCommand(commandTwo, Collections.singletonList(operator.getOwningKey()));
        builder.verify(getServiceHub());

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(builder);
        List<FlowSession> session = Collections.emptyList();

        return subFlow(new FinalityFlow(signedTransaction, session));
    }
}

