package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.Employee;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeService employeeService;
    private final BCryptPasswordEncoder encoder;
    private final AccessLogService accessLog;

    public Employee login(String login, String rawPassword, String ip, String browser) {
        try{
            Employee employee = employeeService.findByLogin(login);
            if (employee == null) {
                log.error("Login Not Found");
                throw new RuntimeException("Login Not Found");
            }
            if(!encoder.matches(rawPassword, employee.getPassword())) {
                log.error("Wrong Password");
                throw new RuntimeException("Wrong Password");
            }

            accessLog.log(login, "EMPLOYEE_LOG_IN","Employee: Authorization", employee.getId(),
                    "Employee has logged in successfully", ip, browser);
            log.info("Employee {} has logged in", login);

            Employee logged=new Employee();
            logged.setLogin(login);
            logged.setRole(employee.getRole());

            VaadinSession.getCurrent().setAttribute(Employee.class, logged);
            return logged;
        }catch(Exception e){
            log.error("Failed Login: {}",e.getMessage(), e);
            throw new RuntimeException("Failed Login"+e.getMessage(), e);
        }
    }

    public Employee getCurrentEmployee() {
        return VaadinSession.getCurrent().getAttribute(Employee.class);
    }

    public boolean hasRole(int minAccessLevel){
        Employee employee = getCurrentEmployee();
        if(employee == null) return false;
        return employee.getRole().getAccessLevel() >= minAccessLevel;
    }

    public void logout() {
        VaadinSession session=VaadinSession.getCurrent();
        if(session!=null){
            session.setAttribute(Employee.class, null);
            session.close();
        }
        UI.getCurrent().navigate("");
    }
}