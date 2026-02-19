package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.MemberEsfDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyMemberRepository extends JpaRepository<MemberEsfDetails, Long> {
    List<MemberEsfDetails> findByFamilyId(Long familyId);
}