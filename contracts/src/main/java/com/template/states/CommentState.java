package com.template.states;

import com.template.contracts.CommentContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(CommentContract.class)
public class CommentState implements ContractState {

    //required - provided by well operator, proposal
    private final String wellName;
    private final String comment;
    private final Boolean acknowledged;
    private final Party sender;
    private final Party receiver;

    @ConstructorForDeserialization
    public CommentState(String wellName, String comment, Boolean acknowledged, Party sender, Party receiver) {
        this.wellName = wellName;
        this.comment = comment;
        this.acknowledged = acknowledged;
        this.sender = sender;
        this.receiver = receiver;
    }

    public CommentState(Boolean acknowledged, CommentState c) {
        this.wellName = c.wellName;
        this.comment = c.comment;
        this.acknowledged = acknowledged;
        this.sender = c.sender;
        this.receiver = c.receiver;
    }

    public Boolean getAcknowledged() { return acknowledged; }
    public String getComment() { return comment; }
    public String getWellName() { return wellName; }
    public Party getSender() { return sender; }
    public Party getReceiver() { return receiver; }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender, receiver);
    }

}
