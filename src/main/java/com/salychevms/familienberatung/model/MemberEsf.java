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
public class MemberEsfDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "family_id", nullable = false)
    private Member member;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberGender gender;
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
    @Column(length = 4000)
    private String workInfo;
    @Column
    private String educationDegree;
    @Column(length = 4000)
    private String educationInfo;
    @Column(length = 1000)
    private String notes;
    @Column
    private String phone;
    @Column
    private String email;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private String createdBy;
    @Column
    private LocalDateTime updatedAt;
    @Column
    private String updatedBy;
    @Column
    private boolean isInvalid=false;
    @Column
    private LocalDateTime invalidAt;
    @Column
    private String invalidBy;
    @Column
    private LocalDateTime restoredAt;
    @Column
    private String restoredBy;
    @Column
    private String restoredReason;

    public MemberEsfDetails() {
    }
}