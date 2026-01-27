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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    public FamilyDocument uploadDocument(Family family, String originalName, String contentType, InputStream data,
                                         Employee employee, String ip, String browser) {
        if (!family.getStatus().equals(RecordStatus.ACTIVE)) {
            log.error("Family {} status is not ACTIVE. Actual status: {}", family.getId(), family.getStatus());
            throw new RuntimeException("Family " + family.getId() + " status is not ACTIVE. Actual status: " + family.getStatus());
        }
        if (!employeeService.hasAccess(employee.getLogin(), 50)) {
            log.error("Employee {} has no access to upload FamilyDocument", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access to upload FamilyDocument");
        }

        String mime=contentType;
        String readable=toReadableType(mime);

        if(readable==null){
            log.error("Family document {} has no readable content type", family.getId());
            throw new RuntimeException("Dateityp nicht erlaubt: " + mime);
        }

        String stored = System.currentTimeMillis() + "_" + originalName;

        long size;
        try {
            size=storeFile(data, family.getId(), stored);
        } catch (Exception e) {
            log.error("Failed to store file. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to store file. Error: " + e.getMessage());
        }

        FamilyDocument document = new FamilyDocument();
        document.setFamily(family);
        document.setUploadedByEmployee(employee);
        document.setOriginalFileName(originalName);
        document.setStoredFileName(stored);
        document.setFileType(contentType!=null?contentType:"application/octet-stream");
        document.setFileSizeBytes(size);
        document.setUploadedAt(LocalDateTime.now());

        FamilyDocument saved = familyDocumentRepository.save(document);

        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_UPLOAD", "FamilyDocument", saved.getId(),
                "New Family Document has been uploaded: " + originalName, ip, browser);
        log.info("Family Document has been uploaded: {}", originalName);
        return saved;
    }

    public FamilyDocument updateName(FamilyDocument doc, FamilyDocument updated, Employee employee,
                                     String ip, String browser) {
        if (!employeeService.hasAccess(employee.getLogin(), 50)) {
            log.error("Employee {} has no access to update FamilyDocument", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access to update FamilyDocument");
        }
        if (doc.isInvalid()) {
            log.error("Family document is invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Family document is invalid. Document name: " + doc.getOriginalFileName());
        }

        FamilyDocument fDoc = familyDocumentRepository.findById(doc.getId()).orElse(null);
        if (fDoc == null) {
            log.error("Family document not found. Family document id: {}", doc.getId());
            throw new RuntimeException("Family document not found. Family document id: " + doc.getId());
        }

        validator.validateText(updated.getOriginalFileName(), 255);

        fDoc.setOriginalFileName(updated.getOriginalFileName());
        fDoc.setUpdatedAt(LocalDateTime.now());
        fDoc.setUpdatedBy(employee.getLogin());

        FamilyDocument result = familyDocumentRepository.save(fDoc);

        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_NAME_UPDATE", "FamilyDocument",
                updated.getId(), "FamilyDocument Name has been updated: " + result.getOriginalFileName(), ip, browser);
        log.info("FamilyDocument {} has been updated: Name {}", result.getId(), result.getOriginalFileName());
        return result;
    }

    public FamilyDocument updateDescription(FamilyDocument doc, FamilyDocument updated, Employee employee,
                                            String ip, String browser) {
        if (!employeeService.hasAccess(employee.getLogin(), 50)) {
            log.error("Employee {} has no access to update FamilyDocument", employee.getLogin());
            throw new RuntimeException("Employee " + employee.getLogin() + " has no access to update FamilyDocument");
        }
        if (doc.isInvalid()) {
            log.error("Family document is invalid. Document name: {}", doc.getOriginalFileName());
            throw new RuntimeException("Family document is invalid. Document name: " + doc.getOriginalFileName());
        }

        FamilyDocument fDoc = familyDocumentRepository.findById(doc.getId()).orElse(null);
        if (fDoc == null) {
            log.error("Family document not found. Family document id: {}", doc.getId());
            throw new RuntimeException("Family document not found. Family document id: " + doc.getId());
        }

        validator.validateText(updated.getDescription(), 4000);

        fDoc.setDescription(updated.getDescription());
        fDoc.setUpdatedAt(LocalDateTime.now());
        fDoc.setUpdatedBy(employee.getLogin());

        FamilyDocument result = familyDocumentRepository.save(fDoc);

        accessLog.log(employee.getLogin(), "FAMILY_DOCUMENT_DESCRIPTION_UPDATE", "FamilyDocument",
                result.getId(), "FamilyDocument description has been updated: "
                        + result.getDescription(), ip, browser);
        log.info("FamilyDocument {} has been updated: description {}", updated.getId(), result.getDescription());
        return result;
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
            } else return familyDocumentRepository.findAllByFamilyIdAndIsInvalidFalse(family.getId());
        } else return familyDocumentRepository.findByFamilyId(family.getId());
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
        if (employee.getRole().getAccessLevel() <= 10) {
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

    public List<FamilyDocument> getAllDocuments(Employee employee) {
        if (employee == null)
            log.error("Employee is null");
        return familyDocumentRepository.findAll();
    }

    private long storeFile(InputStream file, Long familyId, String storedFileName) throws IOException {
        Path dir = getFamilyDir(familyId);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            log.info("Creating family document path with family id {}", familyId);
        }

        Path target = dir.resolve(storedFileName);
        long size;
        try (InputStream in=file){
            size=Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return size;
    }

    private Path getFamilyDir(Long familyId) {
        return Paths.get(storageBasePath, familyId.toString());
    }

    private String toReadableType(String mime) {
        if(mime == null) return null;
        if (mime.contains("pdf")) return "PDF";
        if (mime.contains("doc") || mime.contains("docx") || mime.contains("word")) return "DOC/DOCX";
        if (mime.contains("xls") || mime.contains("xlsx") || mime.contains("excel") || mime.contains("sheet"))
            return "XLS/XLSX";
        if (mime.contains("jpeg") || mime.contains("jpg")) return "JPEG";
        if (mime.contains("png")) return "PNG";
        return null;
    }
}