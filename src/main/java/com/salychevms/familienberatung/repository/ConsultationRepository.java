package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByFamilyId(Long familyId);
    List<Consultation> findByFamilyIdAndDeletedFalse(Long familyId);
    List<Consultation> findByEmployeeId(Long employeeId);
}