package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.repository.ConsultationRepository;
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
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final AccessLogService accessLog;
    private final ValidationService validator;

    public void createConsultation(Member member, Employee employee, LocalDateTime dateTime,
                                   int durationMinutes, String topic, String description, String result,
                                   LocalDateTime followUp, String ip, String browser) {
        if (member == null) {
            log.error("Member is null");
            throw new RuntimeException("Member is null");
        }
        if (!member.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Member status is not ACTIVE");
            throw new RuntimeException("Member status is not ACTIVE");
        }
        if (employee == null) {
            log.error("Employee is null");
            throw new RuntimeException("Employee is null");
        }
        if (employee.getRole().getAccessLevel() <= 10) {
            log.error("Employee {} has no access level", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access level");
        }
        if (dateTime == null) {
            log.error("dateTime is null");
            throw new RuntimeException("dateTime is null");
        }
        if (dateTime.toLocalDate().isAfter(LocalDate.now())) {
            log.error("dateTime is after now");
            throw new RuntimeException("dateTime is after now");
        }
        if (dateTime.toLocalDate().isBefore(member.getJoinedAt())) {
            log.error("Consultation date is before member joined");
            throw new RuntimeException("Consultation date is before member joined");
        }
        if (durationMinutes <= 0) {
            log.error("Duration time can't be 0 or negative");
            throw new RuntimeException("Duration time can't be 0 or negative");
        }
        if (durationMinutes > 480) {
            log.error("Duration time can't be more than 480 minutes (8 hours)");
            throw new RuntimeException("Duration time can't be more than 480 minutes (8 hours)");
        }

        validator.validateText(topic, 255);
        validator.validateText(description, 4000);
        validator.validateText(result, 4000);

        Consultation consultation = new Consultation();
        consultation.setMember(member);
        consultation.setEmployee(employee);
        consultation.setDateTime(dateTime);
        consultation.setBackdated(dateTime.toLocalDate().isBefore(LocalDate.now()));
        consultation.setDurationMinutes(durationMinutes);
        consultation.setTopic(topic);
        consultation.setDescription(description);
        consultation.setResult(result);
        consultation.setFollowUp(followUp);
        consultation.setInvalid(false);
        consultation.setCreatedAt(LocalDateTime.now());
        consultation.setCreatedBy(employee.getLogin());

        Consultation saved = consultationRepository.save(consultation);
        accessLog.log(employee.getLogin(), "CONSULTATION CREATE", "Consultation", saved.getId(),
                "Consultation has been created. Backdated: " + saved.isBackdated(), ip, browser);
        log.info("Consultation has been created by {}. Backdated: {}", employee.getLogin(), saved.isBackdated());
    }

    public void updateConsultation(Consultation consultation, Consultation updated, Member member, Employee employee,
                                   String ip, String browser) {
        if (member == null) {
            log.error("Member is null");
            throw new RuntimeException("Member is null");
        }
        if (consultation == null) {
            log.error("Consultation is null");
            throw new RuntimeException("Consultation is null");
        }
        if (employee == null) {
            log.error("Employee is null");
            throw new RuntimeException("Employee is null");
        }
        if (!member.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Member status is not ACTIVE");
            throw new RuntimeException("Member status is not ACTIVE");
        }
        if (!consultation.getMember().equals(member)) {
            log.error("Consultation does not match member");
            throw new RuntimeException("Consultation does not match member");
        }
        if (consultation.isInvalid()) {
            log.error("Consultation is not invalid");
            throw new RuntimeException("Consultation is not invalid");
        }
        if (employee.getRole().getAccessLevel() <= 10) {
            log.error("Employee {} has no access level", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access level");
        }
        if (updated.getDateTime() == null) {
            log.error("dateTime is null");
            throw new RuntimeException("dateTime is null");
        }
        if (updated.getDateTime().toLocalDate().isAfter(LocalDate.now())) {
            log.error("dateTime is after now");
            throw new RuntimeException("dateTime is after now");
        }
        if (updated.getDateTime().isBefore(member.getCreatedAt())) {
            log.error("Consultation date is before member created");
            throw new RuntimeException("Consultation date is before member created");
        }
        if (updated.getDurationMinutes() <= 0) {
            log.error("Duration time can't be 0 or negative");
            throw new RuntimeException("Duration time can't be 0 or negative");
        }
        if (updated.getDurationMinutes() > 480) {
            log.error("Duration time can't be more than 480 minutes (8 hours)");
            throw new RuntimeException("Duration time can't be more than 480 minutes (8 hours)");
        }
        validator.validateText(updated.getTopic(), 255);
        validator.validateText(updated.getDescription(), 4000);
        validator.validateText(updated.getResult(), 4000);

        consultation.setDateTime(updated.getDateTime());
        consultation.setBackdated(updated.getDateTime().toLocalDate().isBefore(LocalDate.now()));
        consultation.setDurationMinutes(updated.getDurationMinutes());
        consultation.setTopic(updated.getTopic());
        consultation.setDescription(updated.getDescription());
        consultation.setResult(updated.getResult());
        consultation.setFollowUp(updated.getFollowUp());
        consultation.setUpdatedAt(LocalDateTime.now());
        consultation.setUpdatedBy(employee.getLogin());

        Consultation result = consultationRepository.save(consultation);

        accessLog.log(employee.getLogin(), "CONSULTATION_UPDATE", "Consultation", result.getId(),
                "Consultation has been updated", ip, browser);
        log.info("Consultation {} has been updated by {}", result.getId(), employee.getLogin());
    }

    public void invalidateConsultation(Consultation consultation, Member member, Employee employee, String ip, String browser) {
        if (member == null) {
            log.error("Member is null");
            throw new RuntimeException("Member is null");
        }
        if (consultation == null) {
            log.error("Consultation is null");
            throw new RuntimeException("Consultation is null");
        }
        if (employee == null) {
            log.error("Employee is null");
            throw new RuntimeException("Employee is null");
        }
        if (!member.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Member status is not ACTIVE");
            throw new RuntimeException("Member status is not ACTIVE");
        }
        if (!consultation.getMember().equals(member)) {
            log.error("Consultation does not match member");
            throw new RuntimeException("Consultation does not match member");
        }
        if (consultation.isInvalid()) {
            log.error("Consultation is already invalid (soft delete)");
            throw new RuntimeException("Consultation is already invalid (soft delete)");
        }
        if (employee.getRole().getAccessLevel() <= 10) {
            log.error("Employee {} has no access level", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access level");
        }

        consultation.setInvalid(true);
        consultation.setInvalidAt(LocalDateTime.now());
        consultation.setInvalidBy(employee.getLogin());

        consultationRepository.save(consultation);

        accessLog.log(employee.getLogin(), "CONSULTATION_INVALIDATE_(SOFT_DELETE)", "Consultation",
                consultation.getId(), "Consultation has been invalidated (soft delete)", ip, browser);
        log.info("Consultation {} has been invalidated (soft delete) by {}", consultation.getId(), employee.getLogin());
    }

    public void restoreConsultation(Consultation consultation, Member member, Employee employee,
                                    String restoreReason, String ip, String browser) {
        if (member == null) {
            log.error("Member is null");
            throw new RuntimeException("Member is null");
        }
        if (consultation == null) {
            log.error("Consultation is null");
            throw new RuntimeException("Consultation is null");
        }
        if (employee == null) {
            log.error("Employee is null");
            throw new RuntimeException("Employee is null");
        }
        if (!member.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Member status is not ACTIVE");
            throw new RuntimeException("Member status is not ACTIVE");
        }
        if (!consultation.getMember().equals(member)) {
            log.error("Consultation does not match member");
            throw new RuntimeException("Consultation does not match member");
        }
        if (!consultation.isInvalid()) {
            log.error("Consultation is not invalid");
            throw new RuntimeException("Consultation is not invalid");
        }
        if (employee.getRole().getAccessLevel() <= 10) {
            log.error("Employee {} has no access level", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access level");
        }

        long days = java.time.Duration.between(consultation.getInvalidAt(), LocalDateTime.now()).toDays();

        if (days > 14) {
            if (employee.getRole().getAccessLevel() == 50) {
                log.error("Access Denied for {} after 14 days", employee.getLogin());
                throw new RuntimeException("Access Denied for " + employee.getLogin() + " after 14 days");
            } else if (employee.getRole().getAccessLevel() >= 80) {
                validator.validateText(restoreReason, 255);
                consultation.setRestoredReason(restoreReason);
                accessLog.log(employee.getLogin(), "CONSULTATION_RESTORE_AFTER_14_DAYS",
                        "Consultation", consultation.getId(),
                        "Consultation has been restored after 14 days. Reason: " + restoreReason,
                        ip, browser);
                log.info("Consultation Id: {} restored by {} after 14 days. Reason: {}",
                        consultation.getId(), employee.getLogin(), restoreReason);
            }
        } else {
            consultation.setRestoredReason("Permitted before 14 days");
            log.info("Consultation {} has been restored by Employee {} before 14 days.",
                    consultation.getId(), employee.getLogin());
        }

        consultation.setInvalid(false);
        consultation.setInvalidBy(null);
        consultation.setInvalidAt(null);
        consultation.setRestoredBy(employee.getLogin());
        consultation.setRestoredAt(LocalDateTime.now());

        consultationRepository.save(consultation);

        accessLog.log(employee.getLogin(), "CONSULTATION_RESTORE", "Consultation",
                consultation.getId(), "Consultation restored. Reason: " + restoreReason, ip, browser);
        log.info("Consultation {} restored by {}. Reason: {}", consultation.getId(), employee.getLogin(), restoreReason);
    }

    public Consultation getConsultationById(Long consultationId, Employee employee) {
        Consultation consultation = consultationRepository.findConsultationById(consultationId).orElse(null);

        if (employee.getRole().getAccessLevel() == 50 && consultation.isInvalid()) {
            long days = java.time.Duration.between(consultation.getInvalidAt(), LocalDateTime.now()).toDays();
            if (days > 14) {
                log.error("Access Denied for {} after 14 days", employee.getLogin());
                throw new RuntimeException("Access Denied for " + employee.getLogin() + " after 14 days");
            }
        }
        return consultation;
    }

    public List<Consultation> getConsultationsByFamily(Member member) {
        if (member == null) {
            log.error("Member is null");
        }

        List<Consultation> consultations = consultationRepository.findAllByMember(member);
        List<Consultation> results = new ArrayList<>(consultations);
        for (Consultation c : consultations) {
            if (!c.isInvalid()) continue;
            if (c.getInvalidAt() == null) continue;
            long days = java.time.Duration.between(c.getInvalidAt(), LocalDateTime.now()).toDays();
            if (days > 14) results.remove(c);
        }
        return results;
    }

    public void deleteFollowUp(Member member, Consultation consultation, Employee employee, String ip, String browser) {
        if (member == null) {
            log.error("Member is null");
        }
        if (consultation == null) {
            log.error("Consultation is null");
        }
        if (employee == null) {
            log.error("Employee is null");
        }
        if (!member.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Member status is not ACTIVE");
            throw new RuntimeException("Member status is not ACTIVE");
        }
        if (!consultation.getMember().equals(member)) {
            log.error("Consultation does not match member");
            throw new RuntimeException("Consultation does not match member");
        }
        if (consultation.isInvalid()) {
            log.error("Consultation is already invalid (soft delete)");
            throw new RuntimeException("Consultation is already invalid (soft delete)");
        }
        if (employee.getRole().getAccessLevel() <= 10) {
            log.error("Employee {} has no access level", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access level");
        }

        consultation.setFollowUp(null);
        consultationRepository.save(consultation);

        accessLog.log(employee.getLogin(), "CONSULTATION_DELETE_FOLLOW_UP", "Consultation",
                consultation.getId(), "The next appointment has been deleted", ip, browser);
        log.info("Consultation {} has no appointment {}", consultation.getId(), employee.getLogin());
    }

    public List<Consultation> getAll() {
        return consultationRepository.findAll();
    }

    public int getFamilyConsultationsCount(Member member) {
        if (member == null) {
            log.error("Member is null");
            throw new RuntimeException("Member is null");
        }

        return consultationRepository.findAllByMember(member).size();
    }

    public int getDurationTimeMinutesForFamilyCount(Member member) {
        if (member == null) {
            log.error("Member is null");
            throw new RuntimeException("Member is null");
        }

        List<Consultation> consultations = consultationRepository.findAllByMember(member);

        int minutes = 0;
        for (Consultation c : consultations) {
            minutes += c.getDurationMinutes();
        }
        return minutes;
    }
}