-- Phase 2: DB 테이블 생성
-- 1. 사업별 과업참여자 배정
CREATE TABLE IF NOT EXISTS tb_contract_participant (
    participant_id SERIAL PRIMARY KEY,
    proj_id BIGINT REFERENCES sw_pjt(proj_id),
    user_id BIGINT REFERENCES users(user_id),
    role_type VARCHAR(30),
    tech_grade VARCHAR(30),
    task_desc VARCHAR(500),
    is_site_rep BOOLEAN DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0
);

-- 2. 시스템별 공정명 마스터
CREATE TABLE IF NOT EXISTS tb_process_master (
    process_id SERIAL PRIMARY KEY,
    sys_nm_en VARCHAR(30),
    process_name VARCHAR(200),
    sort_order INTEGER DEFAULT 0,
    use_yn VARCHAR(1) DEFAULT 'Y'
);

-- 3. 시스템별 용역 목적/과업 내용 마스터
CREATE TABLE IF NOT EXISTS tb_service_purpose (
    purpose_id SERIAL PRIMARY KEY,
    sys_nm_en VARCHAR(30),
    purpose_type VARCHAR(20),
    purpose_text TEXT,
    sort_order INTEGER DEFAULT 0,
    use_yn VARCHAR(1) DEFAULT 'Y'
);

-- 기본 공정명 데이터
INSERT INTO tb_process_master (sys_nm_en, process_name, sort_order) VALUES
('UPIS', '도시계획정보체계용 GIS SW 유지관리', 1),
('KRAS', '부동산종합공부시스템용 GIS SW 유지관리', 1),
('IPSS', '지하시설물관리시스템용 GIS SW 유지관리', 1),
('GIS_SW', 'GIS SW 유지관리', 1),
('APIMS', '도로관리시스템용 GIS SW 유지관리', 1)
ON CONFLICT DO NOTHING;

-- 기본 용역 목적 데이터
INSERT INTO tb_service_purpose (sys_nm_en, purpose_type, purpose_text, sort_order) VALUES
('UPIS', 'PURPOSE', '도시계획정보체계(UPIS)의 최신 버전 유지와 원활한 서비스를 제공', 1),
('KRAS', 'PURPOSE', '부동산종합공부시스템(KRAS)의 최신 버전 유지와 원활한 서비스를 제공', 1),
('IPSS', 'PURPOSE', '지하시설물관리시스템(IPSS)의 최신 버전 유지와 원활한 서비스를 제공', 1),
('GIS_SW', 'PURPOSE', 'GIS SW의 최신 버전 유지와 원활한 서비스를 제공', 1),
('APIMS', 'PURPOSE', '도로관리시스템(APIMS)의 최신 버전 유지와 원활한 서비스를 제공', 1)
ON CONFLICT DO NOTHING;

-- users.field_role 컬럼 추가 (분야별: 유지보수책임기술자/유지보수참여기술자)
ALTER TABLE users ADD COLUMN IF NOT EXISTS field_role VARCHAR(50);
UPDATE users SET field_role = '유지보수책임기술자' WHERE username = '박욱진' AND (field_role IS NULL OR field_role = '');
UPDATE users SET field_role = '유지보수참여기술자' WHERE username IN ('김한준','서현규') AND (field_role IS NULL OR field_role = '');

-- users.career_years 컬럼 추가 (경력 연수, 예: "22년")
ALTER TABLE users ADD COLUMN IF NOT EXISTS career_years VARCHAR(20);
UPDATE users SET career_years = '22년' WHERE username = '박욱진' AND (career_years IS NULL OR career_years = '');
UPDATE users SET career_years = '13년' WHERE username = '김한준' AND (career_years IS NULL OR career_years = '');
UPDATE users SET career_years = '8년'  WHERE username = '서현규' AND (career_years IS NULL OR career_years = '');

-- ============================================================
-- 점검내역서 관련 테이블
-- ============================================================

-- 점검내역서 마스터 (표지)
CREATE TABLE IF NOT EXISTS inspect_report (
    id              BIGSERIAL PRIMARY KEY,
    pjt_id          BIGINT NOT NULL REFERENCES sw_pjt(proj_id) ON DELETE CASCADE,
    inspect_month   VARCHAR(7),
    sys_type        VARCHAR(20),
    doc_title       VARCHAR(300),
    insp_company    VARCHAR(100),
    insp_name       VARCHAR(50),
    conf_org        VARCHAR(100),
    conf_name       VARCHAR(50),
    insp_dbms       VARCHAR(200),
    insp_gis        VARCHAR(200),
    dbms_ip         VARCHAR(50),
    status          VARCHAR(20) DEFAULT 'DRAFT',
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inspect_report_pjt ON inspect_report(pjt_id);
CREATE INDEX IF NOT EXISTS idx_inspect_report_month ON inspect_report(inspect_month);

-- 방문이력 (업무/증상/조치내용)
CREATE TABLE IF NOT EXISTS inspect_visit_log (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT NOT NULL REFERENCES inspect_report(id) ON DELETE CASCADE,
    visit_year      VARCHAR(4),
    visit_month     VARCHAR(2),
    visit_day       VARCHAR(2),
    task            VARCHAR(200),
    symptom         VARCHAR(500),
    action          VARCHAR(500),
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_visit_log_report ON inspect_visit_log(report_id);

-- 점검결과 항목별 기록
CREATE TABLE IF NOT EXISTS inspect_check_result (
    id              BIGSERIAL PRIMARY KEY,
    report_id       BIGINT NOT NULL REFERENCES inspect_report(id) ON DELETE CASCADE,
    section         VARCHAR(20) NOT NULL,
    category        VARCHAR(50),
    item_name       VARCHAR(200),
    item_method     VARCHAR(300),
    result          VARCHAR(500),
    remarks         VARCHAR(300),
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_check_result_report ON inspect_check_result(report_id);
CREATE INDEX IF NOT EXISTS idx_check_result_section ON inspect_check_result(report_id, section);

-- 점검 템플릿 (시스템별 점검항목 마스터)
CREATE TABLE IF NOT EXISTS inspect_template (
    id              BIGSERIAL PRIMARY KEY,
    template_type   VARCHAR(20) NOT NULL,
    section         VARCHAR(20) NOT NULL,
    category        VARCHAR(50),
    item_name       VARCHAR(200) NOT NULL,
    item_method     VARCHAR(300),
    sort_order      INT DEFAULT 0,
    use_yn          VARCHAR(1) DEFAULT 'Y',
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_inspect_template_type ON inspect_template(template_type, section);

-- ============================================================
-- 점검 템플릿 초기 데이터 (docx 샘플과 100% 일치)
-- ============================================================

-- 기존 데이터 삭제 후 재삽입
DELETE FROM inspect_template;

-- ============================================================
-- UPIS (HW+SW) DB서버 점검결과 - 점검내역서_UPIS_SW_HW샘플.docx Table 11
-- section='DB', 칼럼: 구분 | 점검 항목 | 수행 명령/방법 | 결과
-- ============================================================
INSERT INTO inspect_template (template_type, section, category, item_name, item_method, sort_order) VALUES
('UPIS', 'DB', '육안점검', 'H/W LED 상태 점검', '육안 확인', 1),
('UPIS', 'DB', '육안점검', '디스크 LED 상태 점검', '육안 확인', 2),
('UPIS', 'DB', '육안점검', 'FAN LED 상태 점검', '육안 확인', 3),
('UPIS', 'DB', '육안점검', '전원 LED 상태 점검', '육안 확인', 4),
('UPIS', 'DB', '육안점검', '케이블 연결상태 점검', '육안 확인', 5),
('UPIS', 'DB', '구성 점검', 'CPU 점검', 'lsdev -Cc processor', 6),
('UPIS', 'DB', '구성 점검', 'MEMORY 점검', 'lsdev -Cc memory', 7),
('UPIS', 'DB', '구성 점검', 'Adapter 점검', 'lsdev -Cc adapter', 8),
('UPIS', 'DB', '구성 점검', 'DISK 점검', 'lsdev -Cc disk, lspv', 9),
('UPIS', 'DB', '구성 점검', 'Network 점검', 'netstat -a', 10),
('UPIS', 'DB', '성능 점검', 'CPU 사용률', 'topas, nmon', 11),
('UPIS', 'DB', '성능 점검', 'MEMORY 사용률', 'topas, nmon', 12),
('UPIS', 'DB', '성능 점검', 'SWAP 사용률', 'lsps -a', 13),
('UPIS', 'DB', '성능 점검', 'I/O 사용률', 'iostat 1 10', 14),
('UPIS', 'DB', '성능 점검', '네트워크 사용률', 'topas, netstat -ni', 15),
('UPIS', 'DB', 'DATA 점검', '디스크 사용량', 'df -gH', 16),
('UPIS', 'DB', 'DATA 점검', 'I-node 사용량', 'df -h', 17),
('UPIS', 'DB', 'DATA 점검', '미러 상태', 'lsvg -l rootvg', 18),
('UPIS', 'DB', '네트워크', 'Link 상태', 'netstat -na', 19),
('UPIS', 'DB', '네트워크', 'Ping 상태', 'ping gateway', 20),
('UPIS', 'DB', '네트워크', 'Collisions', 'netstat -ni', 21),
('UPIS', 'DB', '프로세서', '각 프로세서 상태', 'topas, nmon', 22),
('UPIS', 'DB', '로그', '시스템 로그', 'errpt', 23),
('UPIS', 'DB', '로그', '접속 로그', 'who, lastlog', 24)
ON CONFLICT DO NOTHING;

-- ============================================================
-- UPIS (HW+SW) AP서버 점검결과 - 점검내역서_UPIS_SW_HW샘플.docx Table 14
-- section='AP', 칼럼: 종류 | 점검 항목 | 점검 내용 | 점검 기준 | 결과
-- item_method 형식: "점검 내용||점검 기준"
-- ============================================================
INSERT INTO inspect_template (template_type, section, category, item_name, item_method, sort_order) VALUES
('UPIS', 'AP', 'H/W', '시스템 LED', 'Front panel LED 육안 확인||적색등 유무', 1),
('UPIS', 'AP', 'H/W', 'Power Supply', 'Power Supply 육안 확인||적색등 유무', 2),
('UPIS', 'AP', 'H/W', 'Disk', 'LED 육안 확인||적색등 유무', 3),
('UPIS', 'AP', 'H/W', 'CPU', 'CPU 상태 확인||정상용량 확인', 4),
('UPIS', 'AP', 'H/W', 'Memory', 'Memory 상태 확인||정상용량 확인', 5),
('UPIS', 'AP', 'H/W', 'Adapter', 'LED 및 Cable 연결상태 확인||적색등 유무', 6),
('UPIS', 'AP', 'OS', '로그 점검', 'eventlog||error 유무', 7),
('UPIS', 'AP', 'OS', 'Security log', 'Security log 확인||error 유무', 8),
('UPIS', 'AP', 'OS', 'Disk 여유공간', '내컴퓨터||디스크 용량 확인', 9),
('UPIS', 'AP', 'OS', 'Network 점검', 'netstat -r, netstat -e||라우팅/네트워크 에러', 10),
('UPIS', 'AP', 'OS', 'IP 정보', 'ipconfig /all||IP/링크 상태 확인', 11),
('UPIS', 'AP', '보안', '사용자 계정', '사용자 계정 확인||특이계정 유무', 12),
('UPIS', 'AP', '성능', 'CPU', '관리도구-성능 (processor%)||CPU 사용량', 13),
('UPIS', 'AP', '성능', 'Memory', '관리도구-성능 (Memory%)||Memory 사용량', 14)
ON CONFLICT DO NOTHING;

-- ============================================================
-- UPIS (HW+SW) DBMS (Oracle) 점검결과 - 점검내역서_UPIS_SW_HW샘플.docx Table 17
-- section='DBMS', 칼럼: 종류 | 점검 항목 | 수행 명령/쿼리 | 결과
-- ============================================================
INSERT INTO inspect_template (template_type, section, category, item_name, item_method, sort_order) VALUES
('UPIS', 'DBMS', '오라클', '호스트네임', '#hostname', 1),
('UPIS', 'DBMS', '오라클', 'O/S 정보', '#oslevel -s', 2),
('UPIS', 'DBMS', '오라클', 'DB 버전', '#sqlplus', 3),
('UPIS', 'DBMS', '오라클', 'SID 확인', '#echo $ORACLE_SID', 4),
('UPIS', 'DBMS', '오라클', '오라클 로그', '#vi alert_ort.log', 5),
('UPIS', 'DBMS', '오라클', 'Archive Mode', '>archive log list;', 6),
('UPIS', 'DBMS', '오라클', '리두 로그', '>select * from v$logfile;', 7),
('UPIS', 'DBMS', '오라클', '컨트롤 파일', '>select * from v$controlfile;', 8),
('UPIS', 'DBMS', '오라클', 'SGA', '>show sga;', 9),
('UPIS', 'DBMS', '오라클', 'Tablespace 상태', '>select status from dba_tablespaces;', 10),
('UPIS', 'DBMS', '오라클', 'Tablespace 용량', '>select a.tablespace_name ...', 11),
('UPIS', 'DBMS', '오라클', 'Datafile 상태', '>select d.status, v.status ...', 12),
('UPIS', 'DBMS', '오라클', 'Datafile 용량', '>select sum from dba_data_files;', 13),
('UPIS', 'DBMS', '오라클', 'Export 백업', '#crontab -l, ls', 14),
('UPIS', 'DBMS', '오라클', 'Home size', '#df -gP', 15),
('UPIS', 'DBMS', '오라클', 'Oradata Size', '#df -gP', 16),
('UPIS', 'DBMS', '오라클', 'Backup Size', '#df -gP', 17)
ON CONFLICT DO NOTHING;

-- ============================================================
-- UPIS GIS엔진 점검결과 (UPIS/UPIS_SW 공용) - 6항목
-- 칼럼: 대상 | 점검 항목 | 점검 내용 및 방법 | 결과
-- ============================================================
INSERT INTO inspect_template (template_type, section, category, item_name, item_method, sort_order) VALUES
('UPIS', 'GIS', 'GeoNURIS Spatial Server (GSS)', 'GSS 구동확인', 'ps -ef | grep GSS 실행 확인', 1),
('UPIS', 'GIS', 'GeoNURIS Spatial Server (GSS)', 'GSS 로그파일 삭제', '/GeoNURIS_Spatial_Server/log 경로의 로그 중 1달 전 파일 삭제', 2),
('UPIS', 'GIS', 'GeoNURIS Spatial Server (GSS)', 'Desktop Pro 데이터저장소 구동확인', 'Desktop Pro 실행 후 데이터저장소에서 GSS 데이터 불러오기', 3),
('UPIS', 'GIS', 'GeoNURIS GeoWeb Server (GWS)', 'GWS 구동확인', '윈도우 서비스 "GeoNURIS GeoWeb Server 64bit" 구동 상태 확인', 4),
('UPIS', 'GIS', 'GeoNURIS GeoWeb Server (GWS)', 'GWS 로그파일 삭제', 'C:\\Program Files\\GeoNURIS_GeoWeb_Server_64\\webapps\\uwes\\store (DEM/SLOP 제외)', 5),
('UPIS', 'GIS', 'GeoNURIS GeoWeb Server (GWS)', 'GWS 서비스 확인', 'http://웹서버IP:8880/uwes 관리자 접속 → WMS → Preview → 지도 표출', 6),
('UPIS_SW', 'GIS', 'GeoNURIS Spatial Server (GSS)', 'GSS 구동확인', 'ps -ef | grep GSS 실행 확인', 1),
('UPIS_SW', 'GIS', 'GeoNURIS Spatial Server (GSS)', 'GSS 로그파일 삭제', '/GeoNURIS_Spatial_Server/log 경로의 로그 중 1달 전 파일 삭제', 2),
('UPIS_SW', 'GIS', 'GeoNURIS Spatial Server (GSS)', 'Desktop Pro 데이터저장소 구동확인', 'Desktop Pro 실행 후 데이터저장소에서 GSS 데이터 불러오기', 3),
('UPIS_SW', 'GIS', 'GeoNURIS GeoWeb Server (GWS)', 'GWS 구동확인', '윈도우 서비스 "GeoNURIS GeoWeb Server 64bit" 구동 상태 확인', 4),
('UPIS_SW', 'GIS', 'GeoNURIS GeoWeb Server (GWS)', 'GWS 로그파일 삭제', 'C:\\Program Files\\GeoNURIS_GeoWeb_Server_64\\webapps\\uwes\\store (DEM/SLOP 제외)', 5),
('UPIS_SW', 'GIS', 'GeoNURIS GeoWeb Server (GWS)', 'GWS 서비스 확인', 'http://웹서버IP:8880/uwes 관리자 접속 → WMS → Preview → 지도 표출', 6)
ON CONFLICT DO NOTHING;

-- ============================================================
-- UPIS 표준시스템 점검결과 (UPIS/UPIS_SW 공용) - 14항목
-- 칼럼: 대분류 | 중분류 | 점검 내용 | 결과
-- ============================================================
INSERT INTO inspect_template (template_type, section, category, item_name, item_method, sort_order) VALUES
('UPIS', 'APP', '도시계획', '조회/검색', '필지/고시/조서/이력/재해취약성 상세정보 조회·검색', 1),
('UPIS', 'APP', '도시계획', 'KRAS 연계', '부동산토지공부 토지정보(지목/소유구분/면적) 표출 여부', 2),
('UPIS', 'APP', '도시계획', 'KRAS 연계', '토지이용계획확인서 표출 (해당 필지 확인)', 3),
('UPIS', 'APP', '도시계획', 'KRAS 연계', '건축물대장 표출 여부', 4),
('UPIS', 'APP', '통계조회', '시스템통계', '도시계획 통계 데이터 표출 여부', 5),
('UPIS', 'APP', '전자심의', '메뉴활성화', '전자심의 메뉴 존재 확인', 6),
('UPIS', 'APP', '지구단위계획', '조회/검색', '계획정보/규제정보/정보관리 상세정보 조회·검색', 7),
('UPIS', 'APP', '비정형자료실', '메뉴활성화', '비정형자료실 메뉴 존재 확인', 8),
('UPIS', 'APP', '관리자', '사용자관리', '사용자승인/사용자관리/메뉴권한관리 정상 여부', 9),
('UPIS', 'APP', 'GIS엔진', '지도 요청', '현황도/주제도/도시계획시설 등 표출 여부', 10),
('UPIS', 'APP', 'GIS엔진', '필지 이동', '주소 검색 시 필지 이동', 11),
('UPIS', 'APP', 'GIS엔진', '하일라이팅', '검색 필지 하일라이팅', 12),
('UPIS', 'APP', 'GIS엔진', '필지정보', '시설정보/용도지역 등 정보 표출', 13),
('UPIS', 'APP', 'GIS엔진', '이력정보', '시설정보/용도지역 등 이력 표출', 14),
('UPIS_SW', 'APP', '도시계획', '조회/검색', '필지/고시/조서/이력/재해취약성 상세정보 조회·검색', 1),
('UPIS_SW', 'APP', '도시계획', 'KRAS 연계', '부동산토지공부 토지정보(지목/소유구분/면적) 표출 여부', 2),
('UPIS_SW', 'APP', '도시계획', 'KRAS 연계', '토지이용계획확인서 표출 (해당 필지 확인)', 3),
('UPIS_SW', 'APP', '도시계획', 'KRAS 연계', '건축물대장 표출 여부', 4),
('UPIS_SW', 'APP', '통계조회', '시스템통계', '도시계획 통계 데이터 표출 여부', 5),
('UPIS_SW', 'APP', '전자심의', '메뉴활성화', '전자심의 메뉴 존재 확인', 6),
('UPIS_SW', 'APP', '지구단위계획', '조회/검색', '계획정보/규제정보/정보관리 상세정보 조회·검색', 7),
('UPIS_SW', 'APP', '비정형자료실', '메뉴활성화', '비정형자료실 메뉴 존재 확인', 8),
('UPIS_SW', 'APP', '관리자', '사용자관리', '사용자승인/사용자관리/메뉴권한관리 정상 여부', 9),
('UPIS_SW', 'APP', 'GIS엔진', '지도 요청', '현황도/주제도/도시계획시설 등 표출 여부', 10),
('UPIS_SW', 'APP', 'GIS엔진', '필지 이동', '주소 검색 시 필지 이동', 11),
('UPIS_SW', 'APP', 'GIS엔진', '하일라이팅', '검색 필지 하일라이팅', 12),
('UPIS_SW', 'APP', 'GIS엔진', '필지정보', '시설정보/용도지역 등 정보 표출', 13),
('UPIS_SW', 'APP', 'GIS엔진', '이력정보', '시설정보/용도지역 등 이력 표출', 14)
ON CONFLICT DO NOTHING;

-- ============================================================
-- KRAS GIS엔진 점검결과 - 점검내역서_KRAS샘플.docx Table 8
-- 칼럼: 대상 | 점검 내용 및 방법 | 결과 (3칼럼, 점검 항목 없음)
-- item_name에 점검 내용, item_method는 빈값 (프론트에서 대상+내용만 표시)
-- ============================================================
INSERT INTO inspect_template (template_type, section, category, item_name, item_method, sort_order) VALUES
('KRAS', 'GIS', 'GeoNURIS Spatial Server(GSS)', 'GSS 구동확인', 'ps –ef | grep –i geo 실행 확인', 1),
('KRAS', 'GIS', 'GeoNURIS Spatial Server(GSS)', 'GSS 상태확인', 'GSS -I aliveness | GSS -I connections', 2),
('KRAS', 'GIS', 'GeoNURIS Spatial Server(GSS)', 'GSS 로그확인', '/kras_home/geonuris/GeoNURIS_Spatail_Server3.6/logs 최신 로그일자 파일 확인 tail –f catalina.out', 3),
('KRAS', 'GIS', 'GeoNURIS GeoWeb Server(GWS)', 'GWS로그확인', '/kras_home/app/MapStudio/log 최신 로그일자 파일 확인', 4),
('KRAS', 'GIS', 'GeoNURIS GeoWeb Server(GWS)', 'GWS로그확인', '/kras_home/app/MapStudio/log 최신 로그일자 파일 확인', 5),
('KRAS', 'GIS', 'GeoNURIS GeoWeb Server(GWS)', 'GWS서비스 확인', 'http://웹서버 IP:9080/msp 로 관리자페이지 접속 | 로그인 > Spatial > 공간데이터 정상표출 확인 | wms, wfs, wfs transaction 정상표출 확인', 6),
('KRAS', 'GIS', '측량성과 프로그램', '부동산종합공부시스템 실행', 'C/S 실행 확인', 7),
('KRAS', 'GIS', 'GeoNURIS Desktop Pro', 'Desktop Pro 구동확인', '바탕화면의 Desktop Pro 실행', 8),
('KRAS', 'GIS', 'GeoNURIS Desktop Pro', 'Map Display 확인', '데이터저장소를 통해 데이터 목록 갱신 | 공간 데이터 표출 확인', 9)
ON CONFLICT DO NOTHING;

-- pjt_server_info 테이블 제거 (더 이상 사용하지 않음)
DROP TABLE IF EXISTS pjt_server_info CASCADE;
