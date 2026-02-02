package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Delegation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DelegationRepository extends JpaRepository<Delegation, Long> {

    List<Delegation> findDelegationsByToEmployeeAndFamily(Employee toEmployee, Family family);

    List<Delegation> findDelegationsByToEmployee(Employee toEmployee);


    List<Delegation> findAllByEndDateIsBefore(LocalDate endDateBefore);

    boolean existsByFamilyAndToEmployeeAndExpiredFalseAndAbortedManuallyFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Family family, Employee toEmployee, LocalDate today1, LocalDate today2);

    boolean existsByFamilyAndExpiredFalseAndAbortedManuallyFalseAndEndDateGreaterThanEqual(
            Family family, LocalDate today);

    Optional<Delegation> findFirstByFamilyAndExpiredFalseAndAbortedManuallyFalseAndEndDateGreaterThanEqual(
            Family family,
            LocalDate today
    );

}