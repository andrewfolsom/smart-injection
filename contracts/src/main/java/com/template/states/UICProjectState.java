package com.template.states;

import com.template.contracts.UICRequestContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
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

    public UICProjectState(String UICProjectNumber, List<AbstractParty> participants) {
        this.linearId = new UniqueIdentifier(UICProjectNumber);
        this.UICProjectNumber = UICProjectNumber;
        this.participants = new ArrayList<>(participants);
    }

    public String getUICProjectNumber() { return UICProjectNumber; }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() { return linearId; }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() { return participants; }
}
