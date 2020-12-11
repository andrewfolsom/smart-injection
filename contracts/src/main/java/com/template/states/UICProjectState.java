package com.template.states;

import com.template.contracts.UICRequestContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
/*
Version: 1.0.
Simple class for the UICProjectState. For now only includes non-attachments.
 */

@BelongsToContract(UICRequestContract.class)
public class UICProjectState implements LinearState {
    private final UniqueIdentifier linearId;
    private final String UICProjectNumber;
    private final List<AbstractParty> participants;
    private final String projectName;
    private final List<UniqueIdentifier> wellIds;
    private final String status;

    @ConstructorForDeserialization
    public UICProjectState(UniqueIdentifier linearId, String UICProjectNumber, List<AbstractParty> participants,
                            String projectName, List<UniqueIdentifier> wellIds, String status) {
        this.linearId = linearId;
        this.UICProjectNumber = UICProjectNumber;
        this.participants = participants;
        this.projectName = projectName;
        this.wellIds = wellIds;
        this.status = status;
    }

    //For CreateProjectFlow
    public UICProjectState(String projectName, List<AbstractParty> participants) {
        this.linearId = new UniqueIdentifier(projectName);
        this.UICProjectNumber = "NONE";
        this.projectName = projectName;
        this.participants = new ArrayList<>(participants);
        this.wellIds = new ArrayList<>();
        this.status = "Unapproved";
    }

    //For AddRemoveFlow
    public UICProjectState(List<UniqueIdentifier> wellIds, UICProjectState u) {
        this.linearId = u.linearId;
        this.UICProjectNumber = u.UICProjectNumber;
        this.projectName = u.projectName;
        this.participants = u.participants;
        this.wellIds = new ArrayList<>(wellIds);
        this.status = u.status;
    }

    //copy constructor, for updating UIC projectNum
    public UICProjectState(String UICProjectNumber, List<AbstractParty> participants, UICProjectState u) {
        this.linearId = u.linearId;
        this.UICProjectNumber = UICProjectNumber;
        this.participants = new ArrayList<>(participants);
        this.projectName = u.projectName;
        this.wellIds = u.wellIds;
        this.status = u.status;
    }

    // For UICRequestInitiatorFlow
    public UICProjectState(String newStatus, UICProjectState u) {
        this.linearId = u.getLinearId();
        this.UICProjectNumber = u.getUICProjectNumber();
        this.participants = u.getParticipants();
        this.projectName = u.getProjectName();
        this.wellIds = u.getWellIds();
        this.status = newStatus;
    }

    public String getUICProjectNumber() { return UICProjectNumber; }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() { return linearId; }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() { return participants; }

    public List<UniqueIdentifier> getWellIds() { return wellIds; }

    public String getStatus() { return status; }

    public String getProjectName() { return projectName; }

    public void addParticipant(Party newParticipant) { participants.add(newParticipant); }
}
