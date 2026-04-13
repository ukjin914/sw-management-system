-- ============================================================
-- V009: 점검내역서 관련 테이블
-- ① inspect_report       : 점검내역서 마스터 (표지 정보)
-- ② inspect_visit_log    : 방문이력 (업무/증상/조치내용)
-- ③ inspect_check_result : 점검결과 항목별 기록
-- ============================================================

-- 1) 점검내역서 마스터 (표지)
CREATE TABLE IF NOT EXISTS inspect_report (
    id              BIGSERIAL PRIMARY KEY,
    pjt_id          BIGINT NOT NULL REFERENCES sw_pjt(proj_id) ON DELETE CASCADE,
    inspect_month   VARCHAR(7),                 -- 점검년월: '2026-01'
    sys_type        VARCHAR(20),                -- 시스템유형: UPIS, KRAS, IPSS, ETC
    doc_title       VARCHAR(300),               -- 문서제목
    insp_company    VARCHAR(100),               -- 점검자 회사
    insp_name       VARCHAR(50),                -- 점검자 담당자
    conf_org        VARCHAR(100),               -- 확인자 기관 (sw_pjt.org_nm)
    conf_name       VARCHAR(50),                -- 확인자 담당자
    insp_dbms       VARCHAR(200),               -- 점검대상 DBMS
    insp_gis        VARCHAR(200),               -- 점검대상 GIS엔진
    dbms_ip         VARCHAR(50),                -- DBMS 서버 IP
    status          VARCHAR(20) DEFAULT 'DRAFT',-- DRAFT, COMPLETED
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inspect_report_pjt ON inspect_report(pjt_id);
CREATE INDEX IF NOT EXISTS idx_inspect_report_month ON inspect_report(inspect_month);

COMMENT ON TABLE inspect_report IS '점검내역서 마스터 (표지 정보)';
COMMENT ON COLUMN inspect_report.inspect_month IS '점검년월 (yyyy-MM)';
COMMENT ON COLUMN inspect_report.conf_org IS '확인자 기관명 (sw_pjt.org_nm에서 로드)';
COMMENT ON COLUMN inspect_report.status IS '상태: DRAFT(임시저장), COMPLETED(완료)';

-- 2) 방문이력
CREATE TABLE IF NOT EXISTS inspect_visit_log (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT NOT NULL REFERENCES inspect_report(id) ON DELETE CASCADE,
    visit_year      VARCHAR(4),                 -- 년도
    visit_month     VARCHAR(2),                 -- 월
    visit_day       VARCHAR(2),                 -- 일
    task            VARCHAR(200),               -- 업무
    symptom         VARCHAR(500),               -- 증상
    action          VARCHAR(500),               -- 조치내용
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_visit_log_report ON inspect_visit_log(report_id);

COMMENT ON TABLE inspect_visit_log IS '점검내역서 방문이력';
COMMENT ON COLUMN inspect_visit_log.task IS '업무';
COMMENT ON COLUMN inspect_visit_log.symptom IS '증상';
COMMENT ON COLUMN inspect_visit_log.action IS '조치내용';

-- 3) 점검결과 항목별 기록
CREATE TABLE IF NOT EXISTS inspect_check_result (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT NOT NULL REFERENCES inspect_report(id) ON DELETE CASCADE,
    section         VARCHAR(20) NOT NULL,       -- 'DB', 'AP', 'DBMS', 'GIS', 'APP'
    category        VARCHAR(50),                -- 구분 (하드웨어, 소프트웨어 등)
    item_name       VARCHAR(200),               -- 점검항목명
    item_method     VARCHAR(300),               -- 점검방법/명령
    result          VARCHAR(500),               -- 점검결과
    remarks         VARCHAR(300),               -- 비고
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_check_result_report ON inspect_check_result(report_id);
CREATE INDEX IF NOT EXISTS idx_check_result_section ON inspect_check_result(report_id, section);

COMMENT ON TABLE inspect_check_result IS '점검내역서 점검결과 항목';
COMMENT ON COLUMN inspect_check_result.section IS '섹션: DB(DB서버), AP(AP서버), DBMS, GIS, APP(표해시스템)';
COMMENT ON COLUMN inspect_check_result.category IS '구분 (하드웨어, 소프트웨어, OS 등)';
COMMENT ON COLUMN inspect_check_result.result IS '점검결과 값';
