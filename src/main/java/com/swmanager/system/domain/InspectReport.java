package com.swmanager.system.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inspect_report")
public class InspectReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pjt_id", nullable = false)
    private Long pjtId;

    @Column(name = "inspect_month", length = 7)
    private String inspectMonth;

    @Column(name = "sys_type", length = 20)
    private String sysType;

    @Column(name = "doc_title", length = 300)
    private String docTitle;

    @Column(name = "insp_company", length = 100)
    private String inspCompany;

    @Column(name = "insp_name", length = 50)
    private String inspName;

    @Column(name = "conf_org", length = 100)
    private String confOrg;

    @Column(name = "conf_name", length = 50)
    private String confName;

    @Column(name = "insp_dbms", length = 200)
    private String inspDbms;

    @Column(name = "insp_gis", length = 200)
    private String inspGis;

    @Column(name = "dbms_ip", length = 50)
    private String dbmsIp;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "insp_sign", columnDefinition = "TEXT")
    private String inspSign;

    @Column(name = "conf_sign", columnDefinition = "TEXT")
    private String confSign;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "DRAFT";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
