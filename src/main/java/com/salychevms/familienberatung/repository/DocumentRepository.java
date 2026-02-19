package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByMemberId(Long memberId);

    List<Document> findAllByMemberIdAndIsInvalidFalse(Long memberId);
}