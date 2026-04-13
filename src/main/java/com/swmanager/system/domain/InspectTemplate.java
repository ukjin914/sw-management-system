package com.swmanager.system.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inspect_template")
public class InspectTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_type", nullable = false, length = 20)
    private String templateType;

    @Column(name = "section", nullable = false, length = 20)
    private String section;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_method", length = 300)
    private String itemMethod;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "use_yn", length = 1)
    private String useYn;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.useYn == null) {
            this.useYn = "Y";
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }
}
