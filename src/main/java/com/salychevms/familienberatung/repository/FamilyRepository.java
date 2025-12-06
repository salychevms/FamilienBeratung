package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Family;
import com.salychevms.familienberatung.model.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyRepository extends JpaRepository<Family, Long> {
    List<Family> findFamiliesByAssignedEmployeeId(Long employeeId);

    Family findByZeusIdContaining(String zeusId);
}