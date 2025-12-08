package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Family;
import com.salychevms.familienberatung.model.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyRepository extends JpaRepository<Family, Long> {

    List<Family> findAllByAssignedEmployee(Employee assignedEmployee);

    Family findByZeusIdContaining(String zeusId);

    Optional<Family> findByFamilyName(String familyName);
}