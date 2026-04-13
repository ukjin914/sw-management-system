package com.swmanager.system.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.swmanager.system.dto.InspectCheckResultDTO;
import com.swmanager.system.dto.InspectReportDTO;
import com.swmanager.system.repository.InfraRepository;
import com.swmanager.system.repository.SwProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

@Slf4j
@Service
public class InspectPdfService {

    @Autowired private TemplateEngine templateEngine;
    @Autowired private InspectReportService inspectReportService;
    @Autowired private SwProjectRepository swProjectRepository;
    @Autowired private InfraRepository infraRepository;

    private File fontFile;

    public String renderToHtml(Long reportId) {
        InspectReportDTO report = inspectReportService.findById(reportId);
        var project = swProjectRepository.findById(report.getPjtId()).orElse(null);

        Context context = new Context();
        context.setVariable("report", report);
        context.setVariable("project", project);

        // 점검결과를 섹션별로 분리하여 컨텍스트에 추가
        if (report.getCheckResults() != null) {
            context.setVariable("dbItems", report.getCheckResults().stream().filter(r -> "DB".equals(r.getSection())).toList());
            context.setVariable("apItems", report.getCheckResults().stream().filter(r -> "AP".equals(r.getSection())).toList());
            context.setVariable("dbmsItems", report.getCheckResults().stream().filter(r -> "DBMS".equals(r.getSection())).toList());
            context.setVariable("gisItems", report.getCheckResults().stream().filter(r -> "GIS".equals(r.getSection())).toList());
            context.setVariable("appItems", report.getCheckResults().stream().filter(r -> "APP".equals(r.getSection())).toList());
            context.setVariable("dbUsage", report.getCheckResults().stream().filter(r -> "DB_USAGE".equals(r.getSection())).toList());
            context.setVariable("apUsage", report.getCheckResults().stream().filter(r -> "AP_USAGE".equals(r.getSection())).toList());
            context.setVariable("dbmsEtc", report.getCheckResults().stream().filter(r -> "DBMS_ETC".equals(r.getSection())).toList());
        }

        if (project != null) {
            var infraList = infraRepository.findByDistNmAndSysNmEn(
                    project.getDistNm(), project.getSysNmEn());
            context.setVariable("infraList", infraList);
        }

        String templateType = report.getSysType() != null ? report.getSysType() : "UPIS";
        List<InspectCheckResultDTO> templateItems = inspectReportService.getTemplateItems(templateType);
        context.setVariable("templateItems", templateItems);

        return templateEngine.process("pdf/pdf-inspect-report", context);
    }

    /** classpath 폰트를 임시파일로 추출 (한 번만) */
    private synchronized File getFontFile() {
        if (fontFile != null && fontFile.exists()) return fontFile;
        try {
            ClassPathResource res = new ClassPathResource("fonts/malgun.ttf");
            if (!res.exists()) {
                // Windows 시스템 폰트 직접 참조
                File sysFontFile = new File("C:/Windows/Fonts/malgun.ttf");
                if (sysFontFile.exists()) { fontFile = sysFontFile; return fontFile; }
                return null;
            }
            File tmp = Files.createTempFile("malgun", ".ttf").toFile();
            tmp.deleteOnExit();
            try (InputStream in = res.getInputStream(); FileOutputStream out = new FileOutputStream(tmp)) {
                in.transferTo(out);
            }
            fontFile = tmp;
            return fontFile;
        } catch (Exception e) {
            log.warn("폰트 파일 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    public byte[] generatePdf(Long reportId) {
        String html = renderToHtml(reportId);
        log.info("PDF HTML 렌더링 완료, reportId={}, html길이={}", reportId, html.length());

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // 한글 폰트 등록 (File 기반)
            File font = getFontFile();
            if (font != null) {
                builder.useFont(font, "Malgun Gothic");
                log.info("한글 폰트 등록 완료: {}", font.getAbsolutePath());
            } else {
                log.warn("한글 폰트를 찾을 수 없습니다. PDF에 한글이 표시되지 않을 수 있습니다.");
            }

            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();

            byte[] result = os.toByteArray();
            log.info("PDF 생성 완료: {} bytes", result.length);
            return result;
        } catch (Exception e) {
            log.error("점검내역서 PDF 변환 실패", e);
            throw new RuntimeException("PDF 변환 중 오류: " + e.getMessage(), e);
        }
    }
}
