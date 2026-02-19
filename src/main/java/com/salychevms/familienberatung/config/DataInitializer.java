package com.salychevms.familienberatung.config;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.repository.EmployeeRepository;
import com.salychevms.familienberatung.repository.MemberEsfRepository;
import com.salychevms.familienberatung.repository.MemberRepository;
import com.salychevms.familienberatung.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder encoder;
    private final MemberRepository memberRepository;

    @Value("${test.autofill-database}")
    private boolean autoFill;

    @Override
    public void run(String... args) {
        createRole(100, "ADMIN", "Administrator", "Voller Systemzugriff");
        createRole(80, "LEAD", "Projektleiter*in", "Leitet das Projekt und Team");
        createRole(50, "CONSULTANT", "Berater*in", "Beratungstätigkeit");
        createRole(10, "READONLY", "Nur Lesen", "Nur Anzeige der Daten");

        if (employeeRepository.count() == 0) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow(() ->
                    new RuntimeException("Role with name ADMIN is not found."));
            Employee admin = new Employee();
            admin.setFirstName("Admin");
            admin.setLastName("Administrator");
            admin.setPassword(encoder.encode("admin"));
            admin.setLogin("admin");
            admin.setPasswordChangeRequired(true);
            admin.setRole(adminRole);
            admin.setActive(true);
            admin.setArchived(false);
            admin.setCreatedBy("system:first-start");
            admin.setCreatedDate(LocalDateTime.now());
            employeeRepository.save(admin);
            System.out.println("Admin user created admin/admin");
            log.info("Admin user created admin/admin");
        }

        int memberCount = memberRepository.findAll().size();
        if (memberCount == 0 && autoFill) {
            Role leaderRole = roleRepository.findByName("LEAD").orElseThrow(() ->
                    new RuntimeException("Role with name LEAD not found"));
            Employee lead = new Employee();
            lead.setFirstName("Lead");
            lead.setLastName("Leader");
            lead.setPassword(encoder.encode("leader"));
            lead.setLogin("leader");
            lead.setPasswordChangeRequired(true);
            lead.setRole(leaderRole);
            lead.setActive(true);
            lead.setArchived(false);
            lead.setCreatedBy("system:test-profile");
            lead.setCreatedDate(LocalDateTime.now());
            employeeRepository.save(lead);
            System.out.println("Employee 1 user created leader/leader");
            log.info("Employee 1 user created leader/leader");

            Role beraterRole = roleRepository.findByName("CONSULTANT").orElseThrow(() ->
                    new RuntimeException("Role with name CONSULTANT not found"));
            Employee be1 = new Employee();
            be1.setFirstName("Ber1");
            be1.setLastName("Berater 1");
            be1.setPassword(encoder.encode("berat1"));
            be1.setLogin("berat1");
            be1.setPasswordChangeRequired(true);
            be1.setRole(beraterRole);
            be1.setActive(true);
            be1.setArchived(false);
            be1.setCreatedBy("system:test-profile");
            be1.setCreatedDate(LocalDateTime.now());
            employeeRepository.save(be1);
            System.out.println("Employee 2 user created berat1/berat1");
            log.info("Employee 2 user created berat1/berat1");

            Employee be2 = new Employee();
            be2.setFirstName("Ber2");
            be2.setLastName("Berater 2");
            be2.setPassword(encoder.encode("berat2"));
            be2.setLogin("berat2");
            be2.setPasswordChangeRequired(true);
            be2.setRole(beraterRole);
            be2.setActive(true);
            be2.setArchived(false);
            be2.setCreatedBy("system:test-profile");
            be2.setCreatedDate(LocalDateTime.now());
            employeeRepository.save(be2);
            System.out.println("Employee 2 user created berat2/berat2");
            log.info("Employee 2 user created berat2/berat2");
        }
    }

    private void createRole(int accessLevel, String name, String label, String description) {
        roleRepository.findByName(name).ifPresentOrElse(existing -> {
            if (existing.getAccessLevel() != accessLevel) {
                existing.setAccessLevel(accessLevel);
                roleRepository.save(existing);
                log.info("Role {} updated (access level {})", name, accessLevel);
            }
        }, () -> {
            Role role = new Role();
            role.setAccessLevel(accessLevel);
            role.setName(name);
            role.setLabel(label);
            role.setDescription(description);
            roleRepository.save(role);
            log.info("Role {} created (access level {})", name, accessLevel);
        });
    }
}