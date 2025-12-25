package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "family")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String familyName;
    @Column
    private String street;
    @Column
    private String houseNumber;
    @Column
    private String zip;
    @Column
    private String city;
    @Column
    private String phone;
    @Column
    private String email;
    @Column
    private String citizenship;
    @Column
    private String languages;
    @Column(length = 4000)
    private String reasonDescription;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status;
    @Column
    private String notes;

    @ManyToOne
    @JoinColumn(name = "assigned_employee_id", nullable = false)
    private Employee assignedEmployee;

    @Column
    private String zeusId;
    @Column(nullable = false)
    private boolean caseClosed = false;
    @Column
    private LocalDateTime caseClosedAt;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private String createdBy;
    @Column
    private LocalDateTime updatedAt;
    @Column
    private String updatedBy;
    @Column
    private LocalDateTime archivedAt;
    @Column
    private String archivedBy;
    @Column
    private LocalDateTime invalidAt;
    @Column
    private String invalidBy;
    @Column
    private LocalDateTime blockedAt;
    @Column
    private String blockedBy;
    @Column
    private String blockedReason;
    @Column
    private LocalDateTime restoredAt;
    @Column
    private String restoredBy;
    @Column
    private String restoredReason;
    @Column
    private LocalDateTime deletePlannedAt;

    public Family() {
    }

    public Family(Family f) {
        this.id = f.getId();
        this.familyName = f.getFamilyName();
        this.street = f.getStreet();
        this.houseNumber = f.getHouseNumber();
        this.zip = f.getZip();
        this.city = f.getCity();
        this.phone = f.getPhone();
        this.email = f.getEmail();
        this.citizenship = f.getCitizenship();
        this.languages = f.getLanguages();
        this.reasonDescription = f.getReasonDescription();
        this.status = f.getStatus();
        this.notes = f.getNotes();
        this.assignedEmployee = f.getAssignedEmployee();
        this.zeusId = f.getZeusId();
        this.caseClosed = f.isCaseClosed();
        this.caseClosedAt = f.getCaseClosedAt();
        this.createdAt = f.getCreatedAt();
        this.createdBy = f.getCreatedBy();
        this.updatedAt = f.getUpdatedAt();
        this.updatedBy = f.getUpdatedBy();
        this.archivedAt = f.getArchivedAt();
        this.archivedBy = f.getArchivedBy();
        this.invalidAt = f.getInvalidAt();
        this.invalidBy = f.getInvalidBy();
        this.blockedAt = f.getBlockedAt();
        this.blockedBy = f.getBlockedBy();
        this.blockedReason = f.getBlockedReason();
        this.restoredAt = f.getRestoredAt();
        this.restoredBy = f.getRestoredBy();
        this.restoredReason = f.getRestoredReason();
        this.deletePlannedAt = f.getDeletePlannedAt();
    }
}