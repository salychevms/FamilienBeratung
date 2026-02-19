package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Delegation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DelegationRepository extends JpaRepository<Delegation, Long> {

    List<Delegation> findDelegationsByToEmployeeAndMember(Employee toEmployee, Member member);

    List<Delegation> findDelegationsByToEmployee(Employee toEmployee);


    List<Delegation> findAllByEndDateIsBefore(LocalDate endDateBefore);

    boolean existsByMemberAndToEmployeeAndExpiredFalseAndAbortedManuallyFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Member member, Employee toEmployee, LocalDate today1, LocalDate today2);

    boolean existsByMemberAndExpiredFalseAndAbortedManuallyFalseAndEndDateGreaterThanEqual(
            Member member, LocalDate today);

    Optional<Delegation> findFirstByMemberAndExpiredFalseAndAbortedManuallyFalseAndEndDateGreaterThanEqual(
            Member member,
            LocalDate today
    );

}