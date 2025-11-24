package com.salychevms.familienberatung.config;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Role;
import com.salychevms.familienberatung.repository.EmployeeRepository;
import com.salychevms.familienberatung.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        createRole(100, "ADMIN", "Administrator", "Voller Systemzugriff");
        createRole(80, "LEAD", "Projektleiter*in", "Leitet das Projekt und Team");
        createRole(50, "CONSULTANT", "Berater*in", "Beratungstätigkeit");
        createRole(10, "READONLY", "Nur Lesen", "Nur Anzeige der Daten");

        if (employeeRepository.count() == 0) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            Employee admin = new Employee();
            admin.setFirstName("Admin");
            admin.setLastName("Administrator");
            admin.setPassword(encoder.encode("admin"));
            admin.setLogin("admin");
            admin.setPasswordChangeRequired(true);
            admin.setRole(adminRole);
            admin.setActive(true);
            admin.setArchived(false);
            admin.setCreatedBy("system");
            employeeRepository.save(admin);
            System.out.println("Admin user created admin/admin");
            log.info("Admin user created admin/admin");
        }
    }

    private void createRole(int accessLevel, String name, String label, String description) {
        roleRepository.findByName(name).ifPresentOrElse(existing->{
            if(existing.getAccessLevel()!=accessLevel){
                existing.setAccessLevel(accessLevel);
                roleRepository.save(existing);
                log.info("Role {} updated (access level {})", name, accessLevel);
            }
        }, ()->{
            Role role = new Role(accessLevel, name, label, description);
            roleRepository.save(role);
            log.info("Role {} created (access level {})", name, accessLevel);
        });
    }
}