package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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
    @Column(nullable=false)
    private String originalFileName;
    @Column(nullable=false)
    private String storedFileName;
    @Column(nullable=false)
    private String fileType;
    @Column(nullable=false)
    private long fileSizeBytes;
    @Column(length=4000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "uploaded_by_employee_id", nullable = false)
    private Employee uploadedByEmployee;
    @Column(nullable = false)
    private LocalDateTime uploadedAt;
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
    private String restoredBy;
    @Column
    private LocalDateTime restoredAt;
    @Column
    private String restoredReason;

    public FamilyDocument() {}
}