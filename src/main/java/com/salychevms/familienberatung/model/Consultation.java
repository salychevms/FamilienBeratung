package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "consultation")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @Column(nullable = false)
    private LocalDate realDate;
    @Column(nullable = false)
    private LocalTime realStartTime;
    @Column(nullable = false)
    private LocalTime realEndTime;
    @Column(nullable = false)
    private int durationMinutes;
    @Column
    private String topic;
    @Column(length = 3000)
    private String description;
    @Column
    private String result;
    @Column
    private LocalDateTime followUp;
    @Column
    private boolean backdated;
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(nullable = false)
    private String createdBy;
    @Column
    private LocalDateTime updatedAt;
    @Column
    private String updatedBy;
    @Column(nullable = false)
    private boolean deleted = false;
    @Column
    private LocalDate deletedAt;

    public Consultation() {
    }
}