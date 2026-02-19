package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyDocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByFamilyId(Long familyId);

    List<Document> findAllByFamilyIdAndIsInvalidFalse(Long familyId);
}