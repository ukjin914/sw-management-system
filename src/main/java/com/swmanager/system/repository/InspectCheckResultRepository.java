package com.swmanager.system.repository;

import com.swmanager.system.domain.InspectCheckResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface InspectCheckResultRepository extends JpaRepository<InspectCheckResult, Long> {

    List<InspectCheckResult> findByReportIdOrderBySectionAscSortOrderAsc(Long reportId);

    List<InspectCheckResult> findByReportIdAndSection(Long reportId, String section);

    @Modifying
    @Transactional
    @Query("DELETE FROM InspectCheckResult c WHERE c.reportId = :reportId")
    void deleteByReportId(@Param("reportId") Long reportId);
}
