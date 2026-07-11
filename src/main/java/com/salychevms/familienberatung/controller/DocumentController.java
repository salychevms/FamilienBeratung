package com.salychevms.familienberatung.controller;

import com.salychevms.familienberatung.enums.RecordStatus;
import com.salychevms.familienberatung.model.*;
import com.salychevms.familienberatung.service.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService familyDocumentService;
    private final MemberService memberService;
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

        Document doc;
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

        Member member = memberService.getMemberById(doc.getMember().getId());
        if (member == null || member.getStatus().equals(RecordStatus.INVALID)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (employee.getRole().getAccessLevel() == 50 && !member.getAssignedEmployee().equals(employee)) {
            Delegation delegation = null;
            List<Delegation> delegationList=delegationService.getDelegationsByToEmployeeAndFamily(employee, member);
            for(Delegation d:delegationList){
                if(delegationService.isDelegationActive(d)){
                    delegation=d;
                }
            }
            if (delegation==null || delegation.getToEmployee().equals(employee)) {
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

        Path filePath = Path.of(storageBasePath, member.getId().toString(), doc.getStoredFileName());

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
