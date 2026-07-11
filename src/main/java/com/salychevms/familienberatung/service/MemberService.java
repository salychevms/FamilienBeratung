package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Member;
import com.salychevms.familienberatung.enums.RecordStatus;
import com.salychevms.familienberatung.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final EmployeeService employeeService;
    private final ValidationService validator;
    private final AccessLogService accessLog;

    //admin and lead and consultant
    public void createMember(String zeusId, LocalDate joinedAt, LocalDate birthdate, String firstName, String lastName,
                             String street, String houseNumber, String zip, String city, String phone, String email,
                             String reasonDescription, String notes, Employee assignedEmployee,
                             String createdBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(createdBy, 50)) {
                log.error("Access Denied for {}", createdBy);
                throw new RuntimeException("Access Denied for " + createdBy);
            }

            validator.validateBirthday(birthdate);
            validator.validateText(firstName, 255);
            validator.validateText(lastName, 255);
            validator.validateText(street, 255);
            validator.validateText(houseNumber, 255);
            validator.validateText(zip, 255);
            validator.validateText(city, 255);
            validator.validateEmail(email);
            validator.validateText(reasonDescription, 4000);
            validator.validateText(notes, 255);
            validator.validateIp(ip);

            if (joinedAt.isAfter(LocalDate.now())
                    || joinedAt.isBefore(LocalDate.of(2026, 1, 1))) {
                log.error("Joined Date is before 2026");
                throw new RuntimeException("Joined Date is before 2026");
            }
            if (assignedEmployee == null) {
                log.error("Employee not found");
                throw new EntityNotFoundException("Employee not found");
            }
            Employee e = employeeService.findByLogin(assignedEmployee.getLogin());
            if (e == null) {
                log.error("Employee {} not found ", assignedEmployee.getLogin());
            }

            Member member = new Member();

            member.setBirthDate(birthdate);
            member.setJoinedAt(joinedAt);
            member.setZeusId(zeusId);
            member.setFirstName(firstName);
            member.setLastName(lastName);
            member.setStreet(street);
            member.setHouseNumber(houseNumber);
            member.setZip(zip);
            member.setCity(city);
            member.setPhone(phone);
            member.setEmail(email);
            member.setReasonDescription(reasonDescription);
            member.setNotes(notes);
            member.setAssignedEmployee(e);

            member.setStatus(RecordStatus.ACTIVE);
            member.setCaseClosed(false);
            member.setCreatedAt(LocalDateTime.now());
            member.setCreatedBy(createdBy);
            //TODO
            System.out.println(member);
            Member saved = memberRepository.save(member);

            accessLog.log(createdBy, "MEMBER_CREATE", "Member", saved.getId(),
                    "Created member " + firstName + " " + lastName, ip, userBrowser);
            log.info("Member {} created by {}", firstName + " " + lastName, createdBy);
        } catch (Exception e) {
            log.error("Error while saving member {}", firstName + " " + lastName, e);
            throw new RuntimeException("Error while saving member " + firstName + " " + lastName, e);
        }
    }

    //admin and lead and consultant and delegated
    public void updateMember(Member m, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 50)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            if (m == null) {
                log.error("Member not found");
                throw new EntityNotFoundException("Member not found");
            }
            if (m.getJoinedAt().isAfter(LocalDate.now())
                    || m.getJoinedAt().isBefore(LocalDate.of(2026, 1, 1))) {
                log.error("Joined Date is before 2026");
                throw new RuntimeException("Joined Date is before 2026");
            }
            Member member = memberRepository.findById(m.getId()).orElseThrow(() ->
                    new EntityNotFoundException("Member with Id: " + m.getId() + " not found"));

            validator.validateBirthday(m.getBirthDate());
            validator.validateText(m.getFirstName(), 255);
            validator.validateText(m.getLastName(), 255);
            validator.validateText(m.getStreet(), 255);
            validator.validateText(m.getHouseNumber(), 255);
            validator.validateText(m.getZip(), 255);
            validator.validateText(m.getCity(), 255);
            validator.validateEmail(m.getEmail());
            validator.validateText(m.getReasonDescription(), 4000);
            validator.validateText(m.getNotes(), 255);
            validator.validateIp(ip);

            member.setBirthDate(m.getBirthDate());
            member.setJoinedAt(m.getJoinedAt());
            member.setZeusId(m.getZeusId());
            member.setLastName(m.getFirstName());
            member.setLastName(m.getLastName());
            member.setStreet(m.getStreet());
            member.setHouseNumber(m.getHouseNumber());
            member.setZip(m.getZip());
            member.setCity(m.getCity());
            member.setPhone(m.getPhone());
            member.setEmail(m.getEmail());
            member.setReasonDescription(m.getReasonDescription());
            member.setNotes(m.getNotes());

            member.setUpdatedAt(LocalDateTime.now());
            member.setUpdatedBy(updatedBy);

            Member saved = memberRepository.save(member);

            accessLog.log(updatedBy, "MEMBER_UPDATE", "Member", saved.getId(),
                    "Updated member " + member.getFirstName() + " " + member.getLastName(), ip, userBrowser);
            log.info("Member {} updated by {}", member.getFirstName() + " " + member.getLastName(), updatedBy);
        } catch (Exception e) {
            log.error("Error while saving member, error: {}", e.getMessage(), e);
            throw new RuntimeException("Error while saving member ", e);
        }
    }

    //admin and lead
    public void updateAssignedEmployee(Long id, Long assignedEmployeeId, String updatedBy, String ip,
                                       String userBrowser) {
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
            Member member = memberRepository.findById(id).orElseThrow(() ->
                    new RuntimeException("Member with Id: " + id + " not found"));

            Employee employee = employeeService.findById(assignedEmployeeId);
            if (employee == null) {
                log.error("Employee with Id: {} not found", assignedEmployeeId);
                throw new RuntimeException("Employee with Id: " + assignedEmployeeId + " not found");
            }
            if (member.getAssignedEmployee().getId().equals(assignedEmployeeId)) {
                log.error("Assigned employee with Id: {} already assigned for member with Id: {}", assignedEmployeeId,
                        member.getId());
                throw new RuntimeException("Assigned employee with Id: " + assignedEmployeeId +
                        " already assigned for member with Id: " + member.getId());
            }

            member.setAssignedEmployee(employee);
            member.setUpdatedAt(LocalDateTime.now());
            member.setUpdatedBy(updatedBy);

            memberRepository.save(member);
            accessLog.log(updatedBy, "MEMBER_UPDATE ASSIGNED_EMPLOYEE", "Member", member.getId(),
                    "Member has new assigned employee with Id: " + assignedEmployeeId, ip, userBrowser);
            log.info("Member assigned employee has been changed. Member Id: {}, new employee id: {}, updater id: {}",
                    member.getId(), assignedEmployeeId, updatedBy);

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
            Member member = memberRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Member with Id: " + id + " not found"));
            if (member.isCaseClosed()) {
                log.warn("Case for member with id: {} is already closed.", id);
                throw new RuntimeException("Case for member with id: " + id + " is already closed");
            }
            if (member.getStatus().equals(RecordStatus.BLOCKED)) {
                log.warn("Case for member with id: {} is blocked.", id);
                throw new RuntimeException("Case for member with id: " + id + " is blocked");
            }
            if (member.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Case for member with id: {} is archived.", id);
                throw new RuntimeException("Case for member with id: " + id + " is archived");
            }
            if (member.getStatus().equals(RecordStatus.INVALID)) {
                log.warn("Case for member with id: {} is invalid.", id);
                throw new RuntimeException("Case for member with id: " + id + " is invalid");
            }

            member.setCaseClosed(true);
            member.setCaseClosedAt(LocalDateTime.now());
            member.setDeletePlannedAt(member.getCaseClosedAt().plusYears(10));
            member.setUpdatedAt(LocalDateTime.now());
            member.setUpdatedBy(updatedBy);

            memberRepository.save(member);
            accessLog.log(updatedBy, "MEBER_CLOSE_CASE", "Member", member.getId(),
                    "Case closed", ip, userBrowser);

            log.info("Member {} case closed by {}", member.getId(), updatedBy);
        } catch (Exception e) {
            log.error("Error while closing member case. Member Id: {}, error: ", id, e);
            throw new RuntimeException("Error while closing member case. Member Id: " + id, e);
        }
    }

    //admin and lead and consultant and delegated
    public void openCaseBack(Long id, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 50)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Member member = memberRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Member with Id: " + id + " not found"));
            if (!member.isCaseClosed()) {
                log.error("Member case still open, member id: {}", id);
                throw new RuntimeException("Member case still open, member id: " + id);
            }
            if (member.getStatus().equals(RecordStatus.BLOCKED)) {
                log.warn("Case for member with id: {} is blocked.", id);
                throw new RuntimeException("Case for member with id: " + id + " is blocked");
            }
            if (member.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Case for member with id: {} is archived.", id);
                throw new RuntimeException("Case for member with id: " + id + " is archived");
            }
            if (member.getStatus().equals(RecordStatus.INVALID)) {
                log.warn("Case for member with id: {} is invalid.", id);
                throw new RuntimeException("Case for member with id: " + id + " is invalid");
            }
            member.setCaseClosed(false);
            member.setCaseClosedAt(null);
            member.setDeletePlannedAt(null);
            member.setUpdatedAt(LocalDateTime.now());
            member.setUpdatedBy(updatedBy);

            memberRepository.save(member);
            accessLog.log(updatedBy, "MEMBER_CASE_OPEN_BACK", "Member", member.getId(),
                    "Member case opened back", ip, userBrowser);
            log.info("Member {} case opened back by {}", member.getId(), updatedBy);
        } catch (Exception e) {
            log.error("Failed to open member case back. Member Id: {}, error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to open member case back. Member Id: " + id, e);
        }
    }

    //admin and lead and consultant and delegated
    public void invalidateMember(Long id, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 50)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Member member = memberRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Member with Id: " + id + " not found"));

            if (member.isCaseClosed()) {
                log.warn("Case for member with id: {} is already closed.", id);
                throw new RuntimeException("Case for member with id: " + id + " is already closed");
            }
            if (member.getStatus().equals(RecordStatus.INVALID)) {
                log.warn("Member with id: {} is already invalid (soft delete).", id);
                throw new RuntimeException("Member with id: " + id + " is already invalid (soft delete)");
            }
            if (member.getStatus().equals(RecordStatus.BLOCKED)) {
                log.warn("Member with id: {} is already blocked.", id);
                throw new RuntimeException("Member with id: " + id + " is already blocked");
            }
            if (member.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Member with id: {} is already archived.", id);
                throw new RuntimeException("Member with id: " + id + " is already archived");
            }

            member.setStatus(RecordStatus.INVALID);
            member.setInvalidBy(updatedBy);
            member.setInvalidAt(LocalDateTime.now());
            member.setDeletePlannedAt(member.getInvalidAt().plusYears(10));

            memberRepository.save(member);

            accessLog.log(updatedBy, "MEMBER_INVALID_(SOFT_DELETE)", "Member", member.getId(),
                    "Member is invalid (soft delete)", ip, userBrowser);

            log.info("Member {} is invalid (soft delete) by {}", member.getId(), updatedBy);
        } catch (Exception e) {
            log.error("Error while making member invalid (soft delete). Member Id: {}, error: ", id, e);
            throw new RuntimeException("Error while making member invalid (soft delete). Member Id: " + id, e);
        }
    }

    //admin and lead
    public void restoreMember(Long id, String updatedBy, String restoreReason, String ip, String userBrowser) {
        try {
            Member member = memberRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Member with Id: " + id + " not found"));
            if (!member.getStatus().equals(RecordStatus.INVALID)) {
                log.warn("Member with id: {} is not invalid.", id);
                throw new RuntimeException("Member with id: " + id + " is not invalid");
            }

            Employee employee = employeeService.findByLogin(updatedBy);
            if (employee.getRole().getAccessLevel() <= 10) {
                log.error("Access Denied for {}. Role is {}", updatedBy, employee.getRole().getName());
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            long days = java.time.Duration.between(member.getInvalidAt(), LocalDateTime.now()).toDays();

            if (days > 14) {
                if (employee.getRole().getAccessLevel() == 50) {
                    log.error("Access Denied for {} after 14 days", updatedBy);
                    throw new RuntimeException("Access Denied for " + updatedBy + " after 14 days");
                } else if (employee.getRole().getAccessLevel() >= 80) {
                    validator.validateText(restoreReason, 255);
                    member.setRestoredReason(restoreReason);
                    log.info("Employee {} has been restored Member {} after 14 days. Reason: {}",
                            employee.getLogin(), member.getId(), restoreReason);
                    accessLog.log(updatedBy, "MEMBER_RESTORE_AFTER_14_DAYS", "Member", member.getId(),
                            "Member has been restored after 14 days. Reason: " + restoreReason, ip, userBrowser);
                }
            } else {
                member.setRestoredReason("Restore permitted before 14 days");
                log.info("Member {} has been restored by Employee {} before 14 days.", member.getId(), employee.getLogin());
            }

            member.setStatus(RecordStatus.ACTIVE);
            member.setInvalidBy(null);
            member.setInvalidAt(null);
            member.setDeletePlannedAt(null);
            member.setRestoredBy(updatedBy);
            member.setRestoredAt(LocalDateTime.now());

            memberRepository.save(member);
            log.info("Member {} restored (soft delete off)", member.getId());
        } catch (Exception e) {
            log.error("Error while restoring member with Id: {}, error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error while restoring member with Id: " + id, e);
        }
    }

    //admin and lead
    public void archiveMember(Long id, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 50)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Member member = memberRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Member with Id: " + id + " not found"));
            if (member.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Member with id: {} is already archived.", id);
                throw new RuntimeException("Member with id: " + id + " is already archived");
            }

            member.setStatus(RecordStatus.ARCHIVED);
            member.setArchivedBy(updatedBy);
            member.setArchivedAt(LocalDateTime.now());

            if (member.getInvalidAt() != null) {
                member.setDeletePlannedAt(member.getInvalidAt().plusYears(10));
                member.setInvalidAt(null);
                member.setInvalidBy(null);
            } else if (member.getCaseClosedAt() != null) {
                member.setDeletePlannedAt(member.getCaseClosedAt().plusYears(10));
            } else member.setDeletePlannedAt(member.getArchivedAt().plusYears(10));

            memberRepository.save(member);
            accessLog.log(updatedBy, "MEMBER_ARCHIVE", "Member", member.getId(),
                    "Member archived.", ip, userBrowser);
            log.info("Member {} archived.", member.getId());
        } catch (Exception e) {
            log.error("Error while archiving member with Id: {}, error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error while archiving member with Id: " + id, e);
        }
    }

    //admin and lead
    public void unarchiveMember(Long id, String restoreReason, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 50)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Member member = memberRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Member with Id: " + id + " not found"));
            validator.validateText(restoreReason, 255);
            if (!member.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Member with id: {} is not archived.", id);
                throw new RuntimeException("Member with id: " + id + " is not archived");
            }
            member.setStatus(RecordStatus.ACTIVE);
            member.setArchivedBy(null);
            member.setArchivedAt(null);
            member.setDeletePlannedAt(null);
            member.setRestoredBy(updatedBy);
            member.setRestoredAt(LocalDateTime.now());
            member.setRestoredReason(restoreReason);

            memberRepository.save(member);
            accessLog.log(updatedBy, "Member_UNARCHIVE", "Member", member.getId(),
                    "Member unarchived. Reason: " + restoreReason, ip, userBrowser);
            log.info("Member {} unarchived. Reason: {}", member.getId(), restoreReason);
        } catch (Exception e) {
            log.error("Error while unarchiving member with Id: {}, error: ", id, e);
            throw new RuntimeException("Error while unarchiving member with Id: " + id, e);
        }
    }

    //admin and lead
    public void blockMember(Long id, String blockReason, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 80)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Member member = memberRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Member with Id: " + id + " not found"));

            validator.validateText(blockReason, 255);
            if (member.getStatus().equals(RecordStatus.ARCHIVED)) {
                log.warn("Member with id: {} is already archived.", id);
                throw new RuntimeException("Member with id: " + id + " is already archived");
            }
            if (member.getStatus().equals(RecordStatus.INVALID)) {
                log.warn("Member with id: {} is already invalid (soft delete).", id);
                throw new RuntimeException("Member with id: " + id + " is already invalid");
            }
            if (member.getStatus().equals(RecordStatus.BLOCKED)) {
                log.warn("Member with id: {} is already blocked.", id);
                throw new RuntimeException("Member with id: " + id + " is already blocked");
            }
            member.setStatus(RecordStatus.BLOCKED);
            member.setBlockedReason(blockReason);
            member.setBlockedAt(LocalDateTime.now());
            member.setBlockedBy(updatedBy);

            memberRepository.save(member);
            accessLog.log(updatedBy, "MEMBER_BLOCK", "Member", member.getId(),
                    "Member blocked. Reason: " + blockReason, ip, userBrowser);
            log.info("Member {} blocked. Reason: {}", member.getId(), blockReason);
        } catch (Exception e) {
            log.error("Error while blocking member with Id: {}, error: ", id, e);
            throw new RuntimeException("Error while blocking member with Id: " + id, e);
        }
    }

    //admin and lead
    public void unblockMember(Long id, String restoreReason, String updatedBy, String ip, String userBrowser) {
        try {
            if (!employeeService.hasAccess(updatedBy, 80)) {
                log.error("Access Denied for {}", updatedBy);
                throw new RuntimeException("Access Denied for " + updatedBy);
            }

            Member member = memberRepository.findById(id).orElseThrow(() ->
                    new EntityNotFoundException("Member with Id: " + id + " not found"));

            validator.validateText(restoreReason, 255);
            if (!member.getStatus().equals(RecordStatus.BLOCKED)) {
                log.warn("Member with id: {} is not blocked.", id);
                throw new RuntimeException("Member with id: " + id + " is not blocked");
            }
            member.setStatus(RecordStatus.ACTIVE);
            member.setBlockedAt(null);
            member.setBlockedBy(null);
            member.setRestoredReason(restoreReason);
            member.setRestoredBy(updatedBy);
            member.setRestoredAt(LocalDateTime.now());

            memberRepository.save(member);
            accessLog.log(updatedBy, "MEMBER_UNBLOCKED", "Member", member.getId(),
                    "Member unblocked. Reason: " + restoreReason, ip, userBrowser);
            log.info("Member {} unblocked. Reason: {}", member.getId(), restoreReason);
        } catch (Exception e) {
            log.error("Error while unblocking member with Id: {}, error: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error while unblocking member with Id: " + id, e);
        }
    }

    //admin and lead and readonly
    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() ->
                new RuntimeException("Member with Id: " + memberId + " not found"));
    }

    //admin and lead and readonly
    public List<Member> getMembers() {
        return memberRepository.findAll();
    }


    //all
    public List<Member> getMembersByAssignedEmployee(String login, Employee assignedEmployee) {
        validator.validateText(login, 255);
        Employee requester = employeeService.findByLogin(login);
        if (assignedEmployee == null) {
            log.error("AssignedEmployee is null");
        }
        Employee exists = employeeService.findByLogin(assignedEmployee.getLogin());
        if (exists == null) {
            log.error("AssignedEmployee not found");
        }
        if (requester == null) {
            log.error("Employee (requester) not found");
        }
        int lvl = requester.getRole().getAccessLevel();

        //admin and lead and readonly
        if (lvl == 100 || lvl == 80 || lvl == 10)
            return memberRepository.findAllByAssignedEmployee(exists);

        //consultant
        if (lvl == 50) {
            List<Member> members = memberRepository.findAllByAssignedEmployee(exists);
            List<Member> response = new ArrayList<>();
            for (Member m : members) {
                if (m.getStatus().equals(RecordStatus.INVALID)) {
                    long days = Duration.between(m.getInvalidAt(), LocalDateTime.now()).toDays();
                    if (days > 14) continue;
                }
                response.add(m);
            }
            return response;
        }
        return List.of();
    }
}