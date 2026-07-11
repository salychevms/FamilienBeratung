package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.enums.RecordStatus;
import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final EmployeeService employeeService;
    private final AccessLogService accessLog;
    private final ValidationService validator;
    private final MemberService memberService;

    @Value("${storage.base-path}")
    private String storageBasePath;

    public Document uploadDocument(Member member, String originalName, String contentType, InputStream data,
                                   Employee employee, String ip, String browser) {
        if (!member.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Member {} status is not ACTIVE. Actual status: {}", member.getId(), member.getStatus());
            throw new RuntimeException("Member " + member.getId() + " status is not ACTIVE. Actual status: " + member.getStatus());
        }
        if (!employeeService.hasAccess(employee.getLogin(), 50)) {
            log.error("Employee {} has no access to upload Document", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access to upload Document");
        }

        String nameLower = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);

        if (!(nameLower.endsWith(".pdf") || nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg")
                || nameLower.endsWith(".doc") || nameLower.endsWith(".docx") || nameLower.endsWith("word")
                || nameLower.endsWith("sheet") || nameLower.endsWith(".xls") || nameLower.endsWith(".xlsx")
                || nameLower.endsWith("excel") || nameLower.endsWith(".png")
        )) {
            log.error("member document upload failed. Original name: {}", originalName);
            throw new RuntimeException("Member document upload failed. Original name: " + originalName);
        }

        String stored = System.currentTimeMillis() + "_" + originalName;

        long size;
        try {
            size=storeFile(data, member.getId(), stored);
        } catch (Exception e) {
            log.error("Failed to store file. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store file. Error: " + e.getMessage());
        }

        Document document = new Document();
        document.setMember(member);
        document.setUploadedByEmployee(employee);
        document.setOriginalFileName(originalName);
        document.setStoredFileName(stored);
        document.setFileType(contentType != null ? contentType : "application/octet-stream");
        document.setFileSizeBytes(size);
        document.setUploadedAt(LocalDateTime.now());

        Document saved = documentRepository.save(document);

        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_UPLOAD", "Document", saved.getId(),
                "New Member Document has been uploaded: " + originalName, ip, browser);
        log.info("Member Document has been uploaded: {}", originalName);
        return saved;
    }

    public Document updateName(Document doc, Document updated, Employee employee,
                               String ip, String browser) {
        if (!employeeService.hasAccess(employee.getLogin(), 50)) {
            log.error("Employee {} has no access to update Document", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access to update Document");
        }
        if (doc.isInvalid()) {
            log.error("Member document is invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Member document is invalid. Document name: " + doc.getOriginalFileName());
        }

        Document fDoc = documentRepository.findById(doc.getId()).orElse(null);
        if (fDoc == null) {
            log.error("Member document not found. Member document id: {}", doc.getId());
            throw new RuntimeException("Member document not found. Member document id: " + doc.getId());
        }

        validator.validateText(updated.getOriginalFileName(), 255);

        fDoc.setOriginalFileName(updated.getOriginalFileName());
        fDoc.setUpdatedAt(LocalDateTime.now());
        fDoc.setUpdatedBy(employee.getLogin());

        Document result = documentRepository.save(fDoc);

        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_NAME_UPDATE", "Document",
                updated.getId(), "Document Name has been updated: " + result.getOriginalFileName(), ip, browser);
        log.info("Document {} has been updated: Name {}", result.getId(), result.getOriginalFileName());
        return result;
    }

    public Document updateDescription(Document doc, Document updated, Employee employee,
                                      String ip, String browser) {
        if (!employeeService.hasAccess(employee.getLogin(), 50)) {
            log.error("Employee {} has no access to update Document", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access to update Document");
        }
        if (doc.isInvalid()) {
            log.error("Member document is invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Member document is invalid. Document name: " + doc.getOriginalFileName());
        }

        Document fDoc = documentRepository.findById(doc.getId()).orElse(null);
        if (fDoc == null) {
            log.error("Member document not found. Member document id: {}", doc.getId());
            throw new RuntimeException("Member document not found. Member document id: " + doc.getId());
        }

        validator.validateText(updated.getDescription(), 4000);

        fDoc.setDescription(updated.getDescription());
        fDoc.setUpdatedAt(LocalDateTime.now());
        fDoc.setUpdatedBy(employee.getLogin());

        Document result = documentRepository.save(fDoc);

        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_DESCRIPTION_UPDATE", "Document",
                result.getId(), "Document description has been updated: "
                        + result.getDescription(), ip, browser);
        log.info("Document {} has been updated: description {}", updated.getId(), result.getDescription());
        return result;
    }

    public Document getDocument(Employee employee, Long documentId) {
        Document doc = documentRepository.findById(documentId).orElseThrow(() ->
                new RuntimeException("Member document with id: " + documentId + " not found"));

        Member member = memberService.getMemberById(doc.getMember().getId());
        if(member == null) {
            log.error("Member with id: " + doc.getMember().getId() + " not found");
            throw new RuntimeException("Member with id: " + doc.getMember().getId() + " not found");
        }

        if (member.getStatus().equals(RecordStatus.BLOCKED) && employee.getRole().getAccessLevel() == 50) {
            log.error("Member {} status is {}", member.getId(), member.getStatus());
            throw new RuntimeException("Member " + member.getId() + " status is " + member.getStatus());
        }
        if (doc.isInvalid()) {
            log.error("Member document is invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Member document is invalid. Document name: " + doc.getOriginalFileName());
        }
        return doc;
    }

    public List<Document> getDocumentsByFamily(Member member, Employee employee) {
        if (employee.getRole().getAccessLevel() == 50) {
            if (member.getStatus().equals(RecordStatus.BLOCKED)) {
                log.error("Member {} status is {}", member.getId(), member.getStatus());
                throw new RuntimeException("Member " + member.getId() + " status is " + member.getStatus());
            } else return documentRepository.findAllByMemberIdAndIsInvalidFalse(member.getId());
        } else return documentRepository.findByMemberId(member.getId());
    }

    public void invalidateDocument(Member member, Document doc, Employee employee, String ip, String browser) {
        if (!doc.getMember().equals(member)) {
            log.error("Document does not belong to Member");
            throw new RuntimeException("Document does not belong to Member");
        }
        if (employee.getRole().getAccessLevel() <= 10) {
            log.error("Access Denied for Employee role {}", employee.getRole().getName());
            throw new RuntimeException("Access Denied for Employee role: " + employee.getRole().getName());
        }
        if (!member.getStatus().equals(RecordStatus.ACTIVE) && employee.getRole().getAccessLevel() == 50) {
            log.error("Member {} status is not ACTIVE. Actual status: {}", member.getId(), member.getStatus());
            throw new RuntimeException("Member " + member.getId() +
                    " status is not ACTIVE. Actual status: " + member.getStatus());
        }
        if (doc.isInvalid()) {
            log.error("Member document is already invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Member document is already invalid. Document name: " + doc.getOriginalFileName());
        }

        doc.setInvalid(true);
        doc.setInvalidAt(LocalDateTime.now());
        doc.setInvalidBy(employee.getLogin());
        doc.setRestoredAt(null);
        doc.setRestoredBy(null);
        doc.setRestoredReason(null);

        documentRepository.save(doc);
        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_INVALIDATED_(SOFT_DELETE)", "Document",
                doc.getId(), "Document has been invalidated", ip, browser);
        log.info("Document has been invalidated: {}", doc.getId());
    }

    public void restoreDocument(Member member, Document doc, String restoreReason,
                                Employee employee, String ip, String browser) {
        if (!doc.getMember().equals(member)) {
            log.error("Document does not belong to Member");
            throw new RuntimeException("Document does not belong to Member");
        }
        if (employee.getRole().getAccessLevel() <= 10) {
            log.error("Access Denied for Employee role {}", employee.getRole().getName());
            throw new RuntimeException("Access Denied for Employee role: " + employee.getRole().getName());
        }
        if (!member.getStatus().equals(RecordStatus.ACTIVE) && employee.getRole().getAccessLevel() == 50) {
            log.error("Member {} status is not ACTIVE. Actual status: {}", member.getId(), member.getStatus());
            throw new RuntimeException("Member " + member.getId() +
                    " status is not ACTIVE. Actual status: " + member.getStatus());
        }
        if (!doc.isInvalid()) {
            log.error("Member document is not invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Member document is not invalid. Document name: " + doc.getOriginalFileName());
        }
        validator.validateText(restoreReason, 255);

        doc.setInvalid(false);
        doc.setInvalidAt(null);
        doc.setInvalidBy(null);
        doc.setRestoredReason(restoreReason);
        doc.setRestoredAt(LocalDateTime.now());
        doc.setRestoredBy(employee.getLogin());

        documentRepository.save(doc);
        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_RESTORE", "Document", doc.getId(),
                "Document has been restored. Reason: " + restoreReason, ip, browser);
        log.info("Document {} has been restored. Reason: {}", doc.getId(), restoreReason);
    }

    public List<Document> getAllDocuments(Employee employee) {
        if (employee == null)
            log.error("Employee is null");
        return documentRepository.findAll();
    }

    private long storeFile(InputStream file, Long familyId, String storedFileName) throws IOException {
        Path dir = getFamilyDir(familyId);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            log.info("Creating family document path with family id {}", familyId);
        }

        Path target = dir.resolve(storedFileName);
        long size;
        try (InputStream in = file) {
            size = Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return size;
    }

    private Path getFamilyDir(Long familyId) {
        return Paths.get(storageBasePath, familyId.toString());
    }
}