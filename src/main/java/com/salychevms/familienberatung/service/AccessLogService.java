package com.salychevms.familienberatung.service;

import com.salychevms.familienberatung.model.AccessLog;
import com.salychevms.familienberatung.repository.AccessLogRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AccessLogService {

    private final AccessLogRepository accessLogRepository;

    public void log(String userLogin, String action, String entityType, Long entityId,
                    String description, String ip, String browser) {
        AccessLog accessLog = new AccessLog();
        accessLog.setTimestamp(LocalDateTime.now());
        accessLog.setUserLogin(userLogin);
        accessLog.setAction(action);
        accessLog.setEntityType(entityType);
        accessLog.setEntityId(entityId);
        accessLog.setDescription(description);
        accessLog.setIpAddress(ip);
        accessLog.setUserBrowser(browser);

        accessLogRepository.save(accessLog);
        log.info("AccessLog for: {} has been saved successfully at: {}", accessLog.getUserLogin(), accessLog.getTimestamp());
    }

    public void logSimple(String userLogin, String action, String entityType, Long entityId) {
        AccessLog accessLog = new AccessLog();
        accessLog.setTimestamp(LocalDateTime.now());
        accessLog.setUserLogin(userLogin);
        accessLog.setAction(action);
        accessLog.setEntityType(entityType);
        accessLog.setEntityId(entityId);
        accessLog.setDescription("empty");
        accessLog.setIpAddress("unknown");
        accessLog.setUserBrowser("unknown");

        accessLogRepository.save(accessLog);
        log.info("Simple AccessLog for {} has been saved successfully at: {}", accessLog.getUserLogin(), accessLog.getTimestamp());
    }
}