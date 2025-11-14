package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="delegation")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Delegation {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name="family_id", nullable = false)
    private Family family;

    @ManyToOne
    @JoinColumn(name="from_employee_id", nullable = false)
    private Employee fromEmployee;

    @ManyToOne
    @JoinColumn(name="to_employee_id", nullable = false)
    private Employee toEmployee;

    @Column(nullable = false)
    private LocalDate startDate;
    @Column(nullable = false)
    private LocalDate endDate;
    @Column(nullable=false, length=2000)
    private String reason;
    @Column(nullable=false)
    private LocalDateTime createdAt=LocalDateTime.now();
    @Column(nullable=false)
    private String createdBy;
    @Column
    private LocalDateTime updatedAt;
    @Column
    private String updatedBy;
    @Column(nullable = false)
    private boolean deleted=false;
    @Column
    private LocalDate deletedAt;

    public Delegation() {}
}