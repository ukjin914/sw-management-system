# SW Management System - ERD (Entity Relationship Diagram)

> 총 41개 테이블 / 7개 도메인 모듈

---

## 도메인 구조 요약

```
+------------------+     +---------------------+     +-------------------+
|   Core Domain    |     | Infrastructure Mgmt |     |  Contract Mgmt    |
|   (11 tables)    |     |    (6 tables)       |     |   (4 tables)      |
+------------------+     +---------------------+     +-------------------+
| users            |     | tb_infra_master     |     | tb_contract       |
| access_logs      |     | tb_infra_server     |     | tb_contract_      |
| ps_info          |     | tb_infra_software   |     |   participant     |
| sw_pjt           |     | tb_infra_link_upis  |     | tb_contract_target|
| sys_mst          |     | tb_infra_link_api   |     | tb_work_plan      |
| sigungu_code     |     | tb_infra_memo       |     +-------------------+
| prj_types        |     +---------------------+
| cont_stat_mst    |
| cont_frm_mst     |     +---------------------+     +-------------------+
| maint_tp_mst     |     |  Document Mgmt      |     | Inspection &      |
+------------------+     |   (6 tables)        |     | Performance       |
                          +---------------------+     |   (3 tables)      |
                          | tb_document         |     +-------------------+
                          | tb_document_detail  |     | tb_inspect_cycle  |
                          | tb_document_history |     | tb_inspect_       |
                          | tb_document_        |     |   checklist       |
                          |   attachment        |     | tb_inspect_issue  |
                          | tb_document_        |     | tb_performance_   |
                          |   signature         |     |   summary         |
                          +---------------------+     +-------------------+

+---------------------+     +-------------------+
|  License Mgmt       |     | Quotation Mgmt    |
|   (4 tables)        |     |   (7 tables)      |
+---------------------+     +-------------------+
| license_registry    |     | qt_quotation      |
| license_upload_     |     | qt_quotation_item |
|   history           |     | qt_quotation_     |
| geonuris_license    |     |   ledger          |
| qr_license          |     | qt_quote_number_  |
+---------------------+     |   seq             |
                             | qt_product_pattern|
                             | qt_remarks_pattern|
                             | qt_wage_rate      |
                             +-------------------+
```

---

## 핵심 관계도 (Relationship Diagram)

```
                            +----------+
                            |  users   |
                            +----+-----+
                                 |
          +----------+-----------+------------+------------+
          |          |           |            |            |
          v          v           v            v            v
    tb_work_plan  tb_contract  tb_document  tb_inspect  tb_performance
    (assignee)   _participant  (author/     _cycle      _summary
    (created_by) (user)        approver)   (assignee)   (user)


                       +------------------+
                       | tb_infra_master  |
                       +--------+---------+
                                |
        +-----------+-----------+-----------+-----------+----------+
        |           |           |           |           |          |
        v           v           v           v           v          v
  tb_infra_    tb_infra_   tb_infra_   tb_infra_  tb_work_   tb_contract
  server       link_upis   link_api    memo       plan       (infra)
    |                                                |
    v                                                v
  tb_infra_                                    tb_document
  software                                     (plan/infra/
    |                                           contract/proj)
    v                                               |
  tb_contract_                    +--------+--------+--------+--------+
  target                          |        |        |        |        |
  (software)                      v        v        v        v        v
                             doc_detail doc_hist doc_attach doc_sign inspect_
                                                                    checklist
                                                                       |
                                                                       v
                                                                  inspect_issue


    +---------------+          +------------------+
    | qt_quotation  |          |     sw_pjt       |
    +-------+-------+          +--------+---------+
            |                           |
            v                           v
    qt_quotation_item            tb_document (proj)
```

---

## 테이블 상세

### 1. Core - users
| Column | Type | Constraint |
|--------|------|------------|
| user_id | BIGINT | PK, AUTO |
| userid | VARCHAR(255) | UNIQUE, NOT NULL |
| username | VARCHAR(255) | |
| password | VARCHAR(255) | |
| orgNm, deptNm, teamNm | VARCHAR(255) | |
| tel, email | VARCHAR(255) | |
| userRole | VARCHAR(255) | |
| position | VARCHAR(50) | |
| techGrade | VARCHAR(20) | |
| auth* (9 columns) | VARCHAR(255) | default='NONE' |
| failedAttempts | INT | default=0 |
| lockTime | DATETIME | |
| regDt | DATETIME | auto |

### 2. Infra - tb_infra_master
| Column | Type | Constraint |
|--------|------|------------|
| infra_id | BIGINT | PK, AUTO |
| infra_type | VARCHAR(255) | |
| city_nm | VARCHAR(255) | |
| dist_nm | VARCHAR(255) | |
| sys_nm / sys_nm_en | VARCHAR(255) | |
| org_cd / dist_cd | VARCHAR(255) | |

### 3. Infra - tb_infra_server
| Column | Type | Constraint |
|--------|------|------------|
| server_id | BIGINT | PK, AUTO |
| infra_id | BIGINT | FK -> tb_infra_master |
| server_type, ip_addr | VARCHAR(255) | |
| acc_id, acc_pw | VARCHAR(255) | |
| os_nm, mac_addr | VARCHAR(255) | |
| server_model | VARCHAR(200) | |
| serial_no | VARCHAR(100) | |
| cpu/memory/disk/network/power_spec | VARCHAR | |

### 4. Infra - tb_infra_software
| Column | Type | Constraint |
|--------|------|------------|
| sw_id | BIGINT | PK, AUTO |
| server_id | BIGINT | FK -> tb_infra_server |
| sw_category, sw_nm, sw_ver | VARCHAR(255) | |
| port, sw_acc_id, sw_acc_pw | VARCHAR(255) | |
| sid, install_path | VARCHAR(255) | |

### 5. Contract - tb_contract
| Column | Type | Constraint |
|--------|------|------------|
| contract_id | INT | PK, AUTO |
| infra_id | BIGINT | FK -> tb_infra_master |
| contract_name | VARCHAR(300) | NOT NULL |
| contract_no | VARCHAR(100) | |
| contract_type | VARCHAR(30) | NOT NULL |
| contract_amount | BIGINT | |
| guarantee_amount/rate | BIGINT/DECIMAL | |
| start_date, end_date | DATE | |
| progress_status | VARCHAR(30) | NOT NULL |
| client_* (org, addr, phone...) | VARCHAR | |

### 6. Contract - tb_contract_participant
| Column | Type | Constraint |
|--------|------|------------|
| participant_id | INT | PK, AUTO |
| contract_id | INT | FK -> tb_contract |
| user_id | BIGINT | FK -> users |
| role_type | VARCHAR(20) | NOT NULL |
| tech_grade | VARCHAR(20) | |
| is_site_rep | BOOLEAN | default=false |

### 7. Contract - tb_contract_target
| Column | Type | Constraint |
|--------|------|------------|
| target_id | INT | PK, AUTO |
| contract_id | INT | FK -> tb_contract |
| sw_id | BIGINT | FK -> tb_infra_software |
| product_name | VARCHAR(200) | NOT NULL |
| quantity | INT | default=1 |

### 8. Work Plan - tb_work_plan
| Column | Type | Constraint |
|--------|------|------------|
| plan_id | INT | PK, AUTO |
| infra_id | BIGINT | FK -> tb_infra_master |
| plan_type | VARCHAR(30) | NOT NULL |
| assignee_id | BIGINT | FK -> users |
| parent_plan_id | INT | FK -> tb_work_plan (self) |
| title | VARCHAR(300) | NOT NULL |
| start_date | DATE | NOT NULL |
| status | VARCHAR(20) | default='PLANNED' |
| created_by | BIGINT | FK -> users |

### 9. Document - tb_document
| Column | Type | Constraint |
|--------|------|------------|
| doc_id | INT | PK, AUTO |
| doc_no | VARCHAR(50) | UNIQUE, NOT NULL |
| doc_type | VARCHAR(30) | NOT NULL |
| infra_id | BIGINT | FK -> tb_infra_master |
| plan_id | INT | FK -> tb_work_plan |
| contract_id | INT | FK -> tb_contract |
| proj_id | BIGINT | FK -> sw_pjt |
| author_id | BIGINT | FK -> users, NOT NULL |
| approver_id | BIGINT | FK -> users |
| status | VARCHAR(20) | default='DRAFT' |

### 10. Quotation - qt_quotation
| Column | Type | Constraint |
|--------|------|------------|
| quote_id | BIGINT | PK, AUTO |
| quote_number | VARCHAR(30) | UNIQUE, NOT NULL |
| quote_date | DATE | NOT NULL |
| category | VARCHAR(10) | NOT NULL |
| project_name | VARCHAR(500) | NOT NULL |
| total_amount | BIGINT | |
| grand_total | BIGINT | |
| bid_rate | DOUBLE | |
| vat_included | BOOLEAN | default=true |

### 11. License - license_registry
| Column | Type | Constraint |
|--------|------|------------|
| id | BIGINT | PK, AUTO |
| license_id | VARCHAR(50) | NOT NULL |
| product_id | VARCHAR(100) | NOT NULL |
| license_string | TEXT | NOT NULL |
| (+ 63 more columns for license details) | | |

---

## FK 관계 요약

| From | To | FK Column | 관계 |
|------|----|-----------|------|
| tb_infra_server | tb_infra_master | infra_id | N:1 |
| tb_infra_software | tb_infra_server | server_id | N:1 |
| tb_infra_link_upis | tb_infra_master | infra_id | N:1 |
| tb_infra_link_api | tb_infra_master | infra_id | N:1 |
| tb_infra_memo | tb_infra_master | infra_id | N:1 |
| tb_work_plan | tb_infra_master | infra_id | N:1 |
| tb_work_plan | users | assignee_id | N:1 |
| tb_work_plan | users | created_by | N:1 |
| tb_work_plan | tb_work_plan | parent_plan_id | N:1 (self) |
| tb_contract | tb_infra_master | infra_id | N:1 |
| tb_contract_participant | tb_contract | contract_id | N:1 |
| tb_contract_participant | users | user_id | N:1 |
| tb_contract_target | tb_contract | contract_id | N:1 |
| tb_contract_target | tb_infra_software | sw_id | N:1 |
| tb_document | tb_infra_master | infra_id | N:1 |
| tb_document | tb_work_plan | plan_id | N:1 |
| tb_document | tb_contract | contract_id | N:1 |
| tb_document | sw_pjt | proj_id | N:1 |
| tb_document | users | author_id | N:1 |
| tb_document | users | approver_id | N:1 |
| tb_document_detail | tb_document | doc_id | N:1 |
| tb_document_history | tb_document | doc_id | N:1 |
| tb_document_history | users | actor_id | N:1 |
| tb_document_attachment | tb_document | doc_id | N:1 |
| tb_document_signature | tb_document | doc_id | N:1 |
| tb_inspect_checklist | tb_document | doc_id | N:1 |
| tb_inspect_issue | tb_document | doc_id | N:1 |
| tb_inspect_cycle | tb_infra_master | infra_id | N:1 |
| tb_inspect_cycle | users | assignee_id | N:1 |
| tb_performance_summary | users | user_id | N:1 |
| qt_quotation_item | qt_quotation | quote_id | N:1 |
| tb_document | sw_pjt | proj_id | N:1 |
