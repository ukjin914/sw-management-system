package com.swmanager.system.dto;

import com.swmanager.system.domain.InspectCheckResult;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InspectCheckResultDTO {

    private Long id;
    private Long reportId;
    private String section;
    private String category;
    private String itemName;
    private String itemMethod;
    private String result;
    private String remarks;
    private Integer sortOrder;

    public static InspectCheckResultDTO fromEntity(InspectCheckResult e) {
        InspectCheckResultDTO dto = new InspectCheckResultDTO();
        dto.setId(e.getId());
        dto.setReportId(e.getReportId());
        dto.setSection(e.getSection());
        dto.setCategory(e.getCategory());
        dto.setItemName(e.getItemName());
        dto.setItemMethod(e.getItemMethod());
        dto.setResult(e.getResult());
        dto.setRemarks(e.getRemarks());
        dto.setSortOrder(e.getSortOrder());
        return dto;
    }

    public InspectCheckResult toEntity(Long reportId) {
        InspectCheckResult e = new InspectCheckResult();
        e.setId(this.id);
        e.setReportId(reportId);
        e.setSection(this.section);
        e.setCategory(this.category);
        e.setItemName(this.itemName);
        e.setItemMethod(this.itemMethod);
        e.setResult(this.result);
        e.setRemarks(this.remarks);
        e.setSortOrder(this.sortOrder != null ? this.sortOrder : 0);
        return e;
    }
}
