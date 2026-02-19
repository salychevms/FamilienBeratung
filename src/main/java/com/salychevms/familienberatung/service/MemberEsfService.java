package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.repository.MemberEsfRepository;
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
public class MemberEsfService {
    private final MemberEsfRepository memberEsfRepository;
    private final EmployeeService employeeService;
    private final ValidationService validator;
    private final AccessLogService accessLog;

    public MemberEsf createMember(Member family, MemberGender gender,
                                  LocalDate birthDate, String birthCity, String birthCountry, String nationality,
                                  String languages, boolean livesWithFamily, String income, String workInfo,
                                  String educationDegree, String educationInfo, String notes, String phone,
                                  String email, String createdBy, String ip, String userBrowser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update MemberEsf: {}, because Member hasn't status ACTIVE. Member status: {}",
                    family.getId(), family.getStatus());
            throw new RuntimeException("Failed to update MemberEsf: " + family.getId() +
                    " Member status: " + family.getStatus());
        }
        if (!employeeService.hasAccess(createdBy, 50)) {
            log.error("Access Denied for {}", createdBy);
            throw new RuntimeException("Access Denied for " + createdBy);
        }

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

        MemberEsf member = new MemberEsf();
        member.setMember(family);
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

        MemberEsf saved = memberEsfRepository.save(member);

        accessLog.log(createdBy, "FAMILY_MEMBER_CREATE", "MemberEsf", saved.getId(),
                "New MemberEsf created", ip, userBrowser);
        log.info("MemberEsf Id: {} created by {}", saved.getId(), createdBy);
        return saved;
    }

    public void updateMember(Member member, MemberEsf currentMember, MemberEsf updated, String updatedBy, String ip, String userBrowser) {
        if (!member.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update MemberEsf: {}, because Member hasn't status ACTIVE. Member status: {}",
                    member.getId(), member.getStatus());
            throw new RuntimeException("Failed to update MemberEsf: " + member.getId() +
                    " Member status: " + member.getStatus());
        }

        if (!employeeService.hasAccess(updatedBy, 50)) {
            log.error("Access Denied for {}", updatedBy);
            throw new RuntimeException("Access Denied for " + updatedBy);
        }

        if (!currentMember.getMember().equals(member)) {
            log.error("MemberEsf {} does not match Member {}", currentMember, member.getId());
            throw new RuntimeException("MemberEsf " + currentMember.getId() +
                    " does not match MemberEsf " + member.getId());
        }

        if (currentMember.isInvalid()) {
            log.error("MemberEsf {} is invalid", currentMember.getId());
            throw new RuntimeException("MemberEsf is invalid");
        }

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

        MemberEsf saved = memberEsfRepository.save(currentMember);

        accessLog.log(updatedBy, "FAMILY_MEMBER_UPDATE", "MemberEsf", currentMember.getId(),
                "MemberEsf updated", ip, userBrowser);
        log.info("MemberEsf Id: {} updated by {}", saved.getId(), updatedBy);
    }

    public void invalidateMember(Member family, MemberEsf member, String invalidatedBy, String ip, String userBrowser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update MemberEsf: {}, because Member hasn't status ACTIVE. Member status: {}",
                    family.getId(), family.getStatus());
            throw new RuntimeException("Failed to update MemberEsf: " + family.getId() +
                    " Member status: " + family.getStatus());
        }

        if (!employeeService.hasAccess(invalidatedBy, 50)) {
            log.error("Access Denied for {}", invalidatedBy);
            throw new RuntimeException("Access Denied for " + invalidatedBy);
        }

        if (!member.getMember().equals(family)) {
            log.error("MemberEsf {} does not match MemberEsf {}", member, family.getId());
            throw new RuntimeException("MemberEsf " + member.getId() + " does not match MemberEsf " + family.getId());
        }

        if (member.isInvalid()) {
            log.error("MemberEsf {} is invalid", member.getId());
            throw new RuntimeException("MemberEsf is invalid");
        }

        member.setInvalid(true);
        member.setInvalidBy(invalidatedBy);
        member.setInvalidAt(LocalDateTime.now());
        member.setRestoredAt(null);
        member.setRestoredBy(null);
        member.setRestoredReason(null);

        memberEsfRepository.save(member);
        accessLog.log(invalidatedBy, "FAMILY_MEMBER_INVALIDATE", "MemberEsf", member.getId(),
                "MemberEsf has been invalidated", ip, userBrowser);
        log.info("MemberEsf Id: {} invalidated by {}", member.getId(), invalidatedBy);
    }

    public void restoreMember(Member family, MemberEsf member, String restoredBy,
                              String restoreReason, String ip, String userBrowser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Failed to update MemberEsf: {}, because Member hasn't status ACTIVE. Member status: {}",
                    family.getId(), family.getStatus());
            throw new RuntimeException("Failed to update MemberEsf: " + family.getId() +
                    " Member status: " + family.getStatus());
        }

        Employee employee = employeeService.findByLogin(restoredBy);

        if (!employeeService.hasAccess(restoredBy, 50)) {
            log.error("Access Denied for {}", restoredBy);
            throw new RuntimeException("Access Denied for " + restoredBy);
        }

        if (!member.getMember().equals(family)) {
            log.error("MemberEsf {} does not match MemberEsf {}", member, family.getId());
            throw new RuntimeException("MemberEsf " + member.getId() + " does not match MemberEsf " + family.getId());
        }

        if (!member.isInvalid()) {
            log.error("MemberEsf {} is still not invalid ", member.getId());
            throw new RuntimeException("MemberEsf is still not invalid ");
        }

        long days = java.time.Duration.between(member.getInvalidAt(), LocalDateTime.now()).toDays();

        if (days > 14) {
            if (employee.getRole().getAccessLevel() == 50) {
                log.error("Access Denied for {} after 14 days", restoredBy);
                throw new RuntimeException("Access Denied for " + restoredBy + " after 14 days");
            } else if (employee.getRole().getAccessLevel() >= 80) {
                validator.validateText(restoreReason, 255);
                member.setRestoredReason(restoreReason);
                accessLog.log(restoredBy, "FAMILY_RESTORE_AFTER_14_DAYS", "MemberEsf", member.getId(),
                        "MemberEsf has been restored after 14 days. Reason: " + restoreReason, ip, userBrowser);
                log.info("MemberEsf Id: {} restored by {} after 14 days. Reason: {}", member.getId(), restoredBy, restoreReason);
            }
        } else {
            member.setRestoredReason("Permitted before 14 days");
            log.info("MemberEsf {} has been restored by Employee {} before 14 days.", member.getId(), employee.getLogin());
        }

        member.setInvalid(false);
        member.setInvalidBy(null);
        member.setInvalidAt(null);
        member.setRestoredBy(restoredBy);
        member.setRestoredAt(LocalDateTime.now());

        memberEsfRepository.save(member);
        accessLog.log(restoredBy, "FAMILY_MEMBER_RESTORE", "MemberEsf", member.getId(),
                "MemberEsf restored. Reason: " + restoreReason, ip, userBrowser);
        log.info("MemberEsf Id: {} restored by {}. Reason: {}", member.getId(), restoredBy, restoreReason);
    }

    public MemberEsf getMember(Member family, Long memberId, String login) {
        MemberEsf member = memberEsfRepository.findById(memberId).orElseThrow(() ->
                new RuntimeException("MemberEsf with id: " + memberId + " not found"));
        if (!member.getMember().equals(family)) {
            log.error("MemberEsf {} does not match MemberEsf {}", member, family.getId());
            throw new RuntimeException("MemberEsf" + member.getId() + " does not match MemberEsf" + family.getId());
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

    public List<MemberEsf> getMembers(Member family, String login) {
        List<MemberEsf> members = memberEsfRepository.findByMemberId(family.getId());
        if (members.isEmpty()) {
            //log.warn("FamilyMembers with family id: {} not found", family.getId());
            return members;
        }

        List<MemberEsf> result = new ArrayList<>(members);
        for (MemberEsf member : members) {
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