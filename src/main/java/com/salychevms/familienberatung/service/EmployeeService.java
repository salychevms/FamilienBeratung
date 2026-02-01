package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Role;
import com.salychevms.familienberatung.repository.EmployeeRepository;
import com.salychevms.familienberatung.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccessLogService accessLog;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder encoder;
    private final ValidationService validator;

    public boolean hasAccess(String login, int accessLevel) {
        validator.validateText(login, 255);
        Employee employee = employeeRepository.findByLogin(login).orElseThrow(() ->
                new RuntimeException("Employee with login " + login + " not found"));

        if(employee.isArchived() || !employee.isActive()) {
            log.error("Employee {} is blocked or archived", login);
            return false;
        }

        int level = employee.getRole().getAccessLevel();

        boolean allowed = level >= accessLevel;

        if (!allowed) {
            log.info("Employee {} has no access level {}", login, accessLevel);
        } else log.info("Employee {} has access level {}", login, accessLevel);
        return allowed;
    }

    public Employee createEmployee(Employee created, String createdBy, String ip, String userBrowser) {
        validator.validateText(created.getFirstName(), 50);
        validator.validateText(created.getLastName(), 50);
        validator.validateText(created.getLogin(), 50);
        validator.validateEmail(created.getEmail());
        validator.validateIp(ip);
        validator.validatePassword(created.getPassword(), null, created.getLogin(), created.getEmail(),
                created.getMobileNumber(), created.getLandNumber());

        if (employeeRepository.findByLogin(created.getLogin()).isPresent()) {
            log.info("Employee with Login: {} already exists", created.getLogin());
            throw new RuntimeException("Employee with Login: " + created.getLogin() + " already exists");
        }
        if (!roleRepository.existsByName(created.getRole().getName())) {
            log.warn("Role {} doesn't exist", created.getRole().getName());
            throw new RuntimeException("Role " + created.getRole().getName() + " doesn't exist");
        }

        Employee employee = new Employee();
        employee.setFirstName(created.getFirstName());
        employee.setLastName(created.getLastName());
        employee.setLogin(created.getLogin());
        employee.setPassword(encoder.encode(created.getPassword()));
        employee.setEmail(created.getEmail());
        employee.setMobileNumber(created.getMobileNumber());
        employee.setLandNumber(created.getLandNumber());
        employee.setNotes(created.getNotes());
        employee.setRole(created.getRole());
        employee.setCreatedBy(createdBy);
        employee.setCreatedDate(LocalDateTime.now());
        employee.setPasswordChangeRequired(true);
        employee.setArchived(false);
        employee.setActive(true);

        Employee saved = employeeRepository.save(employee);

        accessLog.log(createdBy, "EMPLOYEE_CREATE", "Employee",
                saved.getId(), "Created Employee: " + created.getLogin(), ip, userBrowser);
        log.info("Employee with Login: {} has been created", created.getLogin());
        return saved;
    }

    public Employee updateEmployee(Employee updated, String updatedBy, String ip, String userBrowser) {
        Employee existing = employeeRepository.findById(updated.getId()).orElseThrow(() ->
                new RuntimeException("Employee with Login: " + updated.getLogin() + " doesn't exist"));

        validator.validateText(updated.getFirstName(), 50);
        validator.validateText(updated.getLastName(), 50);
        validator.validateText(updated.getLogin(), 50);
        validator.validateEmail(updated.getEmail());
        validator.validateIp(ip);

        if (!roleRepository.existsByName(updated.getRole().getName())) {
            log.warn("Role {} doesn't exist", updated.getRole().getName());
            throw new RuntimeException("Role " + updated.getRole().getName() + " doesn't exist");
        }

        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setMobileNumber(updated.getMobileNumber());
        existing.setLandNumber(updated.getLandNumber());
        existing.setNotes(updated.getNotes());
        existing.setRole(updated.getRole());
        existing.setUpdatedBy(updatedBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setPasswordChangeRequired(true);
        existing.setArchived(false);
        existing.setActive(true);

        Employee saved = employeeRepository.save(existing);

        accessLog.log(updatedBy, "EMPLOYEE_UPDATE", "Employee", saved.getId(),
                "Updated Employee: " + saved.getLogin(), ip, userBrowser);

        log.info("Employee with Login: {} has been updated", saved.getLogin());
        return saved;
    }

    public void changePassword(Long id, String newPassword, String updatedBy, String ip, String userBrowser) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Employee with Id: " + id + " doesn't exist"));
        validator.validatePassword(newPassword, employee.getPassword(), employee.getLogin(),
                employee.getEmail(), employee.getMobileNumber(), employee.getLandNumber());

        employee.setPassword(encoder.encode(newPassword));
        employee.setPasswordChangeRequired(false);
        employee.setUpdatedBy(updatedBy);
        employee.setUpdatedAt(LocalDateTime.now());

        employeeRepository.save(employee);

        accessLog.log(updatedBy, "EMPLOYEE_PASSWORD_CHANGE", "Employee", employee.getId(),
                "Password changed", ip, userBrowser);

        log.info("Employee with Login: {} has new password ", employee.getLogin());
    }

    public void deactivateEmployee(Long id, String updatedBy, String ip, String userBrowser) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Employee with Id: " + id + " not found"));
        if (!employee.isActive()) {
            log.error("Employee with Login: {} is already deactivated", employee.getLogin());
            throw new RuntimeException("Employee with Login: " + employee.getLogin() + " is already deactivated");
        }
        employee.setActive(false);
        employee.setUpdatedAt(LocalDateTime.now());
        employee.setUpdatedBy(updatedBy);

        employeeRepository.save(employee);

        accessLog.log(updatedBy, "EMPLOYEE_DEACTIVATE", "EMPLOYEE", employee.getId(),
                "Employee deactivated", ip, userBrowser);

        log.info("Employee with Login: {} has been deactivated", employee.getId());
    }

    public void activateEmployee(Long id, String updatedBy, String ip, String userBrowser) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Employee with Id: " + id + " not found"));
        if (employee.isActive()) {
            log.error("Employee with Login: {} is already active", employee.getLogin());
            throw new RuntimeException("Employee with Login: " + employee.getLogin() + " is already active");
        }
        employee.setActive(true);
        employee.setUpdatedAt(LocalDateTime.now());
        employee.setUpdatedBy(updatedBy);

        employeeRepository.save(employee);

        accessLog.log(updatedBy, "EMPLOYEE_ACTIVATE", "EMPLOYEE", employee.getId(),
                "Employee activated", ip, userBrowser);

        log.info("Employee with Login: {} has been activated", employee.getId());
    }

    public void archiveEmployee(Long id, String updatedBy, String ip, String userBrowser) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Employee with Id: " + id + " not found"));

        if (employee.isArchived()) {
            log.error("Employee with Login: {} is already archived", employee.getLogin());
            throw new RuntimeException("Employee with Login: " + employee.getLogin() + " is already archived");
        }

        employee.setArchived(true);
        employee.setActive(false);
        employee.setUpdatedAt(LocalDateTime.now());
        employee.setUpdatedBy(updatedBy);

        employeeRepository.save(employee);

        accessLog.log(updatedBy, "EMPLOYEE_ARCHIVE", "Employee", employee.getId(),
                "Employee archived", ip, userBrowser);

        log.info("Employee with Login: {} has been deactivated and archived", employee.getId());
    }

    public void unarchiveEmployee(Long id, String updatedBy, String ip, String userBrowser) {
        Employee employee = employeeRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Employee with Id: " + id + " not found"));

        if (!employee.isArchived()) {
            log.error("Employee with Login: {} is not archived", employee.getLogin());
            throw new RuntimeException("Employee with Login: " + employee.getLogin() + " is not archived");
        }

        employee.setArchived(false);
        employee.setUpdatedAt(LocalDateTime.now());
        employee.setUpdatedBy(updatedBy);

        employeeRepository.save(employee);

        accessLog.log(updatedBy, "EMPLOYEE_UNARCHIVE", "Employee", employee.getId(),
                "Employee unarchived", ip, userBrowser);

        log.info("Employee with Login: {} has been unarchived", employee.getId());
    }

    public Employee findById(Long id) {
        return employeeRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Employee with Id: " + id + " not found"));
    }

    public Employee findByLogin(String login) {
        return employeeRepository.findByLogin(login).orElseThrow(() ->
                new RuntimeException("Employee with Login: " + login + " not found"));
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }
}