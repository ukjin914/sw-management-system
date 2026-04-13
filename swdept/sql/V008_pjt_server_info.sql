-- ============================================================
-- V008: 프로젝트 서버정보 테이블
-- 점검내역서에서 DB서버/AP서버 정보 자동 로드용
-- [DEPRECATED] 더 이상 사용하지 않음 → 테이블 제거
-- ============================================================

DROP TABLE IF EXISTS pjt_server_info CASCADE;

CREATE TABLE IF NOT EXISTS pjt_server_info (
    id BIGSERIAL PRIMARY KEY,
    pjt_id BIGINT NOT NULL REFERENCES sw_pjt(proj_id) ON DELETE CASCADE,
    server_type VARCHAR(10) NOT NULL,       -- 'DB', 'AP'
    server_name VARCHAR(100),               -- 장비명: IBM P720
    model_name VARCHAR(100),                -- 모델명(M/T, S/N)
    cpu VARCHAR(200),
    memory VARCHAR(100),
    disk VARCHAR(300),
    network VARCHAR(200),
    os VARCHAR(100),
    host_name VARCHAR(50),
    purpose VARCHAR(100),                   -- 사용용도: UPIS DB서버
    power_supply VARCHAR(100),
    remarks TEXT,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_server_info_pjt ON pjt_server_info(pjt_id);

COMMENT ON TABLE pjt_server_info IS '프로젝트 서버정보 (점검내역서 연동)';
COMMENT ON COLUMN pjt_server_info.server_type IS 'DB 또는 AP';
COMMENT ON COLUMN pjt_server_info.purpose IS '사용용도 (예: UPIS DB서버, UPIS AP서버)';
