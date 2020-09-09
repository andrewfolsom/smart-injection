package com.template.states;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
/*
Version: 1.0.
Simple class for the UICProjectState. For now only includes non-attachments.
 */
public class UICProjectState implements LinearState {
    private final UniqueIdentifier linearId;
    private final String UICProjectNumber;

    public UICProjectState(String uicProjectNumber) {
        this.linearId = new UniqueIdentifier(uicProjectNumber);
        UICProjectNumber = uicProjectNumber;
    }

    public String getUICProjectNumber() { return UICProjectNumber; }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() { return linearId; }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() { return Collections.emptyList(); }
}
