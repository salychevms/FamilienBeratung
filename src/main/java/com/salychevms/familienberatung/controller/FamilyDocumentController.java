package com.salychevms.familienberatung.controller;

import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class FamilyDocumentController {
    private final FamilyDocumentService familyDocumentService;
    private final FamilyService familyService;
    private final EmployeeService employeeService;
    private final DelegationService delegationService;

    @Value("${storage.base-path}")
    private String storageBasePath;

    @GetMapping("/preview/{documentId}")
    public void preview(@PathVariable Long documentId, HttpServletResponse response, HttpSession session) throws Exception {
        String authorized = (String) session.getAttribute("AUTH_LOGIN");
        if (authorized == null || authorized.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Employee employee = employeeService.findByLogin(authorized);
        if (employee == null || employee.isArchived() || !employee.isActive()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        FamilyDocument doc;
        try {
            doc = familyDocumentService.getDocument(employee, documentId);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (doc == null || doc.isInvalid()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Family family = familyService.getFamilyById(doc.getFamily().getId());
        if (family == null || family.getStatus().equals(RecordStatus.INVALID)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (employee.getRole().getAccessLevel() == 50 && !family.getAssignedEmployee().equals(employee)) {
            Delegation delegation = delegationService.getDelegationByToEmployeeAndFamily(authorized, family);
            if (delegation == null || !delegationService.isDelegationActive(delegation)
                    || delegation.getToEmployee().equals(employee)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        if (employee.getRole().getAccessLevel() == 10) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (storageBasePath == null || storageBasePath.isBlank()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        Path filePath = Path.of(storageBasePath, family.getId().toString(), doc.getStoredFileName());

        if(!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(doc.getFileType());
        response.setHeader("Content-Disposition", "inline");
        response.setHeader("Cache-Control", "no-store");

        try (InputStream in = Files.newInputStream(filePath)) {
            in.transferTo(response.getOutputStream());
        }
    }
}
