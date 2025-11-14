package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name="access_log")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp=LocalDateTime.now();
    @Column(nullable = false)
    private String userLogin;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String entityType;
    @Column(nullable = false)
    private Long entityId;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private String ipAddress;
    @Column(nullable = false)
    private String userBrowser;
}