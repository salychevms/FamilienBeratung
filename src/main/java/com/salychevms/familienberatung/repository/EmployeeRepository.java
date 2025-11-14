package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByLogin(String login);

    List<Employee> findByActiveTrue();
}