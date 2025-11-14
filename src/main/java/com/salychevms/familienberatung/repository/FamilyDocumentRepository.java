package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.FamilyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyDocumentRepository extends JpaRepository<FamilyDocument, Long> {
    List<FamilyDocument> findByFamilyId(Long familyId);

    List<FamilyDocument> findByFamilyIdAndDeletedFalse(Long familyId);

    List<FamilyDocument> findByUploadedByEmployeeId(Long uploadedByEmployeeId);
}