package com.swmanager.system.dto;

import com.swmanager.system.domain.InspectVisitLog;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InspectVisitLogDTO {

    private Long id;
    private Long reportId;
    private String visitYear;
    private String visitMonth;
    private String visitDay;
    private String task;
    private String symptom;
    private String action;
    private Integer sortOrder;

    public static InspectVisitLogDTO fromEntity(InspectVisitLog e) {
        InspectVisitLogDTO dto = new InspectVisitLogDTO();
        dto.setId(e.getId());
        dto.setReportId(e.getReportId());
        dto.setVisitYear(e.getVisitYear());
        dto.setVisitMonth(e.getVisitMonth());
        dto.setVisitDay(e.getVisitDay());
        dto.setTask(e.getTask());
        dto.setSymptom(e.getSymptom());
        dto.setAction(e.getAction());
        dto.setSortOrder(e.getSortOrder());
        return dto;
    }

    public InspectVisitLog toEntity(Long reportId) {
        InspectVisitLog e = new InspectVisitLog();
        e.setId(this.id);
        e.setReportId(reportId);
        e.setVisitYear(this.visitYear);
        e.setVisitMonth(this.visitMonth);
        e.setVisitDay(this.visitDay);
        e.setTask(this.task);
        e.setSymptom(this.symptom);
        e.setAction(this.action);
        e.setSortOrder(this.sortOrder != null ? this.sortOrder : 0);
        return e;
    }
}
