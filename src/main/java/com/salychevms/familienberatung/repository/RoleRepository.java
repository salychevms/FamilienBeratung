package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    Optional<Role> findByLabel(String label);

    boolean existsByName(String name);

    List<Role> findAll();
}
