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
public class EntryDataEsf {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    private WorkingHoursType workingHoursType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="esf_entry_contract_types", joinColumns = @JoinColumn(name = "member_esf_id"))
    @Column(name = "contract_type")
    @Enumerated(EnumType.STRING)
    private Set<ContractType> contractTypes=new HashSet<>();

    @Enumerated(EnumType.STRING)
    private UnemploymentDuration unemploymentDuration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchoolDegree schoolDegree;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProfessionalDegree professionalDegree;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown germanCitizenship;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown migrantBackground;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown thirdCountryNational;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown minority;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown disability;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown fixedResidence;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown threatenedByHomelessness;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown additionalSgbSupport;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown sgbIXBenefits;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown singleParent;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="esf_entry_child_age_groups", joinColumns = @JoinColumn(name = "member_esf_id"))
    @Column(name = "child_age_group")
    @Enumerated(EnumType.STRING)
    private Set<ChildAgeGroup> childAgeGroups = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private YesNoUnknown increasedCareNeed;

    @Enumerated(EnumType.STRING)
    private YesNoUnknown anotherWorkingPersonInHousehold;
}
