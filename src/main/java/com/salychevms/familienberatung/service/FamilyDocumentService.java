package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.repository.FamilyDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FamilyDocumentService {

    private final FamilyDocumentRepository familyDocumentRepository;
    private final EmployeeService employeeService;
    private final AccessLogService accessLog;
    private final ValidationService validator;

    @Value("${storage.base-path}")
    private String storageBasePath;

    public FamilyDocument uploadDocument(Family family, MultipartFile file, String description, Employee employee,
                                         String ip, String browser) {

        try {
            if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
                log.error("Family {} status is not ACTIVE. Actual status: {}", family.getId(), family.getStatus());
                throw new RuntimeException("Family " + family.getId() + " status is not ACTIVE. Actual status: " + family.getStatus());
            }
            if (!employeeService.hasAccess(employee.getLogin(), 50)) {
                log.error("Employee {} has no access to upload FamilyDocument", employee.getLogin());
                throw new RuntimeException("Employee " + employee.getLogin() + " has no access to upload FamilyDocument");
            }

            validator.validateText(description, 4000);

            String original = file.getOriginalFilename();
            String stored = System.currentTimeMillis() + "_" + original;

            try {
                storeFile(file, family.getId(), stored);
            } catch (Exception e) {
                log.error("Failed to store file. Error: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to store file. Error: " + e.getMessage());
            }

            FamilyDocument document = new FamilyDocument();
            document.setFamily(family);
            document.setUploadedByEmployee(employee);
            document.setOriginalFileName(original);
            document.setStoredFileName(stored);
            String fileType = file.getContentType() != null ?
                    file.getContentType() :
                    "application/octet-stream";
            document.setFileType(fileType);
            document.setFileSizeBytes(file.getSize());
            document.setDescription(description);
            document.setUploadedAt(LocalDateTime.now());

            FamilyDocument saved = familyDocumentRepository.save(document);

            accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_UPLOAD", "FamilyDocument", saved.getId(),
                    "New Family Document has been uploaded: " + original, ip, browser);
            log.info("Family Document has been uploaded: {}", original);
            return saved;
        } catch (Exception e) {
            log.error("Failed to create FamilyDocument. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create FamilyDocument. Error: " + e.getMessage(), e);
        }
    }

    public FamilyDocument updateDescription(Family family, FamilyDocument doc, String description,
                                            Employee employee, String ip, String browser) {
        try {
            if (!doc.getFamily().equals(family)) {
                log.error("Document does not belong to Family");
                throw new RuntimeException("Document does not belong to Family");
            }
            if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
                log.error("Family {} status is not ACTIVE. Actual status: {}", family.getId(), family.getStatus());
                throw new RuntimeException("Family " + family.getId() + " status is not ACTIVE. Actual status: " + family.getStatus());
            }
            if (!employeeService.hasAccess(employee.getLogin(), 50)) {
                log.error("Employee {} has no access to update FamilyDocument", employee.getLogin());
                throw new RuntimeException("Employee " + employee.getLogin() + " has no access to update FamilyDocument");
            }
            if (doc.isInvalid()) {
                log.error("Family document is invalid. Document name: {}", doc.getOriginalFileName());
                throw new RuntimeException("Family document is invalid. Document name: " + doc.getOriginalFileName());
            }

            validator.validateText(description, 4000);

            doc.setDescription(description);
            doc.setUpdatedAt(LocalDateTime.now());
            doc.setUpdatedBy(employee.getLogin());

            FamilyDocument updated = familyDocumentRepository.save(doc);

            accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_DESCRIPTION_UPDATE", "FamilyDocument",
                    updated.getId(), "FamilyDocument description has been updated: " + description, ip, browser);
            log.info("FamilyDocument {} has been updated: description {}", updated.getId(), description);
            return updated;
        } catch (Exception e) {
            log.error("Failed to update description. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update description. Error: " + e.getMessage(), e);
        }
    }

    public FamilyDocument getDocument(Family family, Employee employee, Long documentId) {
        FamilyDocument doc = familyDocumentRepository.findById(documentId).orElseThrow(() ->
                new RuntimeException("Family document with id: " + documentId + " not found"));
        if (!doc.getFamily().equals(family)) {
            log.error("Document does not belong to Family");
            throw new RuntimeException("Document does not belong to Family");
        }
        if (family.getStatus().equals(RecordStatus.BLOCKED) && employee.getRole().getAccessLevel() == 50) {
            log.error("Family {} status is {}", family.getId(), family.getStatus());
            throw new RuntimeException("Family " + family.getId() + " status is " + family.getStatus());
        }
        if (doc.isInvalid()) {
            log.error("Family document is invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Family document is invalid. Document name: " + doc.getOriginalFileName());
        }
        return doc;
    }

    public List<FamilyDocument> getDocumentsByFamily(Family family, Employee employee) {
        if (employee.getRole().getAccessLevel() == 50) {
            if (family.getStatus().equals(RecordStatus.BLOCKED)) {
                log.error("Family {} status is {}", family.getId(), family.getStatus());
                throw new RuntimeException("Family " + family.getId() + " status is " + family.getStatus());
            }else return familyDocumentRepository.findAllByFamilyIdAndIsInvalidFalse(family.getId(),  false);
        }else return familyDocumentRepository.findByFamilyId(family.getId());
    }

    public void invalidateDocument(Family family, FamilyDocument doc, Employee employee, String ip, String browser) {
        if (!doc.getFamily().equals(family)) {
            log.error("Document does not belong to Family");
            throw new RuntimeException("Document does not belong to Family");
        }
        if (employee.getRole().getAccessLevel() <= 10) {
            log.error("Access Denied for Employee role {}", employee.getRole().getName());
            throw new RuntimeException("Access Denied for Employee role: " + employee.getRole().getName());
        }
        if (!family.getStatus().equals(RecordStatus.ACTIVE) && employee.getRole().getAccessLevel() == 50) {
            log.error("Family {} status is not ACTIVE. Actual status: {}", family.getId(), family.getStatus());
            throw new RuntimeException("Family " + family.getId() +
                    " status is not ACTIVE. Actual status: " + family.getStatus());
        }
        if (doc.isInvalid()) {
            log.error("Family document is already invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Family document is already invalid. Document name: " + doc.getOriginalFileName());
        }

        doc.setInvalid(true);
        doc.setInvalidAt(LocalDateTime.now());
        doc.setInvalidBy(employee.getLogin());
        doc.setRestoredAt(null);
        doc.setRestoredBy(null);
        doc.setRestoredReason(null);

        familyDocumentRepository.save(doc);
        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_INVALIDATED_(SOFT_DELETE)", "FamilyDocument",
                doc.getId(), "FamilyDocument has been invalidated", ip, browser);
        log.info("FamilyDocument has been invalidated: {}", doc.getId());
    }

    public void restoreDocument(Family family, FamilyDocument doc, String restoreReason,
                                Employee employee, String ip, String browser) {
        if (!doc.getFamily().equals(family)) {
            log.error("Document does not belong to Family");
            throw new RuntimeException("Document does not belong to Family");
        }
        if (employee.getRole().getAccessLevel() <= 10) {
            log.error("Access Denied for Employee role {}", employee.getRole().getName());
            throw new RuntimeException("Access Denied for Employee role: " + employee.getRole().getName());
        }
        if (!family.getStatus().equals(RecordStatus.ACTIVE) && employee.getRole().getAccessLevel() == 50) {
            log.error("Family {} status is not ACTIVE. Actual status: {}", family.getId(), family.getStatus());
            throw new RuntimeException("Family " + family.getId() +
                    " status is not ACTIVE. Actual status: " + family.getStatus());
        }
        if (!doc.isInvalid()) {
            log.error("Family document is not invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Family document is not invalid. Document name: " + doc.getOriginalFileName());
        }
        validator.validateText(restoreReason, 255);

        doc.setInvalid(false);
        doc.setInvalidAt(null);
        doc.setInvalidBy(null);
        doc.setRestoredReason(restoreReason);
        doc.setRestoredAt(LocalDateTime.now());
        doc.setRestoredBy(employee.getLogin());

        familyDocumentRepository.save(doc);
        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_RESTORE", "FamilyDocument", doc.getId(),
                "FamilyDocument has been restored. Reason: " + restoreReason, ip, browser);
        log.info("FamilyDocument {} has been restored. Reason: {}", doc.getId(), restoreReason);
    }

    public Path downloadDocument(Family family, FamilyDocument doc, Employee employee, String ip, String browser) {
        if (!doc.getFamily().equals(family)) {
            log.error("Document does not belong to Family");
            throw new RuntimeException("Document does not belong to Family");
        }
        if (family.getStatus().equals(RecordStatus.BLOCKED)) {
            log.error("Family {} status is {}", family.getId(), family.getStatus());
            throw new RuntimeException("Family " + family.getId() + " status is " + family.getStatus());
        }
        if(employee.getRole().getAccessLevel() <= 10) {
            log.error("Access Denied for Employee role {}", employee.getRole().getName());
            throw new RuntimeException("Access Denied for Employee role: " + employee.getRole().getName());
        }
        if (doc.isInvalid()) {
            log.error("Family document is already invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Family document is already invalid. Document name: " + doc.getOriginalFileName());
        }

        Path path = getFamilyDir(family.getId()).resolve(doc.getStoredFileName());
        if (!Files.exists(path)) {
            log.error("Stored file for doc {} missing", doc.getId());
            throw new RuntimeException("Stored file for doc " + doc.getId());
        }

        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_DOWNLOAD", "FamilyDocument", doc.getId(),
                "FamilyDocument has been downloaded", ip, browser);
        log.info("FamilyDocument {} has been downloaded. Requester: {}", doc.getId(), employee.getLogin());
        return path;
    }

    private void storeFile(MultipartFile file, Long familyId, String storedFileName) throws IOException {
        Path dir = getFamilyDir(familyId);
        if (!dir.toFile().exists()) {
            Files.createDirectories(dir);
            log.info("Creating family document path with family id {}", familyId);
        }


        Path target = dir.resolve(storedFileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path getFamilyDir(Long familyId) {
        return Paths.get(storageBasePath, familyId.toString());
    }
}