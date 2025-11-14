package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Delegation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DelegationRepository extends JpaRepository<Delegation, Long> {
    List<Delegation> findByFamilyId(Long familyId);

    List<Delegation> findByFamilyIdAndDeletedFalse(Long familyId);

    List<Delegation> findByToEmployeeId(Long toEmployeeId);
}