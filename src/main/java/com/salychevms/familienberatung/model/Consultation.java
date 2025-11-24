package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private LocalDateTime dateTime;
    @Column(nullable = false)
    private int durationMinutes;
    @Column
    private String topic;
    @Column(length = 4000)
    private String description;
    @Column(length = 4000)
    private String result;
    @Column
    private LocalDateTime followUp;
    @Column
    private boolean backdated;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private String createdBy;
    @Column
    private LocalDateTime updatedAt;
    @Column
    private String updatedBy;
    @Column
    private boolean invalid;
    @Column
    private LocalDateTime invalidAt;
    @Column
    private String invalidBy;
    @Column
    private String restoredBy;
    @Column
    private LocalDateTime restoredAt;
    @Column
    private String restoredReason;

    public Consultation() {
    }
}