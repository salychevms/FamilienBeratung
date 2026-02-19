package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.MemberEsf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberEsfRepository extends JpaRepository<MemberEsf, Long> {
    List<MemberEsf> findByMemberId(Long memberId);
}