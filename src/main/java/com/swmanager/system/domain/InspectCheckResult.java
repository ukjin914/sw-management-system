package com.swmanager.system.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inspect_check_result")
public class InspectCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "section", nullable = false, length = 20)
    private String section;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "item_name", length = 200)
    private String itemName;

    @Column(name = "item_method", length = 300)
    private String itemMethod;

    @Column(name = "result", length = 500)
    private String result;

    @Column(name = "remarks", length = 300)
    private String remarks;

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
