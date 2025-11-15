package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_member")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FamilyMemberGender gender;
    @Column(nullable = false)
    private LocalDate birthDate;
    @Column(nullable = false)
    private String birthCity;
    @Column(nullable = false)
    private String birthCountry;
    @Column
    private String nationality;
    @Column
    private String languages;
    @Column(nullable = false)
    private boolean livesWithFamily = true;
    @Column
    private String income;
    @Column
    private String workInfo;
    @Column
    private String educationDegree;
    @Column
    private String educationInfo;
    @Column
    private String notes;
    @Column
    private String phone;
    @Column
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false)
    private String createdBy;
    @Column
    private LocalDateTime updatedAt;
    @Column
    private String updatedBy;
    @Column
    private LocalDateTime invalidAt;
    @Column
    private String invalidBy;

    public  FamilyMember() {
    }
}