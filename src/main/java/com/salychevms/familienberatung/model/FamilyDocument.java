package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="family_document")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FamilyDocument {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name="family_id", nullable = false)
    private Family family;

    @ManyToOne
    @JoinColumn(name = "uploaded_by_employee_id", nullable = false)
    private Employee uploadedByEmployee;
    @Column(nullable=false)
    private String originalFileName;
    @Column(nullable=false)
    private String storedFileName;
    @Column(nullable=false)
    private String fileType;
    @Column(nullable=false)
    private long fileSizeBytes;
    @Column(length=2000)
    private String description;
    @Column(nullable = false)
    private LocalDateTime uploadedAt=LocalDateTime.now();
    @Column
    private LocalDateTime updatedAt;
    @Column
    private String updatedBy;
    @Column(nullable=false)
    private boolean deleted=false;
    @Column
    private LocalDate deletedAt;

    public FamilyDocument() {}
}