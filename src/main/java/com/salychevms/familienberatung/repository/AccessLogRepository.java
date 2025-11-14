package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    List<AccessLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    List<AccessLog> findByUserLogin(String userLogin);
}