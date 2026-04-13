package com.swmanager.system.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inspect_visit_log")
public class InspectVisitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "visit_year", length = 4)
    private String visitYear;

    @Column(name = "visit_month", length = 2)
    private String visitMonth;

    @Column(name = "visit_day", length = 2)
    private String visitDay;

    @Column(name = "task", length = 200)
    private String task;

    @Column(name = "symptom", length = 500)
    private String symptom;

    @Column(name = "action", length = 500)
    private String action;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }
}
