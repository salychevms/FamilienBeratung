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

    public void log(String userLogin,String action, String entityType, Long entityId,
                    String description, String ip, String browser){
        try{
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
        }catch (Exception e){
            log.error("AccessLog for: {} action: {} has been saved failure: {}", userLogin, action, e.getMessage());
        }
    }

    public void logSimple(String userLogin, String action, String entityType, Long entityId){
        try{
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
        }catch (Exception e){
            log.error("Simple AccessLog for: {} has been saved failure: {}", userLogin, e.getMessage());
        }
    }
}