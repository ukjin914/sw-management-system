package com.swmanager.system.repository;

import com.swmanager.system.domain.InspectVisitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface InspectVisitLogRepository extends JpaRepository<InspectVisitLog, Long> {

    List<InspectVisitLog> findByReportIdOrderBySortOrderAsc(Long reportId);

    @Modifying
    @Transactional
    @Query("DELETE FROM InspectVisitLog v WHERE v.reportId = :reportId")
    void deleteByReportId(@Param("reportId") Long reportId);
}
