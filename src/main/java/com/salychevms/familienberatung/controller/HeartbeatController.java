package com.salychevms.familienberatung.controller;

import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PermitAll
public class HeartbeatController {

    @GetMapping("/heartbeat")
    public void heartbeat(HttpSession session) {
        session.setAttribute("lastActivity", System.currentTimeMillis());
    }
}
