package com.template.states;

import com.template.contracts.WellContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
/*
Version: 1.1
State is used to store the relevant information for the well states. Some properties are filled out by the well
operator,some by CalGEM. Spud Date would be added after the Well is approved, and by the well operator.
Each WellState has an LinearId, which is created with an externalId. linearId = UniqueIdentifier(wellName)
ExternalId = wellName.
@input: status(String), wellName(String), owner(Party), operator(Party), lease(String), locationType(String),
        location(List<Float>), spudDate(LocalDate), API(String), UICProjectNumber(String), permit(String),
        permitExpiration(LocalDate)

Example query:
run vaultQuery contractStateType: com.template.states.WellState
 */

@BelongsToContract(WellContract.class)
public class WellState implements LinearState {
//------------------------------------------- PROPERTIES --------------------------------------------------------------

    private final UniqueIdentifier linearId;    //required - provided by corda, proposal
    private final String status;                //required - provided by respective flow

    //required - provided by well operator, proposal
    private final String wellName;
    private final Party owner;
    private final Party operator;
    private final String lease;
    private final String locationType;
    private final List<Float> location;
//    private final SecureHash.SHA256 wellBoreDiagram = null;
//    private final SecureHash.SHA256 areaOfReview = null;
//    private final Date areaOfReviewDate = null;
//    private final SecureHash.SHA256 noticeOfIntent = null;
//    private final Date noticeOfIntentExpiration = null;

    //required - provided by CalGem, approval process
    private final String API;
    private final String UICProjectNumber;
    private final String permit;
    private final LocalDate permitExpiration;

    //optional - added by operator after approval
    private final LocalDate spudDate;

    //optional
//    private final SecureHash.SHA256 supportDocuments;
//    private final SecureHash.SHA256 aquiferExemption;
//    private final UniqueIdentifier reworkLinearID;

// ------------------------------------------- PROPERTIES END----------------------------------------------------------
    //CONSTRUCTORS
    //for well states that have already been created. Used by UpdateWellFlow
    @ConstructorForDeserialization
    public WellState(UniqueIdentifier linearId, String status, String wellName, Party owner, Party operator, String lease, String locationType,
                     List<Float> location, LocalDate spudDate, String API, String UICProjectNumber,
                     String permit, LocalDate permitExpiration) {
        this.linearId = linearId;
        this.status = status;
        this.wellName = wellName;
        this.owner = owner;
        this.operator = operator;
        this.lease = lease;
        this.locationType = locationType;
        this.location = location;
        this.spudDate = spudDate;
        this.API = API;
        this.UICProjectNumber = UICProjectNumber;
        this.permit = permit;
        this.permitExpiration = permitExpiration;
    }

    //new well states
    public WellState(String status, String wellName, Party owner, Party operator, String lease, String locationType,
                     List<Float> location, LocalDate spudDate, String API, String UICProjectNumber,
                     String permit, LocalDate permitExpiration) {
        this.linearId = new UniqueIdentifier(wellName);
        this.status = status;
        this.wellName = wellName;
        this.owner = owner;
        this.operator = operator;
        this.lease = lease;
        this.locationType = locationType;
        this.location = location;
        this.spudDate = spudDate;
        this.API = API;
        this.UICProjectNumber = UICProjectNumber;
        this.permit = permit;
        this.permitExpiration = permitExpiration;
    }

    //GETTERS
    public String getStatus() { return status; }
    public String getWellName() { return wellName; }
    public Party getOwner() { return owner; }
    public Party getOperator() { return operator; }
    public String getLease() { return lease; }
    public String getLocationType() { return locationType; }
    public List<Float> getLocation() { return location; }
    public LocalDate getSpudDate() { return spudDate; }
    public String getAPI() { return API; }
    public String getUICProjectNumber() { return UICProjectNumber; }
    public String getPermit() { return permit; }
    public LocalDate getPermitExpiration() { return permitExpiration; }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Collections.singletonList(operator);
    }
}
