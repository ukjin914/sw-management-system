package com.swmanager.system.repository;

import com.swmanager.system.domain.InspectReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InspectReportRepository extends JpaRepository<InspectReport, Long> {

    List<InspectReport> findByPjtIdOrderByCreatedAtDesc(Long pjtId);

    Optional<InspectReport> findByPjtIdAndInspectMonth(Long pjtId, String inspectMonth);
}
