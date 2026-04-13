package com.swmanager.system.repository;

import com.swmanager.system.domain.Infra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InfraRepository extends JpaRepository<Infra, Long> {

    /** 시군구 + 시스템영문명으로 인프라 조회 (점검내역서용) */
    @Query("SELECT i FROM Infra i WHERE i.distNm = :distNm AND i.sysNmEn = :sysNmEn")
    java.util.List<Infra> findByDistNmAndSysNmEn(@Param("distNm") String distNm, @Param("sysNmEn") String sysNmEn);
    
    // [수정됨] 통합 검색 쿼리
    // 1. infraType(운영/테스트)은 정확히 일치해야 함
    // 2. keyword(검색어)가 있을 경우 시도, 시군구, 시스템명, IP, OS, ID 중 하나라도 포함되면 조회
    @Query("SELECT DISTINCT i FROM Infra i " +
           "LEFT JOIN i.servers s " +
           "WHERE (:type IS NULL OR i.infraType = :type) " +
           "AND (:kw IS NULL OR (" +
           "   i.cityNm LIKE %:kw% OR " +
           "   i.distNm LIKE %:kw% OR " +
           "   i.sysNm LIKE %:kw% OR " +
           "   s.ipAddr LIKE %:kw% OR " +
           "   s.osNm LIKE %:kw% OR " +
           "   s.accId LIKE %:kw% " +
           "))")
    Page<Infra> findAllByKeyword(
            @Param("type") String type,
            @Param("kw") String keyword,
            Pageable pageable);
}