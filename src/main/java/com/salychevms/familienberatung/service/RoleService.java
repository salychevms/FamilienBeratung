package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Role;
import com.salychevms.familienberatung.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final AccessLogService accessLog;
    private final EmployeeService employeeService;
    private final ValidationService validator;

    private void createRole(Employee employee, Role created, String ip, String browser) {
        if (employee == null) throw new RuntimeException("Employee is null");
        Employee e = employeeService.findByLogin(employee.getLogin());
        if (e == null || e.getRole().getAccessLevel() != 100)
            throw new RuntimeException("Access level is not 100 or employee not found");
        if (created == null) throw new RuntimeException("Created role is null");
        validator.validateText(created.getName(), 50);
        validator.validateText(created.getLabel(), 100);
        if (created.getDescription().length() > 255) throw new RuntimeException("Description is too long (255 max)");
        if (created.getAccessLevel() > 100 || created.getAccessLevel() < 0)
            throw new RuntimeException("Access level is out of range (0-100)");

        Role role = new Role();
        role.setAccessLevel(created.getAccessLevel());
        role.setName(created.getName());
        role.setLabel(created.getLabel());
        role.setDescription(created.getDescription());

        roleRepository.save(role);
        accessLog.log(e.getLogin(), "ROLE_CREATE", "Role", role.getId(), "Created Role: " +
                role.getLabel() + ", access level: " + role.getAccessLevel(), ip, browser);
        log.info("New role created: {}; access level: {}", role.getLabel(), role.getAccessLevel());
    }

    public Role getRoleByLabel(Employee employee, String label) {
        if (employee == null) throw new RuntimeException("Employee is null");
        if (label == null) throw new RuntimeException("Label is null");
        int lvl = employeeService.findByLogin(employee.getLogin()).getRole().getAccessLevel();
        if (lvl == 100)
            return roleRepository.findByLabel(label).orElse(null);
        throw new RuntimeException("Access denied");
    }

    public List<Role> getAll(Employee employee) {
        if (employee == null) throw new RuntimeException("Employee is null");
        int lvl = employeeService.findByLogin(employee.getLogin()).getRole().getAccessLevel();
        if (lvl == 100)
            return roleRepository.findAll();
        throw new RuntimeException("Access denied");
    }
}
