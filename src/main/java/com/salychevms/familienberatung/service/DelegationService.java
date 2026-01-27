package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.Delegation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Family;
import com.salychevms.familienberatung.model.RecordStatus;
import com.salychevms.familienberatung.repository.DelegationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DelegationService {
    private final EmployeeService employeeService;
    private final DelegationRepository delegationRepository;
    private final AccessLogService accessLog;
    private final ValidationService validator;

    public Delegation createDelegation(String login, LocalDate start, LocalDate end, Family family,
                                       Employee fromEmployee, Employee toEmployee,
                                       String reason, String ip, String browser) {
        if (!employeeService.hasAccess(login, 80)) {
            log.error("Employee {} does not have access to create delegation", login);
            throw new RuntimeException("Employee " + login + " does not have access to create delegation");
        }
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Family status is not ACTIVE. Family status is {}", family.getStatus());
            throw new RuntimeException("Family status is not ACTIVE. Family status is: " + family.getStatus());
        }
        Delegation isExist = delegationRepository.getDelegationByFamily(family);
        if (isExist != null && !isExist.isExpired()) {
            log.error("Family {} already delegated to {}, delegation ends: {}",
                    family.getId(), isExist.getToEmployee().getLogin(), isExist.getEndDate());
            throw new RuntimeException("Family " + family.getId() +
                    " already delegated to " + isExist.getToEmployee().getLogin() +
                    ", delegation ends: " + isExist.getEndDate());
        }
        if (!family.getAssignedEmployee().getId().equals(fromEmployee.getId())) {
            log.error("Employee FROM and assigned Employee are not the same. " +
                            "FROM Employee: {}, Assigned Employee: {}",
                    fromEmployee.getLogin(), family.getAssignedEmployee().getLogin());
            throw new RuntimeException("Employee FROM and assignedEmployee are not the same. FROM Employee: " +
                    fromEmployee.getLogin() + ", Assigned Employee: " + family.getAssignedEmployee().getLogin());
        }
        if (toEmployee.getId().equals(fromEmployee.getId())) {
            log.error("Employee FROM and Employee TO must be different");
            throw new RuntimeException("Employee FROM and Employee TO must be different");
        }
        if (toEmployee.getRole().getAccessLevel() != 50) {
            log.error("Only Employee with Role 'CONSULTANT' can get Delegation");
            throw new RuntimeException("Only Employee with Role 'CONSULTANT' can get Delegation");
        }
        if (start.isBefore(LocalDate.now())) {
            log.error("Start date is before now");
            throw new RuntimeException("Start date is before now");
        }
        if (end.isBefore(start)) {
            log.error("End date is before start date");
            throw new RuntimeException("End date is before start date");
        }

        validator.validateText(reason, 2000);

        Delegation saved = new Delegation();
        saved.setFamily(family);
        saved.setFromEmployee(fromEmployee);
        saved.setToEmployee(toEmployee);
        saved.setReason(reason);
        saved.setStartDate(start);
        saved.setEndDate(end);
        saved.setExpired(false);
        saved.setCreatedAt(LocalDateTime.now());
        saved.setCreatedBy(login);

        delegationRepository.save(saved);

        accessLog.log(login, "DELEGATION_CREATE", "Delegation", saved.getId(),
                "Delegation created", ip, browser);
        log.info("Delegation {} FROM {} TO {} created. Delegation ends {}",
                saved.getId(), fromEmployee.getLogin(), toEmployee.getLogin(), saved.getEndDate());
        return saved;
    }

    public Delegation updateDelegation(String login, Delegation delegation, LocalDate end,
                                       String reason, String ip, String browser) {
        if (employeeService.findByLogin(login) == null) {
            log.error("Employee with Login {} not found", login);
            throw new RuntimeException("Employee with Login " + login + " not found");
        }
        if (!employeeService.hasAccess(login, 80)) {
            log.error("Employee {} does not have access to update delegation", login);
            throw new RuntimeException("Employee " + login + " does not have access to update delegation");
        }
        if (delegation == null) {
            log.error("Delegation is null");
            throw new RuntimeException("Delegation is null");
        }
        if (delegation.isExpired()) {
            log.error("Delegation {} has already expired. You have to create new delegation", delegation.getId());
            throw new RuntimeException("Delegation " + delegation.getId() + " has already expired");
        }
        if (!end.isAfter(delegation.getEndDate())) {
            log.error("New end date must be after old end date");
            throw new RuntimeException("New end date must be after old end date");
        }
        if (end.isBefore(LocalDate.now())) {
            log.error("End date is before now");
            throw new RuntimeException("End date is before now");
        }

        validator.validateText(reason, 2000);

        delegation.setEndDate(end);
        delegation.setReason(reason);
        delegation.setUpdatedAt(LocalDateTime.now());
        delegation.setUpdatedBy(login);

        Delegation saved = delegationRepository.save(delegation);
        accessLog.log(login, "DELEGATION_UPDATE", "Delegation", saved.getId(),
                "Delegation was updated", ip, browser);
        log.info("Delegation {} was updated by {}", saved.getId(), login);
        return saved;
    }

    public void manualAbortDelegation(String login, Delegation delegation, String ip, String browser) {
        if (employeeService.findByLogin(login) == null) {
            log.error("Employee with Login {} not found", login);
            throw new RuntimeException("Employee with Login " + login + " not found");
        }
        if (delegation == null) {
            log.error("Delegation is null");
            throw new RuntimeException("Delegation is null");
        }
        if (!employeeService.hasAccess(login, 80)) {
            log.error("Employee {} does not have access to update delegation", login);
            throw new RuntimeException("Employee " + login + " does not have access to update delegation");
        }
        if (delegation.isExpired()) {
            log.error("Delegation {} has already expired", delegation.getId());
            throw new RuntimeException("Delegation " + delegation.getId() + " has already expired");
        }

        delegation.setAbortedManually(true);
        delegation.setExpired(true);
        delegation.setAbortedBy(login);
        delegation.setAbortedAt(LocalDateTime.now());

        delegationRepository.save(delegation);
        accessLog.log(login, "DELEGATION_MANUALLY_ABORT", "Delegation", delegation.getId(),
                "Delegation manually aborted", ip, browser);
        log.info("Delegation {} manually aborted by {}", delegation.getId(), login);
    }

    //TODO has to be changed to multisearch
    public Delegation getDelegationByToEmployeeAndFamily(String login, Family family) {
        return delegationRepository.findDelegationByToEmployeeAndFamily(employeeService.findByLogin(login), family);
    }

    public List<Delegation> getDelegationsByToEmployeeAndFamily(Employee employee, Family family) {
        return new ArrayList<>(delegationRepository.findDelegationsByToEmployeeAndFamily(employee, family)
                .stream().filter(this::isDelegationActive).toList());
    }

    public List<Delegation> getDelegationsByToEmployee(Employee employee) {
        List<Delegation> delegations = new ArrayList<>();
        if (employee == null) {
            log.error("Employee is null");
        } else {
            Employee emp = employeeService.findByLogin(employee.getLogin());
            if (emp == null) {
                log.error("Employee with Login {} not found", employee.getLogin());
            } else {
                delegations = delegationRepository.findDelegationsByToEmployee(emp);
            }
        }
        return delegations;
    }

    public List<Delegation> getDelegations() {
        return delegationRepository.findAll();
    }

    public void autoAbortDelegation() {
        List<Delegation> delegations = delegationRepository.findAllByEndDateIsBefore(LocalDate.now()).
                stream().filter(d -> !d.isExpired()).toList();
        for (Delegation d : delegations) {
            d.setExpired(true);
            d.setAbortedManually(false);
            d.setAbortedBy("SYSTEM");
            d.setAbortedAt(LocalDateTime.now());
            delegationRepository.save(d);

            accessLog.log("SYSTEM-AUTO", "DELEGATION_AUTO_ABORT_BY_SCHEDULE", "Delegation",
                    d.getId(), "Delegation has been auto aborted", "SYSTEM-AUTO", "SYSTEM-AUTO");
            log.info("Delegation {} has been auto aborted by SYSTEM because access time has been expired", d.getId());
        }
        log.info("{} delegations have been auto aborted today {} by SYSTEM because access time has expired",
                delegations.size(), LocalDate.now());
    }

    //all
    public List<Family> getDelegatedFamilies(String login) {
        validator.validateText(login, 255);

        Employee requester = employeeService.findByLogin(login);
        if (requester == null) {
            log.error("Employee with login {} not found", login);
            return List.of();
        }

        List<Family> response = new ArrayList<>();
        int lvl = requester.getRole().getAccessLevel();
        //admin and lead and readonly
        if (lvl == 100 || lvl == 80 || lvl == 10) {
            List<Delegation> delegations = getDelegations();
            for (Delegation delegation : delegations) {
                if (delegation.getEndDate().isAfter(LocalDate.now()) && !delegation.isExpired())
                    response.add(delegation.getFamily());
            }
        }

        //consultant
        if (lvl == 50) {
            List<Delegation> delegations = getDelegationsByToEmployee(requester);

            for (Delegation delegation : delegations) {
                if (delegation.getEndDate().isAfter(LocalDate.now()) && !delegation.isExpired()) {
                    response.add(delegation.getFamily());
                }
            }
        }
        return response;
    }

    public boolean isDelegationActive(Delegation delegation) {
        if (delegation == null)
            return false;
        if (delegation.isExpired())
            return false;
        if (delegation.getAbortedAt() != null)
            return false;
        LocalDate end = delegation.getEndDate();
        if (end == null)
            return false;
        return !end.isBefore(LocalDate.now());
    }

    public boolean hasActiveDelegation(Family family, Employee employee) {
        LocalDate today = LocalDate.now();
        return delegationRepository
                .existsByFamilyAndToEmployeeAndExpiredFalseAndAbortedManuallyFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        family, employee, today, today);
    }
}