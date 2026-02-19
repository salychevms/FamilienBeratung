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
public class MemberEsfDetailsService {
    private final FamilyMemberRepository familyMemberRepository;
    private final EmployeeService employeeService;
    private final ValidationService validator;
    private final AccessLogService accessLog;

    public MemberEsfDetails createMember(Member family, String firstName, String lastName, MemberGender gender,
                                         LocalDate birthDate, String birthCity, String birthCountry, String nationality,
                                         String languages, boolean livesWithFamily, String income, String workInfo,
                                         String educationDegree, String educationInfo, String notes, String phone,
                                         String email, String createdBy, String ip, String userBrowser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update MemberEsfDetails: {}, because Member hasn't status ACTIVE. Member status: {}",
                    family.getId(), family.getStatus());
            throw new RuntimeException("Failed to update MemberEsfDetails: " + family.getId() +
                    " Member status: " + family.getStatus());
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

        MemberEsfDetails member = new MemberEsfDetails();
        member.setMember(family);
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

        MemberEsfDetails saved = familyMemberRepository.save(member);

        accessLog.log(createdBy, "FAMILY_MEMBER_CREATE", "MemberEsfDetails", saved.getId(),
                "New MemberEsfDetails created", ip, userBrowser);
        log.info("MemberEsfDetails Id: {} created by {}", saved.getId(), createdBy);
        return saved;
    }

    public void updateMember(Member member, MemberEsfDetails currentMember, MemberEsfDetails updated, String updatedBy, String ip, String userBrowser) {
        if (!member.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update MemberEsfDetails: {}, because Member hasn't status ACTIVE. Member status: {}",
                    member.getId(), member.getStatus());
            throw new RuntimeException("Failed to update MemberEsfDetails: " + member.getId() +
                    " Member status: " + member.getStatus());
        }

        if (!employeeService.hasAccess(updatedBy, 50)) {
            log.error("Access Denied for {}", updatedBy);
            throw new RuntimeException("Access Denied for " + updatedBy);
        }

        if (!currentMember.getMember().equals(member)) {
            log.error("MemberEsfDetails {} does not match Member {}", currentMember, member.getId());
            throw new RuntimeException("MemberEsfDetails " + currentMember.getId() +
                    " does not match MemberEsfDetails " + member.getId());
        }

        if (currentMember.isInvalid()) {
            log.error("MemberEsfDetails {} is invalid", currentMember.getId());
            throw new RuntimeException("MemberEsfDetails is invalid");
        }

        validator.validateText(updated.getFirstName(), 255);
        validator.validateText(updated.getLastName(), 255);
        validator.validateBirthday(updated.getBirthDate());
        validator.validateText(updated.getBirthCity(), 255);
        validator.validateText(updated.getBirthCountry(), 255);
        validator.validateText(updated.getNationality(), 255);
        validator.validateText(updated.getLanguages(), 255);
        validator.validateText(updated.getIncome(), 255);
        validator.validateText(updated.getWorkInfo(), 4000);
        validator.validateText(updated.getEducationDegree(), 255);
        validator.validateText(updated.getEducationInfo(), 4000);
        validator.validateText(updated.getNotes(), 1000);
        validator.validateEmail(updated.getEmail());

        currentMember.setFirstName(updated.getFirstName());
        currentMember.setLastName(updated.getLastName());
        currentMember.setGender(updated.getGender());
        currentMember.setBirthDate(updated.getBirthDate());
        currentMember.setBirthCity(updated.getBirthCity());
        currentMember.setBirthCountry(updated.getBirthCountry());
        currentMember.setNationality(updated.getNationality());
        currentMember.setLanguages(updated.getLanguages());
        currentMember.setLivesWithFamily(updated.isLivesWithFamily());
        currentMember.setIncome(updated.getIncome());
        currentMember.setWorkInfo(updated.getWorkInfo());
        currentMember.setEducationDegree(updated.getEducationDegree());
        currentMember.setEducationInfo(updated.getEducationInfo());
        currentMember.setNotes(updated.getNotes());
        currentMember.setPhone(updated.getPhone());
        currentMember.setEmail(updated.getEmail());
        currentMember.setUpdatedBy(updatedBy);
        currentMember.setUpdatedAt(LocalDateTime.now());

        MemberEsfDetails saved = familyMemberRepository.save(currentMember);

        accessLog.log(updatedBy, "FAMILY_MEMBER_UPDATE", "MemberEsfDetails", currentMember.getId(),
                "MemberEsfDetails updated", ip, userBrowser);
        log.info("MemberEsfDetails Id: {} updated by {}", saved.getId(), updatedBy);
    }

    public void invalidateMember(Member family, MemberEsfDetails member, String invalidatedBy, String ip, String userBrowser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update MemberEsfDetails: {}, because Member hasn't status ACTIVE. Member status: {}",
                    family.getId(), family.getStatus());
            throw new RuntimeException("Failed to update MemberEsfDetails: " + family.getId() +
                    " Member status: " + family.getStatus());
        }

        if (!employeeService.hasAccess(invalidatedBy, 50)) {
            log.error("Access Denied for {}", invalidatedBy);
            throw new RuntimeException("Access Denied for " + invalidatedBy);
        }

        if (!member.getMember().equals(family)) {
            log.error("MemberEsfDetails {} does not match MemberEsfDetails {}", member, family.getId());
            throw new RuntimeException("MemberEsfDetails " + member.getId() + " does not match MemberEsfDetails " + family.getId());
        }

        if (member.isInvalid()) {
            log.error("MemberEsfDetails {} is invalid", member.getId());
            throw new RuntimeException("MemberEsfDetails is invalid");
        }

        member.setInvalid(true);
        member.setInvalidBy(invalidatedBy);
        member.setInvalidAt(LocalDateTime.now());
        member.setRestoredAt(null);
        member.setRestoredBy(null);
        member.setRestoredReason(null);

        familyMemberRepository.save(member);
        accessLog.log(invalidatedBy, "FAMILY_MEMBER_INVALIDATE", "MemberEsfDetails", member.getId(),
                "MemberEsfDetails has been invalidated", ip, userBrowser);
        log.info("MemberEsfDetails Id: {} invalidated by {}", member.getId(), invalidatedBy);
    }

    public void restoreMember(Member family, MemberEsfDetails member, String restoredBy,
                              String restoreReason, String ip, String userBrowser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update MemberEsfDetails: {}, because Member hasn't status ACTIVE. Member status: {}",
                    family.getId(), family.getStatus());
            throw new RuntimeException("Failed to update MemberEsfDetails: " + family.getId() +
                    " Member status: " + family.getStatus());
        }

        Employee employee = employeeService.findByLogin(restoredBy);

        if (!employeeService.hasAccess(restoredBy, 50)) {
            log.error("Access Denied for {}", restoredBy);
            throw new RuntimeException("Access Denied for " + restoredBy);
        }

        if (!member.getMember().equals(family)) {
            log.error("MemberEsfDetails {} does not match MemberEsfDetails {}", member, family.getId());
            throw new RuntimeException("MemberEsfDetails " + member.getId() + " does not match MemberEsfDetails " + family.getId());
        }

        if (!member.isInvalid()) {
            log.error("MemberEsfDetails {} is still not invalid ", member.getId());
            throw new RuntimeException("MemberEsfDetails is still not invalid ");
        }

        long days = java.time.Duration.between(member.getInvalidAt(), LocalDateTime.now()).toDays();

        if (days > 14) {
            if (employee.getRole().getAccessLevel() == 50) {
                log.error("Access Denied for {} after 14 days", restoredBy);
                throw new RuntimeException("Access Denied for " + restoredBy + " after 14 days");
            } else if (employee.getRole().getAccessLevel() >= 80) {
                validator.validateText(restoreReason, 255);
                member.setRestoredReason(restoreReason);
                accessLog.log(restoredBy, "FAMILY_RESTORE_AFTER_14_DAYS", "MemberEsfDetails", member.getId(),
                        "MemberEsfDetails has been restored after 14 days. Reason: " + restoreReason, ip, userBrowser);
                log.info("MemberEsfDetails Id: {} restored by {} after 14 days. Reason: {}", member.getId(), restoredBy, restoreReason);
            }
        } else {
            member.setRestoredReason("Permitted before 14 days");
            log.info("MemberEsfDetails {} has been restored by Employee {} before 14 days.", member.getId(), employee.getLogin());
        }

        member.setInvalid(false);
        member.setInvalidBy(null);
        member.setInvalidAt(null);
        member.setRestoredBy(restoredBy);
        member.setRestoredAt(LocalDateTime.now());

        familyMemberRepository.save(member);
        accessLog.log(restoredBy, "FAMILY_MEMBER_RESTORE", "MemberEsfDetails", member.getId(),
                "MemberEsfDetails restored. Reason: " + restoreReason, ip, userBrowser);
        log.info("MemberEsfDetails Id: {} restored by {}. Reason: {}", member.getId(), restoredBy, restoreReason);
    }

    public MemberEsfDetails getMember(Member family, Long memberId, String login) {
        MemberEsfDetails member = familyMemberRepository.findById(memberId).orElseThrow(() ->
                new RuntimeException("MemberEsfDetails with id: " + memberId + " not found"));
        if (!member.getMember().equals(family)) {
            log.error("MemberEsfDetails {} does not match MemberEsfDetails {}", member, family.getId());
            throw new RuntimeException("MemberEsfDetails" + member.getId() + " does not match MemberEsfDetails" + family.getId());
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

    public List<MemberEsfDetails> getMembers(Member family, String login) {
        List<MemberEsfDetails> members = familyMemberRepository.findByFamilyId(family.getId());
        if (members.isEmpty()) {
            //log.warn("FamilyMembers with family id: {} not found", family.getId());
            return members;
        }

        List<MemberEsfDetails> result = new ArrayList<>(members);
        for (MemberEsfDetails member : members) {
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