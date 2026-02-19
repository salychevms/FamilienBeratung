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
    //TODO: delete
    @Column
    private String citizenship;
    //TODO: delete
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
}