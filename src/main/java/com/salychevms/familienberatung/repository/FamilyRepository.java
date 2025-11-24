package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Family;
import com.salychevms.familienberatung.model.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyRepository extends JpaRepository<Family, Long> {
    List<Family> findFamiliesByAssignedEmployeeId(Long employeeId);

    List<Family> findFamiliesByStatusIs(RecordStatus status);

    List<Family> findFamiliesByCaseClosed(boolean caseClosed);

    List<Family> findFamiliesByFamilyName(String familyName);

    List<Family> findFamiliesByAssignedEmployeeIdAndStatusIs(Long assignedEmployeeId, RecordStatus status);

    List<Family> findFamiliesByAssignedEmployeeIdAndCaseClosed(Long assignedEmployeeId, boolean caseClosed);

    Family findFamilyByAssignedEmployeeIdAndStatusIs(Long assignedEmployeeId, RecordStatus status);

}