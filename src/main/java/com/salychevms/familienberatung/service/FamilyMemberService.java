package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.repository.FamilyMemberRepository;
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
public class FamilyMemberService {
    private final FamilyMemberRepository familyMemberRepository;
    private final EmployeeService employeeService;
    private final ValidationService validator;
    private final AccessLogService accessLog;

    public FamilyMember createMember(Family family, String firstName, String lastName, FamilyMemberGender gender,
                                     LocalDate birthDate, String birthCity, String birthCountry, String nationality,
                                     String languages, boolean livesWithFamily, String income, String workInfo,
                                     String educationDegree, String educationInfo, String notes, String phone,
                                     String email, String createdBy, String ip, String userBrowser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update FamilyMember: {}, because Family hasn't status ACTIVE. Family status: {}",
                    family.getId(), family.getStatus());
            throw new RuntimeException("Failed to update FamilyMember: " + family.getId() +
                    " Family status: " + family.getStatus());
        }
        if (!employeeService.hasAccess(createdBy, 50)) {
            log.error("Access Denied for {}", createdBy);
            throw new RuntimeException("Access Denied for " + createdBy);
        }

        validator.validateText(firstName, 255);
        validator.validateText(lastName, 255);
        validator.validateBirthday(birthDate);
        validator.validateText(birthCity, 255);
        validator.validateText(birthCountry, 255);
        validator.validateText(nationality, 255);
        validator.validateText(languages, 255);
        validator.validateText(income, 255);
        validator.validateText(workInfo, 4000);
        validator.validateText(educationDegree, 255);
        validator.validateText(educationInfo, 4000);
        validator.validateText(notes, 1000);
        validator.validateEmail(email);

        FamilyMember member = new FamilyMember();
        member.setFamily(family);
        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setGender(gender);
        member.setBirthDate(birthDate);
        member.setBirthCity(birthCity);
        member.setBirthCountry(birthCountry);
        member.setNationality(nationality);
        member.setLanguages(languages);
        member.setLivesWithFamily(livesWithFamily);
        member.setIncome(income);
        member.setWorkInfo(workInfo);
        member.setEducationDegree(educationDegree);
        member.setEducationInfo(educationInfo);
        member.setNotes(notes);
        member.setPhone(phone);
        member.setEmail(email);
        member.setCreatedBy(createdBy);
        member.setCreatedAt(LocalDateTime.now());

        FamilyMember saved = familyMemberRepository.save(member);

        accessLog.log(createdBy, "FAMILY_MEMBER_CREATE", "FamilyMember", saved.getId(),
                "New FamilyMember created", ip, userBrowser);
        log.info("FamilyMember Id: {} created by {}", saved.getId(), createdBy);
        return saved;
    }

    public FamilyMember updateMember(Family family, FamilyMember member, String firstName, String lastName, FamilyMemberGender gender,
                                     LocalDate birthDate, String birthCity, String birthCountry, String nationality,
                                     String languages, boolean livesWithFamily, String income, String workInfo,
                                     String educationDegree, String educationInfo, String notes, String phone,
                                     String email, String updatedBy, String ip, String userBrowser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update FamilyMember: {}, because Family hasn't status ACTIVE. Family status: {}",
                    family.getId(), family.getStatus());
            throw new RuntimeException("Failed to update FamilyMember: " + family.getId() +
                    " Family status: " + family.getStatus());
        }

        if (!employeeService.hasAccess(updatedBy, 50)) {
            log.error("Access Denied for {}", updatedBy);
            throw new RuntimeException("Access Denied for " + updatedBy);
        }

        if (!member.getFamily().equals(family)) {
            log.error("FamilyMember {} does not match Family {}", member, family.getId());
            throw new RuntimeException("FamilyMember " + member.getId() +
                    " does not match FamilyMember " + family.getId());
        }

        if (member.isInvalid()) {
            log.error("FamilyMember {} is invalid", member.getId());
            throw new RuntimeException("FamilyMember is invalid");
        }

        validator.validateText(firstName, 255);
        validator.validateText(lastName, 255);
        validator.validateBirthday(birthDate);
        validator.validateText(birthCity, 255);
        validator.validateText(birthCountry, 255);
        validator.validateText(nationality, 255);
        validator.validateText(languages, 255);
        validator.validateText(income, 255);
        validator.validateText(workInfo, 4000);
        validator.validateText(educationDegree, 255);
        validator.validateText(educationInfo, 4000);
        validator.validateText(notes, 1000);
        validator.validateEmail(email);

        member.setFirstName(firstName);
        member.setLastName(lastName);
        member.setGender(gender);
        member.setBirthDate(birthDate);
        member.setBirthCity(birthCity);
        member.setBirthCountry(birthCountry);
        member.setNationality(nationality);
        member.setLanguages(languages);
        member.setLivesWithFamily(livesWithFamily);
        member.setIncome(income);
        member.setWorkInfo(workInfo);
        member.setEducationDegree(educationDegree);
        member.setEducationInfo(educationInfo);
        member.setNotes(notes);
        member.setPhone(phone);
        member.setEmail(email);
        member.setUpdatedBy(updatedBy);
        member.setUpdatedAt(LocalDateTime.now());

        FamilyMember saved = familyMemberRepository.save(member);

        accessLog.log(updatedBy, "FAMILY_MEMBER_UPDATE", "FamilyMember", member.getId(),
                "FamilyMember updated", ip, userBrowser);
        log.info("FamilyMember Id: {} updated by {}", saved.getId(), updatedBy);
        return saved;
    }

    public void invalidateMember(Family family, FamilyMember member, String invalidatedBy, String ip, String userBrowser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update FamilyMember: {}, because Family hasn't status ACTIVE. Family status: {}",
                    family.getId(), family.getStatus());
            throw new RuntimeException("Failed to update FamilyMember: " + family.getId() +
                    " Family status: " + family.getStatus());
        }

        if (!employeeService.hasAccess(invalidatedBy, 50)) {
            log.error("Access Denied for {}", invalidatedBy);
            throw new RuntimeException("Access Denied for " + invalidatedBy);
        }

        if (!member.getFamily().equals(family)) {
            log.error("FamilyMember {} does not match FamilyMember {}", member, family.getId());
            throw new RuntimeException("FamilyMember " + member.getId() + " does not match FamilyMember " + family.getId());
        }

        if (member.isInvalid()) {
            log.error("FamilyMember {} is invalid", member.getId());
            throw new RuntimeException("FamilyMember is invalid");
        }

        member.setInvalid(true);
        member.setInvalidBy(invalidatedBy);
        member.setInvalidAt(LocalDateTime.now());
        member.setRestoredAt(null);
        member.setRestoredBy(null);
        member.setRestoredReason(null);

        familyMemberRepository.save(member);
        accessLog.log(invalidatedBy, "FAMILY_MEMBER_INVALIDATE", "FamilyMember", member.getId(),
                "FamilyMember has been invalidated", ip, userBrowser);
        log.info("FamilyMember Id: {} invalidated by {}", member.getId(), invalidatedBy);
    }

    public void restoreMember(Family family, FamilyMember member, String restoredBy,
                              String restoreReason, String ip, String userBrowser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update FamilyMember: {}, because Family hasn't status ACTIVE. Family status: {}",
                    family.getId(), family.getStatus());
            throw new RuntimeException("Failed to update FamilyMember: " + family.getId() +
                    " Family status: " + family.getStatus());
        }

        Employee employee = employeeService.findByLogin(restoredBy);

        if (!employeeService.hasAccess(restoredBy, 50)) {
            log.error("Access Denied for {}", restoredBy);
            throw new RuntimeException("Access Denied for " + restoredBy);
        }

        if (!member.getFamily().equals(family)) {
            log.error("FamilyMember {} does not match FamilyMember {}", member, family.getId());
            throw new RuntimeException("FamilyMember " + member.getId() + " does not match FamilyMember " + family.getId());
        }

        if (!member.isInvalid()) {
            log.error("FamilyMember {} is still not invalid ", member.getId());
            throw new RuntimeException("FamilyMember is still not invalid ");
        }

        long days = java.time.Duration.between(member.getInvalidAt(), LocalDateTime.now()).toDays();

        if (days > 14) {
            if (employee.getRole().getAccessLevel() == 50) {
                log.error("Access Denied for {} after 14 days", restoredBy);
                throw new RuntimeException("Access Denied for " + restoredBy + " after 14 days");
            } else if (employee.getRole().getAccessLevel() >= 80) {
                validator.validateText(restoreReason, 255);
                member.setRestoredReason(restoreReason);
                accessLog.log(restoredBy, "FAMILY_RESTORE_AFTER_14_DAYS", "FamilyMember", member.getId(),
                        "FamilyMember has been restored after 14 days. Reason: " + restoreReason, ip, userBrowser);
                log.info("FamilyMember Id: {} restored by {} after 14 days. Reason: {}", member.getId(), restoredBy, restoreReason);
            }
        } else {
            member.setRestoredReason("Permitted before 14 days");
            log.info("FamilyMember {} has been restored by Employee {} before 14 days.", member.getId(), employee.getLogin());
        }

        member.setInvalid(false);
        member.setInvalidBy(null);
        member.setInvalidAt(null);
        member.setRestoredBy(restoredBy);
        member.setRestoredAt(LocalDateTime.now());

        familyMemberRepository.save(member);
        accessLog.log(restoredBy, "FAMILY_MEMBER_RESTORE", "FamilyMember", member.getId(),
                "FamilyMember restored. Reason: " + restoreReason, ip, userBrowser);
        log.info("FamilyMember Id: {} restored by {}. Reason: {}", member.getId(), restoredBy, restoreReason);
    }

    public FamilyMember getMember(Family family, Long memberId, String login) {
        FamilyMember member = familyMemberRepository.findById(memberId).orElseThrow(() ->
                new RuntimeException("FamilyMember with id: " + memberId + " not found"));
        if (!member.getFamily().equals(family)) {
            log.error("FamilyMember {} does not match FamilyMember {}", member, family.getId());
            throw new RuntimeException("FamilyMember" + member.getId() + " does not match FamilyMember" + family.getId());
        }
        if (member.isInvalid()) {
            long days = java.time.Duration.between(member.getInvalidAt(), LocalDateTime.now()).toDays();
            if (employeeService.hasAccess(login, 50) && days > 14) {
                log.error("Access Denied for {} after 14 days", login);
                throw new RuntimeException("Access Denied for " + login);
            }
        }
        return member;
    }

    public List<FamilyMember> getMembers(Family family, String login) {
        List<FamilyMember> members = familyMemberRepository.findByFamilyId(family.getId());
        if (members.isEmpty()) {
            //log.warn("FamilyMembers with family id: {} not found", family.getId());
            return members;
        }

        List<FamilyMember> result = new ArrayList<>(members);
        for (FamilyMember member : members) {
            if (member.isInvalid()) {
                long days = java.time.Duration.between(member.getInvalidAt(), LocalDateTime.now()).toDays();
                if (employeeService.hasAccess(login, 50) && days > 14) {
                    result.remove(member);
                }
            }
        }
        return result;
    }
}