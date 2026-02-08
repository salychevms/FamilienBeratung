package com.salychevms.familienberatung.config;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.repository.EmployeeRepository;
import com.salychevms.familienberatung.repository.FamilyMemberRepository;
import com.salychevms.familienberatung.repository.FamilyRepository;
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
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;

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

        int famCount = familyRepository.findAll().size();
        if (famCount == 0 && autoFill) {
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

            Employee emp1 = employeeRepository.findByLogin("berat1").orElseThrow(() ->
                    new RuntimeException("Consultant with login berat1 not found"));
            Employee emp2 = employeeRepository.findByLogin("berat2").orElseThrow(() ->
                    new RuntimeException("Consultant with login berat2 not found"));

            Family f1 = new Family();
            f1.setFamilyName("Family1");
            f1.setStreet("Street1");
            f1.setCity("City1");
            f1.setHouseNumber("House1");
            f1.setZip("Zip1");
            f1.setPhone("+4915671234567");
            f1.setEmail("email1@email.email");
            f1.setCitizenship("citizenship1");
            f1.setLanguages("language1");
            f1.setReasonDescription("test reason description1");
            f1.setStatus(RecordStatus.ACTIVE);
            f1.setNotes("notes1");
            f1.setAssignedEmployee(emp1);
            f1.setCreatedAt(LocalDateTime.now());
            f1.setCreatedBy("system:test-profile");
            familyRepository.save(f1);
            System.out.println("Family " + f1.getFamilyName() + " created for employee " + emp1.getLogin());
            log.info("Family {} created for employee {}", f1.getFamilyName(), emp1.getLogin());

            Family f2 = new Family();
            f2.setFamilyName("Family2");
            f2.setStreet("Street2");
            f2.setCity("City2");
            f2.setHouseNumber("House2");
            f2.setZip("Zip2");
            f2.setPhone("+4915677894561");
            f2.setEmail("email2@email.email");
            f2.setCitizenship("citizenship2");
            f2.setLanguages("language2");
            f2.setReasonDescription("test reason description2");
            f2.setStatus(RecordStatus.ACTIVE);
            f2.setNotes("notes2");
            f2.setAssignedEmployee(emp2);
            f2.setCreatedAt(LocalDateTime.now());
            f2.setCreatedBy("system:test-profile");
            familyRepository.save(f2);
            System.out.println("Family " + f2.getFamilyName() + " created for employee " + emp2.getLogin());
            log.info("Family {} created for employee {}", f2.getFamilyName(), emp2.getLogin());
        }

        famCount=familyRepository.findAll().size();
        int fmCount=familyMemberRepository.findAll().size();
        if(famCount>=2 && fmCount==0) {
            Family fam1 = familyRepository.findByFamilyName("Family1").orElseThrow(() ->
                    new RuntimeException("Family with name Family1 not found."));
            Family fam2 = familyRepository.findByFamilyName("Family2").orElseThrow(() ->
                    new RuntimeException("Family with name Family2 not found."));
            FamilyMember fMem1 = new FamilyMember();
            fMem1.setFamily(fam1);
            fMem1.setFirstName("firstName1");
            fMem1.setLastName("lastName1");
            fMem1.setGender(FamilyMemberGender.WEIBLICH);
            fMem1.setBirthDate(LocalDate.now().minusYears(35));
            fMem1.setBirthCity("city1");
            fMem1.setBirthCountry("country1");
            fMem1.setNationality("nationality1");
            fMem1.setLanguages("language1");
            fMem1.setLivesWithFamily(true);
            fMem1.setIncome("income1");
            fMem1.setWorkInfo("workInfo1");
            fMem1.setEducationDegree("educationDegree1");
            fMem1.setEducationInfo("educationInfo1");
            fMem1.setNotes("notes1");
            fMem1.setPhone("+4915551234567");
            fMem1.setEmail("email@mail.mail");
            fMem1.setCreatedAt(LocalDateTime.now());
            fMem1.setCreatedBy("system:test-profile");
            familyMemberRepository.save(fMem1);
            System.out.println("Member " + fMem1.getLastName() + " created for family " + fam1.getFamilyName());
            log.info("Member {} created for family {}", fMem1.getLastName(), fam1.getFamilyName());

            FamilyMember fMem2 = new FamilyMember();
            fMem2.setFamily(fam2);
            fMem2.setFirstName("firstName2");
            fMem2.setLastName("lastName2");
            fMem2.setGender(FamilyMemberGender.MAENNLICH);
            fMem2.setBirthDate(LocalDate.now().minusYears(29));
            fMem2.setBirthCity("city2");
            fMem2.setBirthCountry("country2");
            fMem2.setNationality("nationality2");
            fMem2.setLanguages("language2");
            fMem2.setLivesWithFamily(true);
            fMem2.setIncome("income2");
            fMem2.setWorkInfo("workInfo2");
            fMem2.setEducationDegree("educationDegree2");
            fMem2.setEducationInfo("educationInfo2");
            fMem2.setNotes("notes2");
            fMem2.setPhone("+491555127777");
            fMem2.setEmail("maaaail@mail.ma");
            fMem2.setCreatedAt(LocalDateTime.now());
            fMem2.setCreatedBy("system:test-profile");
            familyMemberRepository.save(fMem2);
            System.out.println("Member " + fMem2.getLastName() + " created for family " + fam2.getFamilyName());
            log.info("Member {} created for family {}", fMem2.getLastName(), fam2.getFamilyName());
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