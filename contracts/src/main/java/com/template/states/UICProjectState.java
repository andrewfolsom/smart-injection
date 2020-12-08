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
    private List<AbstractParty> participants;
    private String projectName;

    @ConstructorForDeserialization
    public UICProjectState(UniqueIdentifier linearId, String UICProjectNumber, List<AbstractParty> participants,
                            String projectName) {
        this.linearId = linearId;
        this.UICProjectNumber = UICProjectNumber;
        this.participants = participants;
        this.projectName = projectName;
    }
    //Should not be used by ApproveInitiatorFlow.
    public UICProjectState(String projectName, List<AbstractParty> participants) {
        this.linearId = new UniqueIdentifier(projectName);
        this.UICProjectNumber = "NONE";
        this.projectName = projectName;
        this.participants = new ArrayList<>(participants);
    }

    //copy constructor, for updating UIC projectNum
    public UICProjectState(String UICProjectNumber, List<AbstractParty> participants, UICProjectState u) {
        this.linearId = u.linearId;
        this.UICProjectNumber = UICProjectNumber;
        this.participants = new ArrayList<>(participants);
        this.projectName = u.projectName;
    }

    public String getUICProjectNumber() { return UICProjectNumber; }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() { return linearId; }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() { return participants; }
}
