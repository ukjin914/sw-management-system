package com.swmanager.system.controller;

import com.swmanager.system.config.CustomUserDetails;
import com.swmanager.system.domain.Infra;
import com.swmanager.system.domain.User;
import com.swmanager.system.domain.workplan.Document;
import com.swmanager.system.domain.workplan.DocumentDetail;
import com.swmanager.system.domain.workplan.DocumentHistory;
import com.swmanager.system.dto.DocumentDTO;
import com.swmanager.system.domain.PersonInfo;
import com.swmanager.system.repository.InfraRepository;
import com.swmanager.system.repository.PersonInfoRepository;
import com.swmanager.system.repository.SwProjectRepository;
import com.swmanager.system.repository.UserRepository;
import com.swmanager.system.service.DocumentService;
import com.swmanager.system.service.DocumentAttachmentService;
import com.swmanager.system.service.PdfExportService;
import com.swmanager.system.service.HwpxExportService;
import com.swmanager.system.service.ExcelExportService;
import com.swmanager.system.service.LogService;
import com.swmanager.system.service.InspectReportService;
import com.swmanager.system.service.InspectPdfService;
import com.swmanager.system.dto.InspectReportDTO;
import com.swmanager.system.dto.InspectCheckResultDTO;
import com.swmanager.system.domain.workplan.ProcessMaster;
import com.swmanager.system.domain.workplan.ServicePurpose;
import com.swmanager.system.domain.workplan.ContractParticipant;
import com.swmanager.system.repository.workplan.ProcessMasterRepository;
import com.swmanager.system.repository.workplan.ServicePurposeRepository;
import com.swmanager.system.repository.workplan.ContractParticipantRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Slf4j
@Controller
@RequestMapping("/document")
public class DocumentController {

    @Autowired private DocumentService documentService;
    @Autowired private InfraRepository infraRepository;
    @Autowired private PersonInfoRepository personInfoRepository;
    @Autowired private SwProjectRepository swProjectRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private LogService logService;
    @Autowired private ProcessMasterRepository processMasterRepository;
    @Autowired private ServicePurposeRepository servicePurposeRepository;
    @Autowired private ContractParticipantRepository contractParticipantRepository;
    @Autowired private com.swmanager.system.repository.PjtTargetRepository pjtTargetRepository;
    @Autowired private com.swmanager.system.repository.PjtManpowerPlanRepository pjtManpowerPlanRepository;
    @Autowired private com.swmanager.system.repository.PjtScheduleRepository pjtScheduleRepository;
    @Autowired private InspectReportService inspectReportService;
    @Autowired private InspectPdfService inspectPdfService;

    // === 권한 ===

    private CustomUserDetails getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return null;
            Object principal = auth.getPrincipal();
            if (principal instanceof CustomUserDetails) return (CustomUserDetails) principal;
            return null;
        } catch (Exception e) { return null; }
    }

    private boolean isAdmin() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null && auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } catch (Exception e) { return false; }
    }

    private String getAuth() {
        if (isAdmin()) return "EDIT";
        CustomUserDetails cu = getCurrentUser();
        if (cu == null) return "NONE";
        String auth = cu.getUser().getAuthDocument();
        return (auth != null) ? auth : "NONE";
    }

    // === D-01: 문서 목록 ===

    @GetMapping("/list")
    public String documentList(@RequestParam(name = "docType", required = false) String docType,
                                @RequestParam(name = "status", required = false) String status,
                                @RequestParam(name = "cityNm", required = false) String cityNm,
                                @RequestParam(name = "distNm", required = false) String distNm,
                                @RequestParam(name = "keyword", required = false) String keyword,
                                @PageableDefault(size = 15) Pageable pageable,
                                Model model, RedirectAttributes rttr) {
        String auth = getAuth();
        if ("NONE".equals(auth)) {
            rttr.addFlashAttribute("errorMessage", "접근 권한이 없습니다.");
            return "redirect:/";
        }

        Long authorId = null; // 관리자는 전체 조회

        Page<DocumentDTO> documents = documentService.searchDocuments(
                docType, status, cityNm, distNm, authorId, null, null, keyword, pageable);

        // 시도 목록 (드롭다운용), 시군구 목록 (선택된 시도 기준)
        List<String> cityList = documentService.getCityNames();
        List<String> distList = (cityNm != null && !cityNm.isBlank())
                ? documentService.getDistNamesByCity(cityNm)
                : List.of();

        model.addAttribute("documents", documents);
        model.addAttribute("cityList", cityList);
        model.addAttribute("distList", distList);
        model.addAttribute("docType", docType);
        model.addAttribute("status", status);
        model.addAttribute("cityNm", cityNm);
        model.addAttribute("distNm", distNm);
        model.addAttribute("keyword", keyword);
        model.addAttribute("userAuth", auth);

        logService.log("문서관리", "조회", "문서 목록 조회");
        return "document/document-list";
    }

    /** GET /document/api/dist-list?cityNm={시도} - 시군구 카스케이드 로딩용 */
    @GetMapping("/api/dist-list")
    @ResponseBody
    public List<String> getDistList(@RequestParam String cityNm) {
        return documentService.getDistNamesByCity(cityNm);
    }

    // === D-10: 문서 상세/이력 ===

    @GetMapping("/detail/{id}")
    public String documentDetail(@PathVariable Integer id, Model model, RedirectAttributes rttr) {
        String auth = getAuth();
        if ("NONE".equals(auth)) {
            rttr.addFlashAttribute("errorMessage", "접근 권한이 없습니다.");
            return "redirect:/";
        }

        Document doc = documentService.getDocumentById(id);
        DocumentDTO dto = DocumentDTO.fromEntity(doc);
        List<DocumentHistory> histories = documentService.getDocumentHistory(id);

        model.addAttribute("doc", dto);
        model.addAttribute("document", doc);
        model.addAttribute("histories", histories);
        model.addAttribute("userAuth", auth);
        model.addAttribute("users", userRepository.findByEnabledTrue());

        logService.log("문서관리", "조회", "문서 상세 조회 (ID: " + id + ")");
        return "document/document-detail";
    }

    // === 문서 생성 (공통) ===

    @GetMapping("/create")
    public String createForm(@RequestParam(required = false, defaultValue = "") String docType,
                              @RequestParam(required = false) Long infraId,
                              @RequestParam(required = false) Long projId,
                              @RequestParam(required = false) Integer docId,
                              @RequestParam(required = false) Long reportId,
                              Model model, RedirectAttributes rttr) {
        if (!"EDIT".equals(getAuth())) {
            rttr.addFlashAttribute("errorMessage", "작성 권한이 없습니다.");
            return "redirect:/document/list";
        }
        if (docType == null || docType.isBlank()) {
            rttr.addFlashAttribute("errorMessage", "문서 유형을 선택해주세요.");
            return "redirect:/document/list";
        }

        model.addAttribute("docType", docType);
        model.addAttribute("infraId", infraId);
        model.addAttribute("projId", projId);
        model.addAttribute("reportId", reportId);
        model.addAttribute("infraList", infraRepository.findAll(Sort.by("cityNm", "distNm")));
        model.addAttribute("projects", swProjectRepository.findAll(Sort.by(Sort.Direction.DESC, "year", "projId")));
        model.addAttribute("users", userRepository.findByEnabledTrue());

        // === 수정 모드: 기존 문서 데이터 로드 ===
        if (docId != null) {
            try {
                Document existingDoc = documentService.getDocumentById(docId);
                Map<String, Object> existingData = new HashMap<>();
                existingData.put("docId", existingDoc.getDocId());
                existingData.put("docNo", existingDoc.getDocNo());
                existingData.put("title", existingDoc.getTitle());
                existingData.put("sysType", existingDoc.getSysType());
                existingData.put("projId", existingDoc.getProject() != null ? existingDoc.getProject().getProjId() : null);

                Map<String, Map<String, Object>> sectionsMap = new HashMap<>();
                if (existingDoc.getDetails() != null) {
                    for (DocumentDetail d : existingDoc.getDetails()) {
                        sectionsMap.put(d.getSectionKey(), d.getSectionData());
                    }
                }
                existingData.put("sections", sectionsMap);

                // 프로젝트 정보 (시도/시군구/시스템 드롭다운 사전 세팅용)
                if (existingDoc.getProject() != null) {
                    var p = existingDoc.getProject();
                    existingData.put("projYear", p.getYear());
                    existingData.put("projCityNm", p.getCityNm());
                    existingData.put("projDistNm", p.getDistNm());
                    existingData.put("projSysNmEn", p.getSysNmEn());
                    existingData.put("projNm", p.getProjNm());
                }

                model.addAttribute("existingDoc", existingData);
                model.addAttribute("existingDocId", docId);
            } catch (Exception e) {
                log.error("기존 문서 로드 실패 (docId={})", docId, e);
            }
        }

        // 문서유형별 템플릿 분기
        String template = switch (docType) {
            case "COMMENCE" -> "document/doc-commence";
            case "INTERIM" -> "document/doc-interim";
            case "COMPLETION" -> "document/doc-completion";
            case "INSPECT" -> "document/doc-inspect";
            case "FAULT" -> "document/doc-fault";
            case "SUPPORT" -> "document/doc-support";
            case "INSTALL" -> "document/doc-install";
            case "PATCH" -> "document/doc-patch";
            default -> "document/document-list";
        };
        return template;
    }

    // === 문서 저장 (공통 API) ===

    @ResponseBody
    @PostMapping("/api/save")
    public ResponseEntity<Map<String, Object>> saveDocument(@RequestBody Map<String, Object> requestData) {
        if (!"EDIT".equals(getAuth())) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        try {
            CustomUserDetails cu = getCurrentUser();
            User author = cu != null ? cu.getUser() : null;

            String docType = (String) requestData.get("docType");
            String sysType = (String) requestData.get("sysType");
            Long infraId = requestData.get("infraId") != null ? Long.valueOf(requestData.get("infraId").toString()) : null;
            Integer contractId = requestData.get("contractId") != null ? Integer.valueOf(requestData.get("contractId").toString()) : null;
            Integer planId = requestData.get("planId") != null ? Integer.valueOf(requestData.get("planId").toString()) : null;
            Long projId = requestData.get("projId") != null ? Long.valueOf(requestData.get("projId").toString()) : null;
            String title = (String) requestData.get("title");
            Integer docId = requestData.get("docId") != null ? Integer.valueOf(requestData.get("docId").toString()) : null;

            String docNo = (String) requestData.get("docNo"); // 수동입력 문서번호

            Document doc;
            if (docId != null) {
                doc = documentService.getDocumentById(docId);
                doc.setTitle(title);
                doc.setSysType(sysType);
            } else {
                doc = documentService.createDocument(docType, sysType, infraId, contractId, planId, title, author);
            }
            // 문서번호 수동 저장 (빈칸 허용)
            if (docNo != null) {
                doc.setDocNo(docNo.trim().isEmpty() ? null : docNo.trim());
            }

            // sw_pjt 연결
            if (projId != null) {
                doc.setProject(swProjectRepository.findById(projId).orElse(null));
            }

            // 섹션 데이터 저장
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sections = (List<Map<String, Object>>) requestData.get("sections");
            if (sections != null) {
                for (int i = 0; i < sections.size(); i++) {
                    Map<String, Object> sec = sections.get(i);
                    String sectionKey = (String) sec.get("sectionKey");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sectionData = (Map<String, Object>) sec.get("sectionData");
                    documentService.saveSection(doc.getDocId(), sectionKey, sectionData, i);
                }
            }

            logService.log("문서관리", docId != null ? "수정" : "등록",
                    DocumentDTO.getDocTypeLabel(docType) + " " + (docId != null ? "수정" : "등록") + " (ID: " + doc.getDocId() + ")");

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("docId", doc.getDocId());
            result.put("docNo", doc.getDocNo() != null ? doc.getDocNo() : "");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("문서 저장 실패", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // === 문서 상태 변경 (DRAFT ↔ COMPLETED) ===

    @ResponseBody
    @PostMapping("/api/status/{id}")
    public ResponseEntity<Map<String, Object>> changeStatus(@PathVariable Integer id,
                                                             @RequestParam String status,
                                                             @RequestParam(required = false) String comment) {
        if (!"EDIT".equals(getAuth())) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        // DRAFT, COMPLETED 만 허용
        if (!"DRAFT".equals(status) && !"COMPLETED".equals(status)) {
            return ResponseEntity.badRequest().body(Map.of("error", "허용되지 않는 상태입니다."));
        }

        CustomUserDetails cu = getCurrentUser();
        User actor = cu != null ? cu.getUser() : null;

        Document doc = documentService.changeStatus(id, status, actor, comment);
        logService.log("문서관리", "상태변경", "문서 상태변경 (ID: " + id + " → " + status + ")");

        return ResponseEntity.ok(Map.of("success", true, "status", doc.getStatus()));
    }

    // === 문서 삭제 ===

    @PostMapping("/delete/{id}")
    public String deleteDocument(@PathVariable Integer id, RedirectAttributes rttr) {
        if (!"EDIT".equals(getAuth())) {
            rttr.addFlashAttribute("errorMessage", "삭제 권한이 없습니다.");
            return "redirect:/document/list";
        }

        documentService.deleteDocument(id);
        logService.log("문서관리", "삭제", "문서 삭제 (ID: " + id + ")");
        rttr.addFlashAttribute("successMessage", "문서가 삭제되었습니다.");
        return "redirect:/document/list";
    }

    // === D-11: PDF 미리보기/출력 ===

    @Autowired private PdfExportService pdfExportService;
    @Autowired private HwpxExportService hwpxExportService;
    @Autowired private ExcelExportService excelExportService;

    @GetMapping("/preview/{id}")
    public String previewDocument(@PathVariable Integer id, Model model) {
        String html = pdfExportService.renderDocumentToHtml(id);
        model.addAttribute("htmlContent", html);
        model.addAttribute("docId", id);
        return "document/document-preview";
    }

    @ResponseBody
    @GetMapping("/api/pdf/{id}")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Integer id) {
        try {
            byte[] pdfBytes = pdfExportService.generatePdf(id);

            Document doc = documentService.getDocumentById(id);
            String filename = (doc.getDocNo() != null ? doc.getDocNo() : "document") + ".pdf";
            // UTF-8 파일명 인코딩
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("PDF 생성 실패", e);
            return ResponseEntity.status(500).build();
        }
    }

    // === HWPX 한글문서 다운로드 ===

    @ResponseBody
    @GetMapping("/api/hwpx/{id}")
    public ResponseEntity<byte[]> downloadHwpx(@PathVariable Integer id,
                                                @RequestParam(defaultValue = "letter") String type) {
        try {
            byte[] hwpxBytes = hwpxExportService.generateHwpx(id, type);

            Document doc = documentService.getDocumentById(id);
            String projNm = (doc.getProject() != null && doc.getProject().getProjNm() != null)
                    ? doc.getProject().getProjNm() : "문서";
            String docTypeLabel = switch (doc.getDocType()) {
                case "COMMENCE" -> "착수계";
                case "INTERIM" -> "기성계";
                case "COMPLETION" -> "준공계";
                default -> doc.getDocType();
            };
            String typeLabel = switch (type) {
                case "letter" -> "_공문";
                case "inspector" -> "";
                case "commence_body" -> "";
                case "completion_body" -> "_KRAS";
                case "completion_body_upis" -> "_UPIS";
                case "completion_full" -> "";
                default -> "_" + type;
            };
            String filename = projNm + "_" + docTypeLabel + typeLabel + ".hwpx";
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");

            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.hancom.hwpx")
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(hwpxBytes);
        } catch (Exception e) {
            log.error("HWPX 생성 실패", e);
            return ResponseEntity.status(500).build();
        }
    }

    // === 설계내역서 Excel 다운로드 ===

    @ResponseBody
    @GetMapping("/api/excel/{id}")
    public ResponseEntity<byte[]> downloadExcel(@PathVariable Integer id) {
        try {
            Document doc = documentService.getDocumentById(id);
            byte[] excelBytes;
            String suffix;
            if ("INTERIM".equals(doc.getDocType())) {
                excelBytes = excelExportService.generateInterimReport(id);
                suffix = "_기성내역서.xlsx";
            } else {
                excelBytes = excelExportService.generateDesignEstimate(id);
                suffix = "_설계내역서.xlsx";
            }

            String projNm = (doc.getProject() != null && doc.getProject().getProjNm() != null)
                    ? doc.getProject().getProjNm() : "문서";
            String filename = projNm + suffix;
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");

            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("설계내역서 Excel 생성 실패", e);
            return ResponseEntity.status(500).build();
        }
    }

    // === 일괄 다운로드 (ZIP): HWP + Excel 전체 ===
    @ResponseBody
    @GetMapping("/api/zip/{id}")
    public ResponseEntity<byte[]> downloadZip(@PathVariable Integer id) {
        try {
            Document doc = documentService.getDocumentById(id);
            String docType = doc.getDocType();
            String projNm = (doc.getProject() != null && doc.getProject().getProjNm() != null)
                    ? doc.getProject().getProjNm() : "문서";
            String docTypeLabel = switch (docType) {
                case "COMMENCE" -> "착수계";
                case "INTERIM" -> "기성계";
                case "COMPLETION" -> "준공계";
                default -> docType;
            };

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
                // 공문
                if ("COMMENCE".equals(docType) || "INTERIM".equals(docType) || "COMPLETION".equals(docType)) {
                    addZipEntry(zos, projNm + "_" + docTypeLabel + "_공문.hwpx", hwpxExportService.generateHwpx(id, "letter"));
                }
                if ("COMMENCE".equals(docType)) {
                    addZipEntry(zos, projNm + "_착수계.hwpx", hwpxExportService.generateHwpx(id, "commence_body"));
                    addZipEntry(zos, projNm + "_설계내역서.xlsx", excelExportService.generateDesignEstimate(id));
                } else if ("INTERIM".equals(docType)) {
                    addZipEntry(zos, projNm + "_기성검사원.hwpx", hwpxExportService.generateHwpx(id, "inspector"));
                    addZipEntry(zos, projNm + "_기성내역서.xlsx", excelExportService.generateInterimReport(id));
                } else if ("COMPLETION".equals(docType)) {
                    try { addZipEntry(zos, projNm + "_준공계_KRAS.hwpx", hwpxExportService.generateHwpx(id, "completion_body")); } catch (Exception ignore) {}
                    try { addZipEntry(zos, projNm + "_준공계_UPIS.hwpx", hwpxExportService.generateHwpx(id, "completion_body_upis")); } catch (Exception ignore) {}
                }
            }

            String filename = projNm + "_" + docTypeLabel + "_일괄.zip";
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
            return ResponseEntity.ok()
                    .header("Content-Type", "application/zip")
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(baos.toByteArray());
        } catch (Exception e) {
            log.error("일괄 ZIP 생성 실패", e);
            return ResponseEntity.status(500).build();
        }
    }

    private void addZipEntry(java.util.zip.ZipOutputStream zos, String name, byte[] data) throws java.io.IOException {
        zos.putNextEntry(new java.util.zip.ZipEntry(name));
        zos.write(data);
        zos.closeEntry();
    }

    // === 전자서명 API ===

    @ResponseBody
    @PostMapping("/api/signature/save")
    public ResponseEntity<Map<String, Object>> saveSignature(@RequestBody Map<String, Object> data) {
        if (!"EDIT".equals(getAuth())) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        try {
            Integer docId = Integer.valueOf(data.get("docId").toString());
            String signerType = (String) data.get("signerType");
            String signerName = (String) data.get("signerName");
            String signerOrg = (String) data.get("signerOrg");
            String signatureImage = (String) data.get("signatureImage"); // Base64 data URL

            documentService.saveSignature(docId, signerType, signerName, signerOrg, signatureImage);
            logService.log("문서관리", "서명", "전자서명 저장 (문서ID: " + docId + ", " + signerType + ")");

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // === 첨부파일 관리 ===

    @Autowired private DocumentAttachmentService attachmentService;

    @ResponseBody
    @PostMapping("/api/attachment/upload/{docId}")
    public ResponseEntity<Map<String, Object>> uploadAttachment(
            @PathVariable Integer docId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (!"EDIT".equals(getAuth())) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        try {
            var attachment = attachmentService.saveAttachment(docId, file);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "attachId", attachment.getAttachId(),
                    "fileName", attachment.getFileName(),
                    "fileSize", attachment.getFileSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @ResponseBody
    @GetMapping("/api/attachments/{docId}")
    public ResponseEntity<List<Map<String, Object>>> getAttachments(@PathVariable Integer docId) {
        var attachments = attachmentService.getAttachments(docId);
        List<Map<String, Object>> result = attachments.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("attachId", a.getAttachId());
            m.put("fileName", a.getFileName());
            m.put("fileSize", a.getFileSize());
            m.put("mimeType", a.getMimeType());
            m.put("uploadedAt", a.getUploadedAt().toString());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/attachment/delete/{attachId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteAttachment(@PathVariable Integer attachId) {
        if (!"EDIT".equals(getAuth())) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }
        attachmentService.deleteAttachment(attachId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // === 사용자 정보 API (현장대리인/과업참여자용) ===

    @ResponseBody
    @GetMapping("/api/user/{userSeq}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable Long userSeq) {
        return userRepository.findById(userSeq).map(u -> {
            Map<String, Object> data = new HashMap<>();
            data.put("userSeq", u.getUserSeq());
            data.put("username", u.getUsername());
            data.put("positionTitle", u.getPositionTitle());
            data.put("position", u.getPosition());
            data.put("techGrade", u.getTechGrade());
            data.put("mobile", u.getMobile());
            data.put("tel", u.getTel());
            data.put("email", u.getEmail());
            data.put("address", u.getAddress());
            data.put("ssn", u.getSsn());
            data.put("certificate", u.getCertificate());
            data.put("tasks", u.getTasks());
            data.put("deptNm", u.getDeptNm());
            data.put("teamNm", u.getTeamNm());
            data.put("careerYears", u.getCareerYears());
            data.put("fieldRole", u.getFieldRole());
            return ResponseEntity.ok(data);
        }).orElse(ResponseEntity.notFound().build());
    }

    // === sw_pjt 프로젝트 정보 API (착수계/기성계/준공계용) ===

    @ResponseBody
    @GetMapping("/api/project/{projId}")
    public ResponseEntity<Map<String, Object>> getProjectInfo(@PathVariable Long projId) {
        return swProjectRepository.findById(projId).map(p -> {
            Map<String, Object> data = new HashMap<>();
            data.put("projId", p.getProjId());
            data.put("projNm", p.getProjNm());
            data.put("sysNm", p.getSysNm());
            data.put("sysNmEn", p.getSysNmEn());
            data.put("client", p.getClient());
            data.put("orgNm", p.getOrgNm());
            data.put("orgLghNm", p.getOrgLghNm());
            data.put("cityNm", p.getCityNm());
            data.put("distNm", p.getDistNm());
            data.put("distCd", p.getDistCd());
            data.put("contAmt", p.getContAmt());
            data.put("contRt", p.getContRt());
            data.put("swAmt", p.getSwAmt());
            data.put("hwAmt", p.getHwAmt());
            data.put("contDt", p.getContDt() != null ? p.getContDt().toString() : null);
            data.put("startDt", p.getStartDt() != null ? p.getStartDt().toString() : null);
            data.put("endDt", p.getEndDt() != null ? p.getEndDt().toString() : null);
            data.put("instDt", p.getInstDt() != null ? p.getInstDt().toString() : null);
            data.put("contEnt", p.getContEnt());
            data.put("contDept", p.getContDept());
            data.put("contType", p.getContType());
            data.put("bizType", p.getBizType());
            data.put("bizCat", p.getBizCat());
            data.put("bizCatEn", p.getBizCatEn());
            data.put("maintType", p.getMaintType());
            data.put("prePay", p.getPrePay());
            data.put("payProg", p.getPayProg());
            data.put("payComp", p.getPayComp());
            data.put("year", p.getYear());

            // PersonInfo 연동 (담당자 정보)
            if (p.getPersonId() != null) {
                personInfoRepository.findById(p.getPersonId()).ifPresent(pi -> {
                    data.put("personNm", pi.getUserNm());
                    data.put("personTel", pi.getTel());
                    data.put("personEmail", pi.getEmail());
                    data.put("personDept", pi.getDeptNm());
                    data.put("personTeam", pi.getTeamNm());
                    data.put("personPos", pi.getPos());
                    data.put("personOrg", pi.getOrgNm());
                });
            }
            return ResponseEntity.ok(data);
        }).orElse(ResponseEntity.notFound().build());
    }

    // === 사업 검색 3단계 필터 API ===

    /** 1단계: 연도 목록 */
    @ResponseBody
    @GetMapping("/api/project-years")
    public ResponseEntity<List<Integer>> getProjectYears() {
        return ResponseEntity.ok(swProjectRepository.findDistinctYears());
    }

    /** 2단계: 연도 선택 → 시도(cityNm) 목록 */
    @ResponseBody
    @GetMapping("/api/project-cities")
    public ResponseEntity<List<String>> getProjectCities(@RequestParam Integer year) {
        return ResponseEntity.ok(swProjectRepository.findDistinctCityNmByYear(year));
    }

    /** 2-1단계: 연도+시도 선택 → 시군구(distNm) 목록 */
    @ResponseBody
    @GetMapping("/api/project-districts")
    public ResponseEntity<List<String>> getProjectDistricts(
            @RequestParam Integer year, @RequestParam String cityNm) {
        return ResponseEntity.ok(swProjectRepository.findDistinctDistNmByYearAndCityNm(year, cityNm));
    }

    /** 3단계: 연도+지자체 → 시스템영문명 목록 */
    @ResponseBody
    @GetMapping("/api/project-systems")
    public ResponseEntity<List<String>> getProjectSystems(
            @RequestParam Integer year, @RequestParam String cityNm, @RequestParam String distNm) {
        return ResponseEntity.ok(swProjectRepository.findDistinctSysNmEnByYearAndCity(year, cityNm, distNm));
    }

    /** 최종: 연도+지자체+시스템 → 사업 목록 */
    @ResponseBody
    @GetMapping("/api/projects")
    public ResponseEntity<List<Map<String, Object>>> getProjectsFiltered(
            @RequestParam Integer year, @RequestParam String cityNm,
            @RequestParam String distNm, @RequestParam String sysNmEn) {
        var projects = swProjectRepository.findByYearAndCityNmAndDistNmAndSysNmEnOrderByProjIdDesc(
                year, cityNm, distNm, sysNmEn);
        List<Map<String, Object>> result = projects.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("projId", p.getProjId());
            m.put("year", p.getYear());
            m.put("projNm", p.getProjNm());
            m.put("sysNm", p.getSysNm());
            m.put("sysNmEn", p.getSysNmEn());
            m.put("contAmt", p.getContAmt());
            m.put("cityNm", p.getCityNm());
            m.put("distNm", p.getDistNm());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    // ========== 일괄 작성 기능 ==========

    /** 일괄 작성 페이지 */
    @GetMapping("/batch")
    public String batchPage(Model model) {
        if (!"EDIT".equals(getAuth())) {
            return "redirect:/document/list";
        }
        model.addAttribute("users", userRepository.findByEnabledTrue());
        return "document/doc-batch";
    }

    /** 일괄 대상 사업 목록 조회 API */
    @ResponseBody
    @GetMapping("/api/project-systems-all")
    public ResponseEntity<List<Map<String, Object>>> getAllSystemsForYear(@RequestParam Integer year) {
        var projects = swProjectRepository.findAll().stream()
                .filter(p -> year.equals(p.getYear()))
                .filter(p -> p.getSysNmEn() != null && !p.getSysNmEn().isEmpty())
                .toList();
        java.util.LinkedHashMap<String, String> map = new java.util.LinkedHashMap<>();
        projects.forEach(p -> map.putIfAbsent(p.getSysNmEn(), p.getSysNm()));
        List<Map<String, Object>> result = map.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("sysNmEn", e.getKey());
            m.put("sysNm", e.getValue());
            return m;
        }).sorted((a, b) -> String.valueOf(a.get("sysNmEn")).compareTo(String.valueOf(b.get("sysNmEn")))).toList();
        return ResponseEntity.ok(result);
    }

    @ResponseBody
    @GetMapping("/api/batch/targets")
    public ResponseEntity<List<Map<String, Object>>> getBatchTargets(
            @RequestParam Integer year, @RequestParam String docType,
            @RequestParam(required = false) String sysNmEn) {

        List<com.swmanager.system.domain.SwProject> projects;
        if ("INTERIM".equals(docType)) {
            projects = swProjectRepository.findByYearAndInterimYnOrderByCityNmAscDistNmAsc(year, "Y");
        } else if ("COMPLETION".equals(docType)) {
            projects = swProjectRepository.findByYearAndCompletionYnOrderByCityNmAscDistNmAsc(year, "Y");
        } else {
            return ResponseEntity.badRequest().body(List.of());
        }
        if (sysNmEn != null && !sysNmEn.isEmpty()) {
            projects = projects.stream().filter(p -> sysNmEn.equals(p.getSysNmEn())).toList();
        }

        List<Map<String, Object>> result = projects.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("projId", p.getProjId());
            m.put("projNm", p.getProjNm());
            m.put("sysNmEn", p.getSysNmEn());
            m.put("cityNm", p.getCityNm());
            m.put("distNm", p.getDistNm());
            m.put("orgNm", p.getOrgNm());
            m.put("contAmt", p.getContAmt());
            m.put("contDt", p.getContDt() != null ? p.getContDt().toString() : null);
            m.put("startDt", p.getStartDt() != null ? p.getStartDt().toString() : null);
            m.put("endDt", p.getEndDt() != null ? p.getEndDt().toString() : null);
            m.put("client", p.getClient());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    /** 일괄 자동 생성 API */
    @ResponseBody
    @PostMapping("/api/batch/generate")
    public ResponseEntity<Map<String, Object>> batchGenerate(@RequestBody Map<String, Object> requestData) {
        if (!"EDIT".equals(getAuth())) {
            return ResponseEntity.status(403).body(Map.of("error", "권한이 없습니다."));
        }

        try {
            CustomUserDetails cu = getCurrentUser();
            User author = cu != null ? cu.getUser() : null;

            String docType = (String) requestData.get("docType");
            @SuppressWarnings("unchecked")
            List<Number> projIds = (List<Number>) requestData.get("projIds");
            @SuppressWarnings("unchecked")
            Map<String, Object> commonData = (Map<String, Object>) requestData.get("commonData");

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (Number projIdNum : projIds) {
                Long projId = projIdNum.longValue();
                try {
                    var projOpt = swProjectRepository.findById(projId);
                    if (projOpt.isEmpty()) {
                        failCount++;
                        results.add(Map.of("projId", projId, "success", false, "error", "프로젝트 없음"));
                        continue;
                    }
                    var p = projOpt.get();

                    // 문서 생성 - 기성계는 "기성금 신청 건", 준공계는 "준공계 제출 건"
                    String title = "「" + p.getProjNm() + "」" +
                            (("INTERIM".equals(docType)) ? "기성금 신청 건" : "준공계 제출 건");

                    Document doc = documentService.createDocument(docType, p.getSysNmEn(), null, null, null, title, author);
                    doc.setProject(p);

                    // 공문(letter) 섹션 자동 생성
                    Map<String, Object> letterData = buildBatchLetterData(p, docType, commonData);
                    documentService.saveSection(doc.getDocId(), "letter", letterData, 0);

                    // 본문 섹션 자동 생성
                    if ("INTERIM".equals(docType)) {
                        // inspector 섹션 (기성검사원)
                        Map<String, Object> insp = new HashMap<>();
                        insp.put("name", p.getProjNm());
                        insp.put("amount", p.getContAmt() != null ? p.getContAmt().toString() : "");
                        insp.put("contractDate", p.getContDt() != null ? p.getContDt().toString() : "");
                        insp.put("periodFrom", p.getStartDt() != null ? p.getStartDt().toString() : "");
                        insp.put("periodTo", p.getEndDt() != null ? p.getEndDt().toString() : "");
                        if (commonData != null) {
                            if (commonData.get("interimYear") != null) insp.put("interimYear", commonData.get("interimYear"));
                            if (commonData.get("interimMonth") != null) insp.put("interimMonth", commonData.get("interimMonth"));
                            if (commonData.get("interimDay") != null) insp.put("interimDay", commonData.get("interimDay"));
                            Object pr = commonData.get("paymentRate");
                            if (pr != null && !pr.toString().isEmpty()) {
                                insp.put("paymentRate", pr);
                                // 기성율 × 계약금 → 금회기성금액 자동 계산
                                try {
                                    double rate = Double.parseDouble(pr.toString());
                                    long contAmt = p.getContAmt() != null ? p.getContAmt() : 0L;
                                    long paymentAmt = Math.round(contAmt * rate / 100.0);
                                    insp.put("paymentAmount", String.valueOf(paymentAmt));
                                } catch (NumberFormatException ignore) {}
                            }
                        }
                        documentService.saveSection(doc.getDocId(), "inspector", insp, 1);

                        // detail_sheet 섹션 (기성내역서)
                        Map<String, Object> detail = new HashMap<>();
                        detail.put("name", p.getProjNm());
                        detail.put("contAmt", p.getContAmt());
                        detail.put("bidRate", p.getContRt());
                        if (commonData != null) {
                            if (commonData.get("periodText") != null) detail.put("periodText", commonData.get("periodText"));
                            if (commonData.get("prevRate") != null) detail.put("prevRate", commonData.get("prevRate"));
                        }
                        // KRAS 시스템이면 GeoNURIS for KRAS v1.0 항목 자동 추가
                        String sysEn = p.getSysNmEn() != null ? p.getSysNmEn().toUpperCase() : "";
                        String sysKo = p.getSysNm() != null ? p.getSysNm() : "";
                        if (sysEn.contains("KRAS") || sysKo.contains("KRAS")) {
                            List<Map<String, Object>> items = new ArrayList<>();
                            Map<String, Object> it = new HashMap<>();
                            it.put("name", "GeoNURIS for KRAS v1.0");
                            it.put("unitPrice", 77000000L);
                            items.add(it);
                            detail.put("items", items);
                        }
                        documentService.saveSection(doc.getDocId(), "detail_sheet", detail, 2);
                    } else {
                        Map<String, Object> compData = new HashMap<>();
                        compData.put("name", p.getProjNm());
                        compData.put("amount", p.getContAmt() != null ? p.getContAmt().toString() : "");
                        compData.put("contractDate", p.getContDt() != null ? p.getContDt().toString() : "");
                        compData.put("startDate", p.getStartDt() != null ? p.getStartDt().toString() : "");
                        compData.put("endDate", p.getEndDt() != null ? p.getEndDt().toString() : "");
                        documentService.saveSection(doc.getDocId(), "completion", compData, 1);
                    }

                    successCount++;
                    results.add(Map.of("projId", projId, "success", true, "docId", doc.getDocId(),
                            "projNm", p.getProjNm(), "cityNm", p.getCityNm(), "distNm", p.getDistNm()));

                } catch (Exception e) {
                    failCount++;
                    results.add(Map.of("projId", projId, "success", false, "error", e.getMessage()));
                }
            }

            logService.log("문서관리", "일괄생성",
                    docType + " 일괄생성 (성공: " + successCount + ", 실패: " + failCount + ")");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "totalCount", projIds.size(),
                    "successCount", successCount,
                    "failCount", failCount,
                    "results", results
            ));
        } catch (Exception e) {
            log.error("일괄 생성 실패", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** 일괄 생성용 공문(letter) 데이터 빌드 */
    private Map<String, Object> buildBatchLetterData(com.swmanager.system.domain.SwProject p,
                                                      String docType, Map<String, Object> commonData) {
        Map<String, Object> data = new HashMap<>();

        // 수신자 자동 생성
        String recipient = p.getOrgNm() != null ? p.getOrgNm() :
                (p.getDistNm() != null ? p.getDistNm() + "청" : p.getCityNm() + "청");
        data.put("to", recipient);

        // 공통 데이터 (담당자, 문서번호 등)
        if (commonData != null) {
            if (commonData.get("manager") != null) data.put("manager", commonData.get("manager"));
            if (commonData.get("tel") != null) data.put("tel", commonData.get("tel"));
            if (commonData.get("date") != null) data.put("date", commonData.get("date"));
        }

        // 제목 - 기성계는 "기성금 신청 건", 준공계는 "준공계 제출 건"
        String titleSuffix = "INTERIM".equals(docType) ? "기성금 신청 건" : "준공계 제출 건";
        data.put("title", "「" + p.getProjNm() + "」" + titleSuffix);

        // 본문
        String contDtFmt = "";
        if (p.getContDt() != null) {
            contDtFmt = p.getContDt().getYear() + ". " +
                    p.getContDt().getMonthValue() + ". " +
                    p.getContDt().getDayOfMonth() + ".";
        }

        String body2;
        String attachList;
        if ("INTERIM".equals(docType)) {
            body2 = "2. 귀 기관과 당사 간에 계약(" + contDtFmt + ")한 『" + p.getProjNm() +
                    "』과 관련하여 붙임과 같이 기성을 신청하오니 검토 후 조치하여 주시기 바랍니다.";
            attachList = "1. 기성검사원 1부.\n                          2. 기성내역서 1부.\n                          3. 점검내역서 1부.    끝.";
        } else {
            body2 = "2. 귀 기관과 당사 간에 계약(" + contDtFmt + ")한 『" + p.getProjNm() +
                    "』에 관하여 과업을 완료함에 따라 제출합니다.";
            attachList = "1. 준공계 2부.    끝.";
        }
        data.put("body", "1. 귀 기관의 무궁한 발전을 기원합니다.\n\n" + body2 + "\n\n\n※ 붙 임 : " + attachList);

        return data;
    }

    // ── Phase 2: 공정명 마스터 / 용역목적 / 사업별 참여자 API ──

    /** 시스템별 공정명 목록 조회 */
    @GetMapping("/api/process-master")
    @ResponseBody
    public List<Map<String, Object>> getProcessMasterList(@RequestParam String sysNmEn) {
        List<ProcessMaster> list = processMasterRepository.findBySysNmEnAndUseYnOrderBySortOrder(sysNmEn, "Y");
        List<Map<String, Object>> result = new ArrayList<>();
        for (ProcessMaster pm : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("processId", pm.getProcessId());
            m.put("processName", pm.getProcessName());
            result.add(m);
        }
        return result;
    }

    /** 시스템별 용역목적/과업내용 조회 */
    @GetMapping("/api/service-purpose")
    @ResponseBody
    public List<Map<String, Object>> getServicePurposeList(
            @RequestParam String sysNmEn,
            @RequestParam(required = false) String purposeType) {
        List<ServicePurpose> list;
        if (purposeType != null && !purposeType.isEmpty()) {
            list = servicePurposeRepository.findBySysNmEnAndPurposeTypeAndUseYnOrderBySortOrder(sysNmEn, purposeType, "Y");
        } else {
            list = servicePurposeRepository.findBySysNmEnAndUseYnOrderBySortOrder(sysNmEn, "Y");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (ServicePurpose sp : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("purposeId", sp.getPurposeId());
            m.put("purposeType", sp.getPurposeType());
            m.put("purposeText", sp.getPurposeText());
            result.add(m);
        }
        return result;
    }

    /** 사업별 과업참여자 조회 */
    @GetMapping("/api/contract-participants/{projId}")
    @ResponseBody
    public List<Map<String, Object>> getContractParticipants(@PathVariable Long projId) {
        List<ContractParticipant> list = contractParticipantRepository.findByProject_ProjIdOrderBySortOrder(projId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ContractParticipant cp : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("participantId", cp.getParticipantId());
            m.put("userId", cp.getUser() != null ? cp.getUser().getUserSeq() : null);
            m.put("userName", cp.getUser() != null ? cp.getUser().getUsername() : "");
            m.put("position", cp.getUser() != null ? cp.getUser().getPositionTitle() : "");
            m.put("roleType", cp.getRoleType());
            m.put("techGrade", cp.getTechGrade());
            m.put("taskDesc", cp.getTaskDesc());
            m.put("isSiteRep", cp.getIsSiteRep());
            m.put("ssn", cp.getUser() != null ? cp.getUser().getSsn() : "");
            m.put("certificate", cp.getUser() != null ? cp.getUser().getCertificate() : "");
            m.put("tasks", cp.getUser() != null ? cp.getUser().getTasks() : "");
            result.add(m);
        }
        return result;
    }

    /** 사업별 과업참여자 저장 */
    @PostMapping("/api/contract-participants/{projId}")
    @ResponseBody
    public Map<String, Object> saveContractParticipants(
            @PathVariable Long projId,
            @RequestBody List<Map<String, Object>> participantList) {
        Map<String, Object> result = new HashMap<>();
        try {
            var project = swProjectRepository.findById(projId).orElse(null);
            if (project == null) {
                result.put("success", false);
                result.put("error", "사업을 찾을 수 없습니다.");
                return result;
            }

            // 기존 참여자 삭제 후 재등록
            List<ContractParticipant> existing = contractParticipantRepository.findByProject_ProjIdOrderBySortOrder(projId);
            contractParticipantRepository.deleteAll(existing);

            int order = 0;
            for (Map<String, Object> item : participantList) {
                ContractParticipant cp = new ContractParticipant();
                cp.setProject(project);

                Object userIdObj = item.get("userId");
                if (userIdObj != null) {
                    Long userId = Long.parseLong(userIdObj.toString());
                    userRepository.findById(userId).ifPresent(cp::setUser);
                }

                cp.setRoleType((String) item.getOrDefault("roleType", "PARTICIPANT"));
                cp.setTechGrade((String) item.getOrDefault("techGrade", ""));
                cp.setTaskDesc((String) item.getOrDefault("taskDesc", ""));
                cp.setIsSiteRep(Boolean.TRUE.equals(item.get("isSiteRep")));
                cp.setSortOrder(order++);

                contractParticipantRepository.save(cp);
            }

            result.put("success", true);
            result.put("count", participantList.size());
        } catch (Exception e) {
            log.error("과업참여자 저장 실패: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // ========================================================
    // 사업수행계획서 (P10~P13) API
    // ========================================================

    /** 사업수행계획서 데이터 조회 */
    @ResponseBody
    @GetMapping("/api/plan/{projId}")
    public ResponseEntity<Map<String, Object>> getPlanData(@PathVariable Long projId) {
        Map<String, Object> result = new HashMap<>();
        var pOpt = swProjectRepository.findById(projId);
        if (pOpt.isEmpty()) return ResponseEntity.notFound().build();
        var p = pOpt.get();
        result.put("projPurpose", p.getProjPurpose());
        result.put("supportType", p.getSupportType());
        result.put("scopeText", p.getScopeText());
        result.put("inspectMethod", p.getInspectMethod());

        // targets
        List<Map<String, Object>> targets = new ArrayList<>();
        for (var t : pjtTargetRepository.findByProjIdOrderBySortOrderAsc(projId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("productName", t.getProductName());
            m.put("qty", t.getQty());
            targets.add(m);
        }
        result.put("targets", targets);

        // manpower plans
        List<Map<String, Object>> mps = new ArrayList<>();
        for (var mp : pjtManpowerPlanRepository.findByProjIdOrderBySortOrderAsc(projId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", mp.getId());
            m.put("stepName", mp.getStepName());
            m.put("startDt", mp.getStartDt() != null ? mp.getStartDt().toString() : null);
            m.put("endDt", mp.getEndDt() != null ? mp.getEndDt().toString() : null);
            m.put("gradeSpecial", mp.getGradeSpecial());
            m.put("gradeHigh", mp.getGradeHigh());
            m.put("gradeMid", mp.getGradeMid());
            m.put("gradeLow", mp.getGradeLow());
            m.put("funcHigh", mp.getFuncHigh());
            m.put("funcMid", mp.getFuncMid());
            m.put("funcLow", mp.getFuncLow());
            m.put("remark", mp.getRemark());
            mps.add(m);
        }
        result.put("manpowerPlans", mps);

        // schedules
        List<Map<String, Object>> schs = new ArrayList<>();
        for (var s : pjtScheduleRepository.findByProjIdOrderBySortOrderAsc(projId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("processName", s.getProcessName());
            m.put("m01", s.getM01()); m.put("m02", s.getM02()); m.put("m03", s.getM03());
            m.put("m04", s.getM04()); m.put("m05", s.getM05()); m.put("m06", s.getM06());
            m.put("m07", s.getM07()); m.put("m08", s.getM08()); m.put("m09", s.getM09());
            m.put("m10", s.getM10()); m.put("m11", s.getM11()); m.put("m12", s.getM12());
            m.put("remark", s.getRemark());
            schs.add(m);
        }
        result.put("schedules", schs);

        return ResponseEntity.ok(result);
    }

    /** 사업수행계획서 데이터 저장 (overwrite) */
    @ResponseBody
    @PostMapping("/api/plan/{projId}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Map<String, Object>> savePlanData(
            @PathVariable Long projId,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            var pOpt = swProjectRepository.findById(projId);
            if (pOpt.isEmpty()) return ResponseEntity.notFound().build();
            var p = pOpt.get();

            // sw_pjt 4 columns
            if (body.containsKey("projPurpose"))   p.setProjPurpose((String) body.get("projPurpose"));
            if (body.containsKey("supportType"))   p.setSupportType((String) body.get("supportType"));
            if (body.containsKey("scopeText"))     p.setScopeText((String) body.get("scopeText"));
            if (body.containsKey("inspectMethod")) p.setInspectMethod((String) body.get("inspectMethod"));
            swProjectRepository.save(p);

            // targets - delete & insert
            if (body.containsKey("targets")) {
                pjtTargetRepository.deleteByProjId(projId);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> arr = (List<Map<String, Object>>) body.get("targets");
                int order = 0;
                for (Map<String, Object> m : arr) {
                    var t = new com.swmanager.system.domain.workplan.PjtTarget();
                    t.setProjId(projId);
                    t.setProductName((String) m.get("productName"));
                    Object q = m.get("qty");
                    t.setQty(q == null ? 1 : ((Number) q).intValue());
                    t.setSortOrder(order++);
                    pjtTargetRepository.save(t);
                }
            }

            // manpower plans
            if (body.containsKey("manpowerPlans")) {
                pjtManpowerPlanRepository.deleteByProjId(projId);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> arr = (List<Map<String, Object>>) body.get("manpowerPlans");
                int order = 0;
                for (Map<String, Object> m : arr) {
                    var mp = new com.swmanager.system.domain.workplan.PjtManpowerPlan();
                    mp.setProjId(projId);
                    mp.setStepName((String) m.get("stepName"));
                    String sd = (String) m.get("startDt");
                    String ed = (String) m.get("endDt");
                    if (sd != null && !sd.isEmpty()) mp.setStartDt(java.time.LocalDate.parse(sd));
                    if (ed != null && !ed.isEmpty()) mp.setEndDt(java.time.LocalDate.parse(ed));
                    mp.setGradeSpecial(asInt(m.get("gradeSpecial")));
                    mp.setGradeHigh(asInt(m.get("gradeHigh")));
                    mp.setGradeMid(asInt(m.get("gradeMid")));
                    mp.setGradeLow(asInt(m.get("gradeLow")));
                    mp.setFuncHigh(asInt(m.get("funcHigh")));
                    mp.setFuncMid(asInt(m.get("funcMid")));
                    mp.setFuncLow(asInt(m.get("funcLow")));
                    mp.setRemark((String) m.get("remark"));
                    mp.setSortOrder(order++);
                    pjtManpowerPlanRepository.save(mp);
                }
            }

            // schedules
            if (body.containsKey("schedules")) {
                pjtScheduleRepository.deleteByProjId(projId);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> arr = (List<Map<String, Object>>) body.get("schedules");
                int order = 0;
                for (Map<String, Object> m : arr) {
                    var s = new com.swmanager.system.domain.workplan.PjtSchedule();
                    s.setProjId(projId);
                    s.setProcessName((String) m.get("processName"));
                    s.setM01(asBool(m.get("m01"))); s.setM02(asBool(m.get("m02")));
                    s.setM03(asBool(m.get("m03"))); s.setM04(asBool(m.get("m04")));
                    s.setM05(asBool(m.get("m05"))); s.setM06(asBool(m.get("m06")));
                    s.setM07(asBool(m.get("m07"))); s.setM08(asBool(m.get("m08")));
                    s.setM09(asBool(m.get("m09"))); s.setM10(asBool(m.get("m10")));
                    s.setM11(asBool(m.get("m11"))); s.setM12(asBool(m.get("m12")));
                    s.setRemark((String) m.get("remark"));
                    s.setSortOrder(order++);
                    pjtScheduleRepository.save(s);
                }
            }

            result.put("success", true);
        } catch (Exception e) {
            log.error("사업수행계획서 저장 실패: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    private Integer asInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }
    private Boolean asBool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        return "true".equalsIgnoreCase(o.toString()) || "1".equals(o.toString());
    }

    // ============================================================
    // 점검내역서 API
    // ============================================================

    /** POST /document/api/inspect-report - 저장 (신규/수정) */
    @PostMapping("/api/inspect-report")
    @ResponseBody
    public ResponseEntity<?> saveInspectReport(@RequestBody InspectReportDTO dto) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            InspectReportDTO saved = inspectReportService.save(dto);
            result.put("success", true);
            result.put("data", saved);
        } catch (Exception e) {
            log.error("점검내역서 저장 실패: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /** GET /document/api/inspect-report/{id} - 단건 조회 */
    @GetMapping("/api/inspect-report/{id}")
    @ResponseBody
    public ResponseEntity<?> getInspectReport(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            InspectReportDTO dto = inspectReportService.findById(id);
            result.put("success", true);
            result.put("data", dto);
        } catch (Exception e) {
            log.error("점검내역서 조회 실패: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /** GET /document/api/inspect-reports?pjtId={pjtId} - 프로젝트별 목록 */
    @GetMapping("/api/inspect-reports")
    @ResponseBody
    public ResponseEntity<?> listInspectReports(@RequestParam Long pjtId) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            result.put("success", true);
            result.put("data", inspectReportService.findByProject(pjtId));
        } catch (Exception e) {
            log.error("점검내역서 목록 조회 실패: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /** GET /document/api/inspect-report/previous-visits?pjtId={pjtId}&inspectMonth={YYYY-MM} - 이전 월 이력 조회 (신규 작성용) */
    @GetMapping("/api/inspect-report/previous-visits")
    @ResponseBody
    public ResponseEntity<?> getPreviousVisits(@RequestParam Long pjtId, @RequestParam String inspectMonth) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            result.put("success", true);
            result.put("data", inspectReportService.getPreviousVisits(pjtId, inspectMonth));
        } catch (Exception e) {
            log.error("이전 월 이력 조회 실패: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /** DELETE /document/api/inspect-report/{id} - 삭제 */
    @DeleteMapping("/api/inspect-report/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteInspectReport(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            inspectReportService.delete(id);
            result.put("success", true);
        } catch (Exception e) {
            log.error("점검내역서 삭제 실패: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /** GET /document/api/inspect-template?type={templateType} - 템플릿 조회 */
    @GetMapping("/api/inspect-template")
    @ResponseBody
    public ResponseEntity<?> getInspectTemplate(@RequestParam String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<InspectCheckResultDTO> items = inspectReportService.getTemplateItems(type);
            result.put("success", true);
            result.put("data", items);
        } catch (Exception e) {
            log.error("점검 템플릿 조회 실패: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }

    /** GET /document/api/infra-servers?distNm=양양군&sysNmEn=UPIS - 인프라 서버정보 조회 */
    @GetMapping("/api/infra-servers")
    @ResponseBody
    public ResponseEntity<?> getInfraServers(@RequestParam String distNm, @RequestParam String sysNmEn) {
        var infraList = infraRepository.findByDistNmAndSysNmEn(distNm, sysNmEn);
        if (infraList.isEmpty()) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        var infra = infraList.get(0);
        List<Map<String, Object>> servers = new ArrayList<>();
        for (var s : infra.getServers()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("serverId", s.getServerId());
            m.put("serverType", s.getServerType());
            m.put("ipAddr", s.getIpAddr());
            m.put("osNm", s.getOsNm());
            m.put("macAddr", s.getMacAddr());
            m.put("serverModel", s.getServerModel());
            m.put("serialNo", s.getSerialNo());
            m.put("cpuSpec", s.getCpuSpec());
            m.put("memorySpec", s.getMemorySpec());
            m.put("diskSpec", s.getDiskSpec());
            m.put("networkSpec", s.getNetworkSpec());
            m.put("powerSpec", s.getPowerSpec());
            m.put("osDetail", s.getOsDetail());
            m.put("rackLocation", s.getRackLocation());
            m.put("note", s.getNote());
            // 소프트웨어 목록
            List<Map<String, String>> swList = new ArrayList<>();
            for (var sw : s.getSoftwares()) {
                Map<String, String> swMap = new LinkedHashMap<>();
                swMap.put("swCategory", sw.getSwCategory());
                swMap.put("swNm", sw.getSwNm());
                swMap.put("swVer", sw.getSwVer() != null ? sw.getSwVer() : "");
                swList.add(swMap);
            }
            m.put("softwares", swList);
            servers.add(m);
        }
        return ResponseEntity.ok(servers);
    }

    // ============================================================
    // 점검내역서 미리보기 / PDF 다운로드
    // ============================================================

    /** GET /document/api/inspect-pdf/{reportId} - 점검내역서 PDF 다운로드 */
    @GetMapping("/api/inspect-pdf/{reportId}")
    @ResponseBody
    public ResponseEntity<byte[]> downloadInspectPdf(@PathVariable Long reportId) {
        try {
            byte[] pdf = inspectPdfService.generatePdf(reportId);
            InspectReportDTO report = inspectReportService.findById(reportId);
            String monthSuffix = "";
            if (report.getVisits() != null && !report.getVisits().isEmpty()) {
                monthSuffix = "_" + report.getVisits().get(0).getVisitMonth() + "월";
            } else if (report.getInspectMonth() != null && report.getInspectMonth().length() >= 7) {
                monthSuffix = "_" + Integer.parseInt(report.getInspectMonth().substring(5)) + "월";
            }
            String filename = (report.getDocTitle() != null ? report.getDocTitle() : "점검내역서") + monthSuffix + ".pdf";
            String encoded = java.net.URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename*=UTF-8''" + encoded)
                    .body(pdf);
        } catch (Exception e) {
            log.error("점검내역서 PDF 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** GET /document/inspect-detail/{reportId} - 점검내역서 상세 정보 */
    @GetMapping("/inspect-detail/{reportId}")
    public String inspectDetail(@PathVariable Long reportId, Model model) {
        try {
            InspectReportDTO report = inspectReportService.findById(reportId);
            model.addAttribute("report", report);

            // 점검결과를 섹션별로 분리하여 모델에 추가
            if (report.getCheckResults() != null) {
                model.addAttribute("dbItems", report.getCheckResults().stream().filter(r -> "DB".equals(r.getSection())).toList());
                model.addAttribute("apItems", report.getCheckResults().stream().filter(r -> "AP".equals(r.getSection())).toList());
                model.addAttribute("dbmsItems", report.getCheckResults().stream().filter(r -> "DBMS".equals(r.getSection())).toList());
                model.addAttribute("gisItems", report.getCheckResults().stream().filter(r -> "GIS".equals(r.getSection())).toList());
                model.addAttribute("appItems", report.getCheckResults().stream().filter(r -> "APP".equals(r.getSection())).toList());
                model.addAttribute("dbUsage", report.getCheckResults().stream().filter(r -> "DB_USAGE".equals(r.getSection())).toList());
                model.addAttribute("apUsage", report.getCheckResults().stream().filter(r -> "AP_USAGE".equals(r.getSection())).toList());
                model.addAttribute("dbmsEtc", report.getCheckResults().stream().filter(r -> "DBMS_ETC".equals(r.getSection())).toList());
                model.addAttribute("appEtc", report.getCheckResults().stream().filter(r -> "APP_ETC".equals(r.getSection())).toList());
            }

            var project = swProjectRepository.findById(report.getPjtId()).orElse(null);
            model.addAttribute("project", project);

            if (project != null) {
                var infraList = infraRepository.findByDistNmAndSysNmEn(
                        project.getDistNm(), project.getSysNmEn());
                model.addAttribute("infraList", infraList);
            }

            return "document/inspect-detail";
        } catch (Exception e) {
            log.error("점검내역서 상세 조회 실패: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "점검내역서를 찾을 수 없습니다.");
            return "redirect:/document/list";
        }
    }

    /** GET /document/inspect-preview/{reportId} - 점검내역서 미리보기 */
    @GetMapping("/inspect-preview/{reportId}")
    public String inspectPreview(@PathVariable Long reportId, Model model) {
        try {
            InspectReportDTO report = inspectReportService.findById(reportId);
            model.addAttribute("report", report);

            // 점검결과를 섹션별로 분리하여 모델에 추가
            if (report.getCheckResults() != null) {
                model.addAttribute("dbItems", report.getCheckResults().stream().filter(r -> "DB".equals(r.getSection())).toList());
                model.addAttribute("apItems", report.getCheckResults().stream().filter(r -> "AP".equals(r.getSection())).toList());
                model.addAttribute("dbmsItems", report.getCheckResults().stream().filter(r -> "DBMS".equals(r.getSection())).toList());
                model.addAttribute("gisItems", report.getCheckResults().stream().filter(r -> "GIS".equals(r.getSection())).toList());
                model.addAttribute("appItems", report.getCheckResults().stream().filter(r -> "APP".equals(r.getSection())).toList());
                model.addAttribute("dbUsage", report.getCheckResults().stream().filter(r -> "DB_USAGE".equals(r.getSection())).toList());
                model.addAttribute("apUsage", report.getCheckResults().stream().filter(r -> "AP_USAGE".equals(r.getSection())).toList());
                model.addAttribute("dbmsEtc", report.getCheckResults().stream().filter(r -> "DBMS_ETC".equals(r.getSection())).toList());
                model.addAttribute("appEtc", report.getCheckResults().stream().filter(r -> "APP_ETC".equals(r.getSection())).toList());
            }

            // 프로젝트 정보
            var project = swProjectRepository.findById(report.getPjtId()).orElse(null);
            model.addAttribute("project", project);

            // 인프라 서버 정보
            if (project != null) {
                var infraList = infraRepository.findByDistNmAndSysNmEn(
                        project.getDistNm(), project.getSysNmEn());
                model.addAttribute("infraList", infraList);
            }

            // 점검 템플릿 (섹션별 그룹핑용)
            String templateType = report.getSysType() != null ? report.getSysType() : "UPIS";
            List<InspectCheckResultDTO> templateItems = inspectReportService.getTemplateItems(templateType);
            model.addAttribute("templateItems", templateItems);

            return "document/inspect-preview";
        } catch (Exception e) {
            log.error("점검내역서 미리보기 실패: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "점검내역서를 찾을 수 없습니다.");
            return "redirect:/document/list";
        }
    }
}
