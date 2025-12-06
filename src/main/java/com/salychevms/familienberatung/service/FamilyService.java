package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.Delegation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Family;
import com.salychevms.familienberatung.model.RecordStatus;
import com.salychevms.familienberatung.repository.FamilyRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final EmployeeService employeeService;
    private final ValidationService validator;
    private final AccessLogService accessLog;
    private final DelegationService delegationService;

    //admin and lead and consultant
    public Family createFamily(String zeusId, String familyName, String street, String houseNumber, String zip, String city,
                               String phone, String email, String citizenship, String languages, String reasonDescription,
                               String notes, Long assignedEmployeeId, String createdBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(createdBy, 50)) {
                log.error("Access Denied for {}", createdBy);
                throw new RuntimeException("Access Denied for " + createdBy);
            }

            validator.validateText(familyName, 255);
            validator.validateText(street, 255);
            validator.validateText(houseNumber, 255);
            validator.validateText(zip, 255);
            validator.validateText(city, 255);
            validator.validatePhone(phone);
            validator.validateEmail(email);
            validator.validateText(citizenship, 255);
            validator.validateText(languages, 255);
            validator.validateText(reasonDescription, 4000);
            validator.validateText(notes, 255);
            validator.validateIp(ip);

            Employee assignedEmployee = employeeService.findById(assignedEmployeeId);
            if (assignedEmployee == null) {
                log.error("Employee with Id: {} not found", assignedEmployeeId);
                throw new EntityNotFoundException("Employee with Id: " + assignedEmployeeId + " not found");
            }

            Family family = new Family();

            family.setZeusId(zeusId);
            family.setFamilyName(familyName);
            family.setStreet(street);
            family.setHouseNumber(houseNumber);
            family.setZip(zip);
            family.setCity(city);
            family.setPhone(phone);
            family.setEmail(email);
            family.setCitizenship(citizenship);
            family.setLanguages(languages);
            family.setReasonDescription(reasonDescription);
            family.setNotes(notes);
            family.setAssignedEmployee(assignedEmployee);

            family.setStatus(RecordStatus.ACTIVE);
            family.setCaseClosed(false);
            family.setCreatedAt(LocalDateTime.now());
            family.setCreatedBy(createdBy);

            Family saved = familyRepository.save(family);

            accessLog.log(createdBy, "FAMILY_CREATE", "Family", saved.getId(),
                    "Created family " + familyName, ip, userBrowser);
            log.info("Family {} created by {}", familyName, createdBy);
            return saved;
        } catch (Exception e) {
            log.error("Error while saving family {}", familyName, e);
            throw new RuntimeException("Error while saving family " + familyName, e);
        }
    }

    //admin and lead and consultant and delegated
    public Family updateFamily(Long id, String zeusId, String familyName, String street, String houseNumber, String zip,
                               String city, String phone, String email, String citizenship, String languages,
                               String reasonDescription, String notes, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 50)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Family family = familyRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Family with Id: " + id + " not found"));
            if (!family.getAssignedEmployee().getLogin().equals(updatedBy) &&
                    !delegationService.hasAccessForUpdate(updatedBy, family)) {
                log.error("Access Denied for {}, no rights to this family", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            validator.validateText(familyName, 255);
            validator.validateText(street, 255);
            validator.validateText(houseNumber, 255);
            validator.validateText(zip, 255);
            validator.validateText(city, 255);
            validator.validatePhone(phone);
            validator.validateEmail(email);
            validator.validateText(citizenship, 255);
            validator.validateText(languages, 255);
            validator.validateText(reasonDescription, 4000);
            validator.validateText(notes, 255);
            validator.validateIp(ip);

            family.setZeusId(zeusId);
            family.setFamilyName(familyName);
            family.setStreet(street);
            family.setHouseNumber(houseNumber);
            family.setZip(zip);
            family.setCity(city);
            family.setPhone(phone);
            family.setEmail(email);
            family.setCitizenship(citizenship);
            family.setLanguages(languages);
            family.setReasonDescription(reasonDescription);
            family.setNotes(notes);

            family.setUpdatedAt(LocalDateTime.now());
            family.setUpdatedBy(updatedBy);

            Family saved = familyRepository.save(family);

            accessLog.log(updatedBy, "FAMILY_UPDATE", "Family", saved.getId(),
                    "Updated family " + familyName, ip, userBrowser);
            log.info("Family {} updated by {}", familyName, updatedBy);
            return saved;
        } catch (Exception e) {
            log.error("Error while saving family {}, error: {}", familyName, e.getMessage(), e);
            throw new RuntimeException("Error while saving family " + familyName, e);
        }
    }

    //admin and lead
    public void updateAssignedEmployee(Long id, Long assignedEmployeeId, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 80)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Employee updater = employeeService.findByLogin(updatedBy);
            if (updater == null) {
                log.error("Employee with Login: {} not found", updatedBy);
                throw new RuntimeException("Employee with Login: " + updatedBy + " not found");
            }
            if (!updater.getRole().getName().equals("ADMIN") && !updater.getRole().getName().equals("LEAD")) {
                log.error("Employee {} has no access to this role", updatedBy);
                throw new RuntimeException("Employee with Login: " + updatedBy + " has no access to this role");
            }
            Family family = familyRepository.findById(id).orElseThrow(() ->
                    new RuntimeException("Family with Id: " + id + " not found"));

            Employee employee = employeeService.findById(assignedEmployeeId);
            if (employee == null) {
                log.error("Employee with Id: {} not found", assignedEmployeeId);
                throw new RuntimeException("Employee with Id: " + assignedEmployeeId + " not found");
            }
            if (family.getAssignedEmployee().getId().equals(assignedEmployeeId)) {
                log.error("Assigned employee with Id: {} already assigned for family with Id: {}", assignedEmployeeId, family.getId());
                throw new RuntimeException("Assigned employee with Id: " + assignedEmployeeId +
                        " already assigned for family with Id: " + family.getId());
            }

            family.setAssignedEmployee(employee);
            family.setUpdatedAt(LocalDateTime.now());
            family.setUpdatedBy(updatedBy);

            familyRepository.save(family);
            accessLog.log(updatedBy, "FAMILY_UPDATE ASSIGNED_EMPLOYEE", "Family", family.getId(),
                    "Family has new assigned employee with Id: " + assignedEmployeeId, ip, userBrowser);
            log.info("Family assigned employee has been changed. Family Id: {}, new employee id: {}, updater id: {}",
                    family.getId(), assignedEmployeeId, updatedBy);

        } catch (
                Exception e) {
            log.error("Failed to update assigned employee, error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update assigned employee", e);
        }
    }

    //admin and lead and consultant and delegated
    public void closeCase(Long id, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 50)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }
            Family family = familyRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Family with Id: " + id + " not found"));

            if (!family.getAssignedEmployee().getLogin().equals(updatedBy) &&
                    !delegationService.hasAccessForUpdate(updatedBy, family)) {
                log.error("Access Denied for {}, no rights to this family", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            if (family.isCaseClosed()) {
                log.warn("Case for family with id: {} is already closed.", id);
                throw new RuntimeException("Case for family with id: " + id + " is already closed");
            }
            if (family.getStatus().equals(RecordStatus.BLOCKED)) {
                log.warn("Case for family with id: {} is blocked.", id);
                throw new RuntimeException("Case for family with id: " + id + " is blocked");
            }
            if (family.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Case for family with id: {} is archived.", id);
                throw new RuntimeException("Case for family with id: " + id + " is archived");
            }
            if (family.getStatus().equals(RecordStatus.INVALID)) {
                log.warn("Case for family with id: {} is invalid.", id);
                throw new RuntimeException("Case for family with id: " + id + " is invalid");
            }

            family.setCaseClosed(true);
            family.setCaseClosedAt(LocalDateTime.now());
            family.setDeletePlannedAt(family.getCaseClosedAt().plusYears(10));

            familyRepository.save(family);
            accessLog.log(updatedBy, "FAMILY_CLOSE_CASE", "Family", family.getId(),
                    "Case closed", ip, userBrowser);

            log.info("Family {} case closed by {}", family.getId(), updatedBy);
        } catch (Exception e) {
            log.error("Error while closing family case. Family Id: {}, error: ", id, e);
            throw new RuntimeException("Error while closing family case. Family Id: " + id, e);
        }
    }

    //admin and lead and consultant and delegated
    public void openCaseBack(Long id, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 50)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Family family = familyRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Family with Id: " + id + " not found"));

            if (!family.getAssignedEmployee().getLogin().equals(updatedBy) &&
                    !delegationService.hasAccessForUpdate(updatedBy, family)) {
                log.error("Access Denied for {}, no rights to this family", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }
            if (!family.isCaseClosed()) {
                log.error("Family case still open, family id: {}", id);
                throw new RuntimeException("Family case still open, family id: " + id);
            }
            if (family.getStatus().equals(RecordStatus.BLOCKED)) {
                log.warn("Case for family with id: {} is blocked.", id);
                throw new RuntimeException("Case for family with id: " + id + " is blocked");
            }
            if (family.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Case for family with id: {} is archived.", id);
                throw new RuntimeException("Case for family with id: " + id + " is archived");
            }
            if (family.getStatus().equals(RecordStatus.INVALID)) {
                log.warn("Case for family with id: {} is invalid.", id);
                throw new RuntimeException("Case for family with id: " + id + " is invalid");
            }
            family.setCaseClosed(false);
            family.setCaseClosedAt(null);
            family.setDeletePlannedAt(null);

            familyRepository.save(family);
            accessLog.log(updatedBy, "FAMILY_CASE_OPEN_BACK", "Family", family.getId(),
                    "Family case opened back", ip, userBrowser);
            log.info("Family {} case opened back by {}", family.getId(), updatedBy);
        } catch (Exception e) {
            log.error("Failed to open family case back. Family Id: {}, error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to open family case back. Family Id: " + id, e);
        }
    }

    //admin and lead and consultant and delegated
    public void invalidateFamily(Long id, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 50)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Family family = familyRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Family with Id: " + id + " not found"));

            if (!family.getAssignedEmployee().getLogin().equals(updatedBy) &&
                    !delegationService.hasAccessForUpdate(updatedBy, family)) {
                log.error("Access Denied for {}, no rights to this family", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }
            if (family.isCaseClosed()) {
                log.warn("Case for family with id: {} is already closed.", id);
                throw new RuntimeException("Case for family with id: " + id + " is already closed");
            }
            if (family.getStatus().equals(RecordStatus.INVALID)) {
                log.warn("Family with id: {} is already invalid (soft delete).", id);
                throw new RuntimeException("Family with id: " + id + " is already invalid (soft delete)");
            }
            if (family.getStatus().equals(RecordStatus.BLOCKED)) {
                log.warn("Family with id: {} is already blocked.", id);
                throw new RuntimeException("Family with id: " + id + " is already blocked");
            }
            if (family.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Family with id: {} is already archived.", id);
                throw new RuntimeException("Family with id: " + id + " is already archived");
            }

            family.setStatus(RecordStatus.INVALID);
            family.setInvalidBy(updatedBy);
            family.setInvalidAt(LocalDateTime.now());
            family.setDeletePlannedAt(family.getInvalidAt().plusYears(10));

            familyRepository.save(family);

            accessLog.log(updatedBy, "FAMILY_INVALID_(SOFT_DELETE)", "Family", family.getId(),
                    "Family is invalid (soft delete)", ip, userBrowser);

            log.info("Family {} is invalid (soft delete) by {}", family.getId(), updatedBy);
        } catch (Exception e) {
            log.error("Error while making family invalid (soft delete). Family Id: {}, error: ", id, e);
            throw new RuntimeException("Error while making family invalid (soft delete). Family Id: " + id, e);
        }
    }

    //admin and lead
    public void restoreFamily(Long id, String updatedBy, String restoreReason, String ip, String userBrowser) {
        try {
            Family family = familyRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Family with Id: " + id + " not found"));
            if (!family.getStatus().equals(RecordStatus.INVALID)) {
                log.warn("Family with id: {} is not invalid.", id);
                throw new RuntimeException("Family with id: " + id + " is not invalid");
            }

            Employee employee = employeeService.findByLogin(updatedBy);
            if (employee.getRole().getAccessLevel() <= 10) {
                log.error("Access Denied for {}. Role is {}", updatedBy, employee.getRole().getName());
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            long days = java.time.Duration.between(family.getInvalidAt(), LocalDateTime.now()).toDays();

            if (days > 14) {
                if (employee.getRole().getAccessLevel() == 50) {
                    log.error("Access Denied for {} after 14 days", updatedBy);
                    throw new RuntimeException("Access Denied for " + updatedBy + " after 14 days");
                } else if (employee.getRole().getAccessLevel() >= 80) {
                    validator.validateText(restoreReason, 255);
                    family.setRestoredReason(restoreReason);
                    log.info("Employee {} has been restored Family {} after 14 days. Reason: {}",
                            employee.getLogin(), family.getId(), restoreReason);
                    accessLog.log(updatedBy, "FAMILY_RESTORE_AFTER_14_DAYS", "Family", family.getId(),
                            "Family has been restored after 14 days. Reason: " + restoreReason, ip, userBrowser);
                }
            } else {
                family.setRestoredReason("Permitted before 14 days");
                log.info("Family {} has been restored by Employee {} before 14 days.", family.getId(), employee.getLogin());
            }

            family.setStatus(RecordStatus.ACTIVE);
            family.setInvalidBy(null);
            family.setInvalidAt(null);
            family.setDeletePlannedAt(null);
            family.setRestoredBy(updatedBy);
            family.setRestoredAt(LocalDateTime.now());

            familyRepository.save(family);
            log.info("Family {} restored (soft delete off)", family.getId());
        } catch (Exception e) {
            log.error("Error while restoring family with Id: {}, error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error while restoring family with Id: " + id, e);
        }
    }

    //admin and lead
    public void archiveFamily(Long id, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 80)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Family family = familyRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Family with Id: " + id + " not found"));
            if (family.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Family with id: {} is already archived.", id);
                throw new RuntimeException("Family with id: " + id + " is already archived");
            }

            family.setStatus(RecordStatus.ARCHIVED);
            family.setArchivedBy(updatedBy);
            family.setArchivedAt(LocalDateTime.now());

            if (family.getInvalidAt() != null) {
                family.setDeletePlannedAt(family.getInvalidAt().plusYears(10));
                family.setInvalidAt(null);
                family.setInvalidBy(null);
            } else if (family.getCaseClosedAt() != null) {
                family.setDeletePlannedAt(family.getCaseClosedAt().plusYears(10));
            } else family.setDeletePlannedAt(family.getArchivedAt().plusYears(10));

            familyRepository.save(family);
            accessLog.log(updatedBy, "FAMILY_ARCHIVE", "Family", family.getId(),
                    "Family archived.", ip, userBrowser);
            log.info("Family {} archived.", family.getId());
        } catch (Exception e) {
            log.error("Error while archiving family with Id: {}, error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error while archiving family with Id: " + id, e);
        }
    }

    //admin and lead
    public void unarchiveFamily(Long id, String restoreReason, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 80)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Family family = familyRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Family with Id: " + id + " not found"));
            validator.validateText(restoreReason, 255);
            if (!family.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Family with id: {} is not archived.", id);
                throw new RuntimeException("Family with id: " + id + " is not archived");
            }
            family.setStatus(RecordStatus.ACTIVE);
            family.setArchivedBy(null);
            family.setArchivedAt(null);
            family.setDeletePlannedAt(null);
            family.setRestoredBy(updatedBy);
            family.setRestoredAt(LocalDateTime.now());
            family.setRestoredReason(restoreReason);

            familyRepository.save(family);
            accessLog.log(updatedBy, "FAMILY_UNARCHIVE", "Family", family.getId(),
                    "Family unarchived. Reason: " + restoreReason, ip, userBrowser);
            log.info("Family {} unarchived. Reason: {}", family.getId(), restoreReason);
        } catch (Exception e) {
            log.error("Error while unarchiving family with Id: {}, error: ", id, e);
            throw new RuntimeException("Error while unarchiving family with Id: " + id, e);
        }
    }

    //admin and lead
    public void blockFamily(Long id, String blockReason, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 80)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Family family = familyRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Family with Id: " + id + " not found"));

            validator.validateText(blockReason, 255);
            if (family.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Family with id: {} is already archived.", id);
                throw new RuntimeException("Family with id: " + id + " is already archived");
            }
            if (family.getStatus().equals(RecordStatus.INVALID)) {
                log.warn("Family with id: {} is already invalid (soft delete).", id);
                throw new RuntimeException("Family with id: " + id + " is already invalid");
            }
            if (family.getStatus().equals(RecordStatus.BLOCKED)) {
                log.warn("Family with id: {} is already blocked.", id);
                throw new RuntimeException("Family with id: " + id + " is already blocked");
            }
            family.setStatus(RecordStatus.BLOCKED);
            family.setBlockedReason(blockReason);
            family.setBlockedAt(LocalDateTime.now());
            family.setBlockedBy(updatedBy);

            familyRepository.save(family);
            accessLog.log(updatedBy, "FAMILY_BLOCK", "Family", family.getId(),
                    "Family blocked. Reason: " + blockReason, ip, userBrowser);
            log.info("Family {} blocked. Reason: {}", family.getId(), blockReason);
        } catch (Exception e) {
            log.error("Error while blocking family with Id: {}, error: ", id, e);
            throw new RuntimeException("Error while blocking family with Id: " + id, e);
        }
    }

    //admin and lead
    public void unblockFamily(Long id, String restoreReason, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 80)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Family family = familyRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Family with Id: " + id + " not found"));

            validator.validateText(restoreReason, 255);
            if (!family.getStatus().equals(RecordStatus.BLOCKED)) {
                log.warn("Family with id: {} is not blocked.", id);
                throw new RuntimeException("Family with id: " + id + " is not blocked");
            }
            family.setStatus(RecordStatus.ACTIVE);
            family.setBlockedAt(null);
            family.setBlockedBy(null);
            family.setRestoredReason(restoreReason);
            family.setRestoredBy(updatedBy);
            family.setRestoredAt(LocalDateTime.now());

            familyRepository.save(family);
            accessLog.log(updatedBy, "FAMILY_UNBLOCKED", "Family", family.getId(),
                    "Family unblocked. Reason: " + restoreReason, ip, userBrowser);
            log.info("Family {} unblocked. Reason: {}", family.getId(), restoreReason);
        } catch (Exception e) {
            log.error("Error while unblocking family with Id: {}, error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error while unblocking family with Id: " + id, e);
        }
    }

    //admin and lead and readonly
    public Family getFamily(String login, Long familyId) {
        validator.validateText(login, 255);
        int accessLevel = employeeService.findByLogin(login).getRole().getAccessLevel();
        if (accessLevel < 80 && accessLevel > 10) {
            log.error("Access Denied for {}", login);
            throw new RuntimeException("Access Denied for " + login);
        }
        return familyRepository.findById(familyId).orElseThrow(() ->
                new RuntimeException("Family with Id: " + familyId + " not found"));
    }

    //admin and lead and readonly
    public List<Family> getFamilies(String login) {
        validator.validateText(login, 255);
        int accessLevel = employeeService.findByLogin(login).getRole().getAccessLevel();
        if (accessLevel < 80 && accessLevel > 10) {
            log.error("Access Denied for {}", login);
            throw new RuntimeException("Access Denied for " + login);
        }
        return familyRepository.findAll();
    }

    //all
    public Family getFamilyByAssignedEmployee(String login, Long familyId) {
        if (familyId == null) {
            log.error("Family id is null");
            throw new RuntimeException("Family id is null");
        }
        Family family = familyRepository.findById(familyId).orElseThrow(() ->
                new RuntimeException("Family with Id: " + familyId + " not found"));
        if (!family.getAssignedEmployee().getLogin().equals(login) &&
                !delegationService.hasAccessForUpdate(login, family)) {
            log.error("Access Denied for {}, no rights to this family", login);
            throw new RuntimeException("Access Denied for " + login);
        }
        return family;
    }

    //all
    public List<Family> getFamiliesByAssignedEmployee(String login, Long assignedEmployeeId) {
        validator.validateText(login, 255);
        Employee requester = employeeService.findByLogin(login);

        if (requester == null) {
            log.error("Employee with login {} not found", login);
            throw new RuntimeException("Employee with login: " + login + " not found");
        }

        //admin and lead and readonly
        if (!(requester.getRole().getAccessLevel() < 80 && requester.getRole().getAccessLevel() > 10)) {
            return familyRepository.findFamiliesByAssignedEmployeeId(assignedEmployeeId);
        }

        //consultant
        if (requester.getRole().getAccessLevel() == 50) {
            if (requester.getId().equals(assignedEmployeeId)) {
                List<Family> families = familyRepository.findFamiliesByAssignedEmployeeId(requester.getId());
                if (families.isEmpty()) {
                    log.error("Family assigned employee with login {} not found", login);
                    throw new RuntimeException("Family assigned employee with login: " + login + " not found");
                }
                List<Family> result = new ArrayList<>(families);
                for (Family family : families) {
                    if (family.getStatus().equals(RecordStatus.INVALID)) {
                        long days = java.time.Duration.between(family.getInvalidAt(), LocalDateTime.now()).toDays();
                        if (days > 14) {
                            result.remove(family);
                        }
                    }
                }
                return result;
            }
        }
        log.error("Access Denied for {}", login);
        throw new RuntimeException("Access Denied for " + login);
    }

    //all
    public Family getDelegatedFamily(String login, Long familyId) {
        validator.validateText(login, 255);
        if (familyId == null) {
            log.error("Family id is null");
            throw new RuntimeException("Family id is null");
        }

        Employee requester = employeeService.findByLogin(login);
        if (requester == null) {
            log.error("Employee with login {} not found", login);
            throw new RuntimeException("Employee with login: " + login + " not found");
        }

        //admin and lead and readonly
        if (!(requester.getRole().getAccessLevel() < 80 && requester.getRole().getAccessLevel() > 10)) {
            return familyRepository.findById(familyId).orElseThrow(() ->
                    new RuntimeException("Family with Id: " + familyId + " not found"));
        }

        //consultant
        Delegation delegation = delegationService
                .getDelegationByToEmployeeAndFamily(requester.getLogin(),
                        familyRepository.findById(familyId)
                                .orElseThrow(() ->
                                        new RuntimeException("Family with Id: " + familyId + " not found")));

        if (delegation == null) {
            log.error("Access Denied for {}, no delegated access", login);
            throw new RuntimeException("Access Denied for " + login);
        }

        return delegation.getFamily();
    }

    //all
    public List<Family> getDelegatedFamilies(String login) {
        validator.validateText(login, 255);

        Employee requester = employeeService.findByLogin(login);
        if (requester == null) {
            log.error("Employee with login {} not found", login);
            throw new RuntimeException("Employee with login: " + login + " not found");
        }

        //admin and lead and readonly
        if (!(requester.getRole().getAccessLevel() < 80 && requester.getRole().getAccessLevel() > 10)) {
            List<Delegation> delegations = delegationService.getDelegations();
            return delegations.stream().map(Delegation::getFamily).toList();
        }

        //consultant
        List<Delegation> delegations = delegationService.getDelegationsByToEmployee(requester);
        List<Family> families = new ArrayList<>();
        for (Delegation delegation : delegations) {
            if (delegation.getEndDate().isBefore(LocalDate.now())) {
                families.add(delegation.getFamily());
            }
        }
        return families;
    }
}