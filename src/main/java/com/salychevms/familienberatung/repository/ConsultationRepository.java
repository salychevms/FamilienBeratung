package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Consultation;
import com.salychevms.familienberatung.model.Family;
import org.aspectj.apache.bcel.classfile.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findConsultationById(Long id);

    List<Consultation> findConsultationByFamily(Family family);

    List<Consultation> findAllByFamily(Family family);
}