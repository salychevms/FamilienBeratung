package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
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

    @Column(nullable = false, length = 150)
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
    @Column
    private String reasonDescription;
    @Column
    private String status;
    @Column
    private String notes;

    @ManyToOne
    @JoinColumn(name = "assigned_employee_id")
    private Employee assignedEmployee;

    @Column(nullable = false)
    private boolean caseClosed = false;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false)
    private String createdBy;
    @Column
    private LocalDateTime updatedAt;
    @Column
    private String updatedBy;
    @Column(nullable = false)
    private boolean deleted;
    @Column
    private LocalDate deletedAt;

    public Family() {
    }
}