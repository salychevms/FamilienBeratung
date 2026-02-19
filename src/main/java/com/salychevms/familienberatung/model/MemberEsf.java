package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_esf")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MemberEsf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberGender gender;
    //TODO
    @Column(nullable = false)
    private LocalDate birthDate;
    //TODO
    @Column(nullable = false)
    private String birthCity;
    //TODO
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

    public MemberEsf() {
    }
}