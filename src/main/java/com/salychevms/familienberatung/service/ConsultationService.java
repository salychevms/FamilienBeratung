package com.salychevms.familienberatung.service;


import com.salychevms.familienberatung.model.Consultation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Family;
import com.salychevms.familienberatung.model.RecordStatus;
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
    private final DelegationService delegationService;
    private final AccessLogService accessLog;
    private final ValidationService validator;

    public Consultation createConsultation(Family family, Employee employee, LocalDateTime dateTime,
                                           int durationMinutes, String topic, String description, String result,
                                           LocalDateTime followUp, String ip, String browser) {
        try {
            if (family == null) {
                log.error("Family is null");
                throw new RuntimeException("Family is null");
            }
            if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
                log.error("Family status is not ACTIVE");
                throw new RuntimeException("Family status is not ACTIVE");
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
            if (dateTime.isBefore(family.getCreatedAt())) {
                log.error("Consultation date is before family created");
                throw new RuntimeException("Consultation date is before family created");
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
            consultation.setFamily(family);
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
            return saved;
        } catch (Exception e) {
            log.error("Failed to create consultation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create consultation" + e.getMessage(), e);
        }
    }

    public Consultation updateConsultation(Consultation consultation, Family family, Employee employee,
                                           LocalDateTime dateTime, int durationMinutes, String topic, String description,
                                           String result, LocalDateTime followUp, String ip, String browser) {
        try {
            if (family == null) {
                log.error("Family is null");
                throw new RuntimeException("Family is null");
            }
            if (consultation == null) {
                log.error("Consultation is null");
                throw new RuntimeException("Consultation is null");
            }
            if (employee == null) {
                log.error("Employee is null");
                throw new RuntimeException("Employee is null");
            }
            if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
                log.error("Family status is not ACTIVE");
                throw new RuntimeException("Family status is not ACTIVE");
            }
            if (!consultation.getFamily().equals(family)) {
                log.error("Consultation does not match family");
                throw new RuntimeException("Consultation does not match family");
            }
            if (consultation.isInvalid()) {
                log.error("Consultation is not invalid");
                throw new RuntimeException("Consultation is not invalid");
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
            if (dateTime.isBefore(family.getCreatedAt())) {
                log.error("Consultation date is before family created");
                throw new RuntimeException("Consultation date is before family created");
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

            consultation.setDateTime(dateTime);
            consultation.setBackdated(dateTime.toLocalDate().isBefore(LocalDate.now()));
            consultation.setDurationMinutes(durationMinutes);
            consultation.setTopic(topic);
            consultation.setDescription(description);
            consultation.setResult(result);
            consultation.setFollowUp(followUp);
            consultation.setUpdatedAt(LocalDateTime.now());
            consultation.setUpdatedBy(employee.getLogin());

            Consultation updated = consultationRepository.save(consultation);

            accessLog.log(employee.getLogin(), "CONSULTATION_UPDATE", "Consultation", updated.getId(),
                    "Consultation has been updated", ip, browser);
            log.info("Consultation {} has been updated by {}", updated.getId(), employee.getLogin());
            return updated;
        } catch (Exception e) {
            log.error("Failed to update consultation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update consultation" + e.getMessage(), e);
        }
    }

    public void invalidateConsultation(Consultation consultation, Family family, Employee employee, String ip, String browser) {
        try {
            if (family == null) {
                log.error("Family is null");
                throw new RuntimeException("Family is null");
            }
            if (consultation == null) {
                log.error("Consultation is null");
                throw new RuntimeException("Consultation is null");
            }
            if (employee == null) {
                log.error("Employee is null");
                throw new RuntimeException("Employee is null");
            }
            if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
                log.error("Family status is not ACTIVE");
                throw new RuntimeException("Family status is not ACTIVE");
            }
            if (!consultation.getFamily().equals(family)) {
                log.error("Consultation does not match family");
                throw new RuntimeException("Consultation does not match family");
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
        } catch (Exception e) {
            log.error("Failed to invalidate (soft delete) consultation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to invalidate (soft delete) consultation" + e.getMessage(), e);
        }
    }

    public void restoreConsultation(Consultation consultation, Family family, Employee employee,
                                    String restoreReason, String ip, String browser) {
        try {
            if (family == null) {
                log.error("Family is null");
                throw new RuntimeException("Family is null");
            }
            if (consultation == null) {
                log.error("Consultation is null");
                throw new RuntimeException("Consultation is null");
            }
            if (employee == null) {
                log.error("Employee is null");
                throw new RuntimeException("Employee is null");
            }
            if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
                log.error("Family status is not ACTIVE");
                throw new RuntimeException("Family status is not ACTIVE");
            }
            if (!consultation.getFamily().equals(family)) {
                log.error("Consultation does not match family");
                throw new RuntimeException("Consultation does not match family");
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
            log.info("Consultation {} restored by {}. Reason: {}",
                    consultation.getId(), employee.getLogin(), restoreReason);
        } catch (Exception e) {
            log.error("Failed to restore consultation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to restore consultation" + e.getMessage(), e);
        }
    }

    public Consultation getConsultationById(Long consultationId, Employee employee) {
        try {
            if (consultationId == null) {
                log.error("Consultation is null");
                throw new RuntimeException("Consultation is null");
            }
            if (employee == null) {
                log.error("Employee is null");
                throw new RuntimeException("Employee is null");
            }

            Consultation consultation = consultationRepository.findConsultationById(consultationId).orElseThrow(() ->
                    new RuntimeException("Consultation not found"));

            if (employee.getRole().getAccessLevel() == 50 && consultation.isInvalid()) {
                long days = java.time.Duration.between(consultation.getInvalidAt(), LocalDateTime.now()).toDays();
                if (days > 14) {
                    log.error("Access Denied for {} after 14 days", employee.getLogin());
                    throw new RuntimeException("Access Denied for " + employee.getLogin() + " after 14 days");
                }
            }
            return consultation;
        } catch (Exception e) {
            log.error("Failed to retrieve consultation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve consultation" + e.getMessage(), e);
        }
    }

    public List<Consultation> getConsultationsByFamily(Family family, Employee employee) {
        try {
            if (family == null) {
                log.error("Family is null");
                throw new RuntimeException("Family is null");
            }
            if (employee == null) {
                log.error("Employee is null");
                throw new RuntimeException("Employee is null");
            }
            List<Consultation> consultations = consultationRepository.findAllByFamily(family);
            if (employee.getRole().getAccessLevel() == 50) {
                List<Consultation> results = new ArrayList<>(consultations);
                for (Consultation c : consultations) {
                    long days = java.time.Duration.between(c.getInvalidAt(), LocalDateTime.now()).toDays();
                    if (days > 14 && c.isInvalid()) results.remove(c);
                }
                return results;
            } else return consultations;
        } catch (Exception e) {
            log.error("Failed to retrieve all consultations for family {}: {}",
                    family.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve consultations for family " +
                    family.getId() + ": " + e.getMessage(), e);
        }
    }

    public List<Consultation> getAll(Employee employee) {
        try {
            if (employee == null) {
                log.error("Employee is null");
            }
            if (employee.getRole().getAccessLevel() == 50) {
                List<Consultation> consultations = consultationRepository.findAll();
                List<Consultation> results = new ArrayList<>();
                for (Consultation c : consultations) {
                    if ((c.getFamily().getAssignedEmployee().getId().equals(employee.getId())))
                        results.add(c);
                }
                return results.stream().filter(r -> {
                    if (!r.isInvalid()) return true;
                    if (r.getInvalidAt() == null) return false;
                    long days = java.time.Duration.between(r.getInvalidAt(), LocalDateTime.now()).toDays();
                    return days <= 14;
                }).toList();
            } else return consultationRepository.findAll();
        } catch (
                Exception e) {
            log.error("Failed to retrieve all consultations: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve all consultations: " + e.getMessage(), e);
        }
    }
}