package com.salychevms.familienberatung.repository;

import com.salychevms.familienberatung.model.Consultation;
import com.salychevms.familienberatung.model.Employee;
import com.salychevms.familienberatung.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    Optional<Consultation> findConsultationById(Long id);

    List<Consultation> findConsultationByMember(Member member);

    List<Consultation> findAllByMember(Member member);

    List<Consultation> getConsultationsByEmployee(Employee employee);
}