package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Delegation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DelegationRepository extends JpaRepository<Delegation, Long> {

    Delegation findDelegationByToEmployeeAndFamily(Employee toEmployee, Family family);

    List<Delegation> findDelegationsByToEmployee(Employee toEmployee);

    Delegation getDelegationByFamily(Family family);

    List<Delegation> findAllByEndDateIsBefore(LocalDate endDateBefore);

    Delegation getDelegationByToEmployeeAndFamily(Employee toEmployee, Family family);
}