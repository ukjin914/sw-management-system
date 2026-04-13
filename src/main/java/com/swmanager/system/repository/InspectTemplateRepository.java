package com.swmanager.system.repository;

import com.swmanager.system.domain.InspectTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectTemplateRepository extends JpaRepository<InspectTemplate, Long> {

    List<InspectTemplate> findByTemplateTypeAndUseYnOrderBySectionAscSortOrderAsc(
            String templateType, String useYn);

    List<InspectTemplate> findByTemplateTypeAndSectionAndUseYnOrderBySortOrderAsc(
            String templateType, String section, String useYn);
}
