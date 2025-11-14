package com.salychevms.familienberatung.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;
    @Column(nullable = false, length = 50)
    private String lastName;
    @Column
    private String email;
    @Column(length = 30)
    private String mobileNumber;
    @Column(length = 30)
    private String landNumber;
    @Column(nullable = false, unique = true, length = 50)
    private String login;
    @Column(nullable = false, length = 200)
    private String password;
    @Column(nullable = false)
    private boolean passwordChangeRequired = false;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private boolean deleted = false;
    @Column
    private LocalDate deletedAt;
    @Column
    private String notes;
    @Column(nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();
    @Column(length = 50)
    private String createdBy;
    private LocalDateTime updatedAt;
    @Column(length = 50)
    private String updatedBy;

    public Employee() {
    }
}