package com.salychevms.familienberatung.model;

import com.salychevms.familienberatung.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Embeddable
@Getter
@Setter
public class ExitDataEsf {

    @Column(nullable = false)
    private boolean newlyRegisteredAsJobSeeker;

    @Enumerated(EnumType.STRING)
    private EducationAfterExitType educationAfterExitType;

    @Enumerated(EnumType.STRING)
    private VocationalTrainingType vocationalTrainingType;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentAfterExit;

    @Enumerated(EnumType.STRING)
    private WorkingHoursType workingHoursAfterExit;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="esf_exit_contract_types", joinColumns = @JoinColumn(name = "member_esf_id"))
    @Column(name = "contract_type")
    @Enumerated(EnumType.STRING)
    private Set<ContractType> contractAfterExit = new HashSet<>();

    @Column(nullable = false)
    private boolean qualificationAchieved;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="esf_exit_qualification_proofs", joinColumns = @JoinColumn(name = "member_esf_id"))
    @Column(name = "qualification_proof_type")
    @Enumerated(EnumType.STRING)
    private Set<QualificationProofType> qualificationProofs = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private EmploymentExtensionResult employmentExtensionResult;

    @Enumerated(EnumType.STRING)
    private SgbAfterExitType sgbAfterExitType;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown sgbIXAfterExit;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="esf_exit_financial_benefits", joinColumns = @JoinColumn(name = "member_esf_id"))
    @Column(name = "benefit_type")
    @Enumerated(EnumType.STRING)
    private Set<BenefitType> financialBenefits = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="esf_exit_support_types", joinColumns = @JoinColumn(name = "member_esf_id"))
    @Column(name = "support_type")
    @Enumerated(EnumType.STRING)
    private Set<SupportType> nonFinancialSupport = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private YesNoUnknown familyStabilized;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="esf_exit_barriers", joinColumns = @JoinColumn(name = "member_esf_id"))
    @Column(name="barrier_type")
    @Enumerated(EnumType.STRING)
    private Set<BarrierType> barriersOvercome = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private YesNoUnknown serviceAccessBarriersOvercome;

    @Enumerated(EnumType.STRING)
    private ProjectTerminationReason terminationReason;
}
