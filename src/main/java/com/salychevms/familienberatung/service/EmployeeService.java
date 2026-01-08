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

        int level = employee.getRole().getAccessLevel();

        boolean allowed = level >= accessLevel;

        if (!allowed) {
            log.info("Employee {} has no access level {}", login, accessLevel);
        } else log.info("Employee {} has access level {}", login, accessLevel);
        return allowed;
    }

    public Employee createEmployee(String firstName, String lastName, String login, String rewPassword,
                                   String email, String mobileNumber, String landNumber, String notes,
                                   Role role, String createdBy, String ip, String userBrowser) {
        validator.validateText(firstName, 50);
        validator.validateText(lastName, 50);
        validator.validateText(login, 50);
        validator.validateEmail(email);
        validator.validateIp(ip);
        validator.validatePassword(rewPassword, null, login, email, mobileNumber, landNumber);

        if (employeeRepository.findByLogin(login).isPresent()) {
            log.info("Employee with Login: {} already exists", login);
            throw new RuntimeException("Employee with Login: " + login + " already exists");
        }
        if (!roleRepository.existsByName(role.getName())) {
            log.warn("Role {} doesn't exist", role);
            throw new RuntimeException("Role " + role + " doesn't exist");
        }

        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setLogin(login);
        employee.setPassword(encoder.encode(rewPassword));
        employee.setEmail(email);
        employee.setMobileNumber(mobileNumber);
        employee.setLandNumber(landNumber);
        employee.setNotes(notes);
        employee.setRole(role);
        employee.setCreatedBy(createdBy);
        employee.setCreatedDate(LocalDateTime.now());
        employee.setPasswordChangeRequired(true);
        employee.setArchived(false);
        employee.setActive(true);

        Employee saved = employeeRepository.save(employee);

        accessLog.log(createdBy, "EMPLOYEE_CREATE", "Employee",
                saved.getId(), "Created Employee: " + login, ip, userBrowser);
        log.info("Employee with Login: {} has been created", login);
        return saved;
    }

    public Employee updateEmployee(Long id, String login, String firstName, String lastName,
                                   String email, String mobileNumber, String landNumber, String notes,
                                   Role role, String updatedBy, String ip, String userBrowser) {
        Employee existing = employeeRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Employee with Login: " + login + " doesn't exist"));

        validator.validateText(firstName, 50);
        validator.validateText(lastName, 50);
        validator.validateText(login, 50);
        validator.validateEmail(email);
        validator.validateIp(ip);

        if (!roleRepository.existsByName(role.getName())) {
            log.warn("Role {} doesn't exist", role);
            throw new RuntimeException("Role " + role + " doesn't exist");
        }

        existing.setFirstName(firstName);
        existing.setLastName(lastName);
        existing.setEmail(email);
        existing.setMobileNumber(mobileNumber);
        existing.setLandNumber(landNumber);
        existing.setNotes(notes);
        existing.setRole(role);
        existing.setUpdatedBy(updatedBy);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setPasswordChangeRequired(true);
        existing.setArchived(false);
        existing.setActive(true);

        Employee saved = employeeRepository.save(existing);

        accessLog.log(updatedBy, "EMPLOYEE_UPDATE", "Employee", saved.getId(),
                "Updated Employee: " + login, ip, userBrowser);

        log.info("Employee with Login: {} has been updated", login);
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