package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FamilyRepository extends JpaRepository<Member, Long> {

    List<Member> findAllByAssignedEmployee(Employee assignedEmployee);

    Member findByZeusIdContaining(String zeusId);

    Optional<Member> findByFamilyName(String familyName);

    Optional<Member> getByAssignedEmployeeAndId(Employee employee, Long id);
}