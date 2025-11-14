package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyRepository extends JpaRepository<Family, Long> {
    List<Family> findByAssignedEmployeeId(Long employeeId);

    List<Family> findByDeletedFalse();
}