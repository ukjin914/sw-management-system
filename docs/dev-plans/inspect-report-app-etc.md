# [개발계획서] 점검내역서 7번 섹션 기타 항목 추가

- **작성팀**: 개발팀
- **작성일**: 2026-04-14
- **기획서**: [docs/plans/inspect-report-app-etc.md](../plans/inspect-report-app-etc.md) (v2 승인됨)
- **상태**: v2 (codex 재검토 대기) — 1차 검토 지적사항 4건 반영

### 개정 이력

| 버전 | 변경 내용 |
|------|-----------|
| v1 | 초안 |
| v2 | codex 검토 반영: ①KRAS 시 7번+기타 비표시로 T4 정정 ②T14 저장 방식 확정(delete-all-then-insert → 삭제됨) ③FR-10 잔여 글자 수 안내 UI/JS/테스트 추가 ④완료 체크리스트에 T14, T15 포함 |

---

## 1. 영향 범위

| 계층 | 파일 | 변경 유형 |
|------|------|-----------|
| Frontend (편집) | `src/main/resources/templates/document/doc-inspect.html` | 마크업 추가 + JS 2곳 확장 |
| Frontend (상세) | `src/main/resources/templates/document/inspect-detail.html` | Thymeleaf 블록 추가 |
| Frontend (미리보기) | `src/main/resources/templates/document/inspect-preview.html` | Thymeleaf 블록 추가 |
| Frontend (PDF) | `src/main/resources/templates/pdf/pdf-inspect-report.html` | Thymeleaf 블록 추가 |
| Backend (Controller) | `src/main/java/com/swmanager/system/controller/DocumentController.java` | `appEtc` 모델 속성 2곳 추가 |
| Backend (Service) | `src/main/java/com/swmanager/system/service/InspectPdfService.java` | `appEtc` 컨텍스트 변수 1곳 추가 |
| DB | **없음** | — |
| Repository/Entity | **없음** | 기존 `InspectCheckResult` 엔티티 재사용 |

**총 6개 파일, DB/Entity/Repository 변경 없음**

---

## 2. 변경 파일별 상세 내용

### 2-1. `doc-inspect.html` (편집 화면)

#### (A) 마크업 추가 — 라인 352 뒤 (표준시스템 점검결과 테이블 직후)

```html
<!-- 기존: line 347~352 -->
<!-- 표준시스템 점검결과 (UPIS/UPIS_SW) -->
<div class="sub-title" id="appTitle" style="margin-top:30px;">표준시스템 점검결과</div>
<table class="check-table" id="appCheckTable">
    <thead>...</thead>
    <tbody id="appCheckBody"></tbody>
</table>

<!-- 신규 추가: APP 기타 -->
<table class="check-table" id="appEtcTable" style="margin-top:0;">
    <thead><tr><th colspan="1" style="background:#e3f2fd;text-align:left;font-weight:bold;">기타</th></tr></thead>
    <tbody>
        <tr><td>
            <textarea id="app_etc" rows="4" maxlength="500"
                      style="width:100%;border:none;resize:vertical;"
                      placeholder="기타 사항 입력"
                      oninput="updateAppEtcCounter()"></textarea>
            <small id="app_etc_counter"
                   style="display:block;text-align:right;color:#999;font-size:0.8em;margin-top:2px;">
                0 / 500
            </small>
        </td></tr>
    </tbody>
</table>
```

> DBMS 기타(line 331-336)와 기본 구조 동일. 추가 속성: `id="appEtcTable"` (숨김 제어용), `maxlength="500"`, `oninput` 으로 글자 수 카운터 갱신 (FR-10).

#### (A-2) 잔여 글자 수 안내 JS — 전역 함수 (FR-10)

```javascript
function updateAppEtcCounter() {
    var el = document.getElementById('app_etc');
    var counter = document.getElementById('app_etc_counter');
    if (!el || !counter) return;
    var len = el.value.length;
    var max = 500;
    counter.textContent = len + ' / ' + max;
    // 95% 이상(475자 이상)일 때 붉은색으로 강조 (FR-10)
    counter.style.color = (len >= max * 0.95) ? '#e74a3b' : '#999';
}
// 로드 시에도 한 번 호출 (복원된 값 반영)
```

> `restoreCheckResults()` 에서 `APP_ETC` 복원 후 `updateAppEtcCounter()` 호출 추가.

#### (B) 저장 JS 확장 — 라인 1184 뒤

```javascript
// 기존 (line 1180~1184): DBMS 기타
var dbmsEtcEl = document.getElementById('dbms_etc');
if (dbmsEtcEl && dbmsEtcEl.value) {
    checkResults.push({ section: 'DBMS_ETC', ... });
}

// 신규 추가: APP 기타 (7번 섹션이 보일 때만 저장 — FR-9)
var appEtcEl = document.getElementById('app_etc');
var appTable = document.getElementById('appCheckTable');
var appVisible = appTable && appTable.style.display !== 'none';
if (appEtcEl && appEtcEl.value && appVisible) {
    checkResults.push({ section: 'APP_ETC', category: '기타', itemName: '', itemMethod: '', result: appEtcEl.value, sortOrder: 0 });
}
```

> **핵심**: `appVisible` 체크로 FR-9 구현 (숨겨진 섹션은 payload 제외).

#### (C) 로드 JS 확장 — 라인 1452 뒤

```javascript
// 기존 (line 1448~1452): DBMS_ETC 복원
if (r.section === 'DBMS_ETC') { ... return; }

// 신규 추가
if (r.section === 'APP_ETC') {
    var appEtcEl2 = document.getElementById('app_etc');
    if (appEtcEl2) {
        appEtcEl2.value = r.result || '';
        updateAppEtcCounter();  // FR-10: 복원 후 카운터 동기화
    }
    return;
}
```

#### (D) 섹션 표시/숨김 로직 확장 — 라인 789~847 부근

기존 `appTitle.textContent = ...` / `appTable.style.display = ...` 들과 **완전히 같은 타이밍**에 `appEtcTable` 도 함께 표시/숨김 제어:

```javascript
// 예: line 789~790 부근
appTitle.textContent = '표준시스템 점검결과';
appTable.style.display = '';
document.getElementById('appEtcTable').style.display = '';   // 신규

// 숨길 때도 동일하게 추가
document.getElementById('appEtcTable').style.display = 'none';
```

확인 후 영향 받는 분기 모두에 동일 패턴 적용 (UPIS / UPIS_SW / KRAS / 미해당 유형 등).

---

### 2-2. `DocumentController.java`

두 곳 (상세 뷰 핸들러, 미리보기 핸들러 — 기존 `dbmsEtc` 가 있는 위치):

```java
// 라인 1442 / 1478 직후
model.addAttribute("dbmsEtc", ...);
model.addAttribute("appEtc",                                                   // 신규
    report.getCheckResults().stream()
          .filter(r -> "APP_ETC".equals(r.getSection()))
          .toList());
```

---

### 2-3. `InspectPdfService.java`

```java
// 라인 50 직후
context.setVariable("dbmsEtc", ...);
context.setVariable("appEtc",                                                  // 신규
    report.getCheckResults().stream()
          .filter(r -> "APP_ETC".equals(r.getSection()))
          .toList());
```

---

### 2-4. `inspect-detail.html` (상세)

라인 260 부근, 7번 섹션 표시 블록 하단에:

```html
<!-- APP 기타 -->
<th:block th:if="${appEtc != null and !appEtc.isEmpty()}">
    <table class="usage-table">
        <thead><tr><th>기타</th></tr></thead>
        <tbody>
            <tr th:each="u : ${appEtc}">
                <td th:text="${u.result}"
                    style="white-space: pre-wrap; word-break: break-word;"></td>
            </tr>
        </tbody>
    </table>
</th:block>
```

> DBMS 기타(line 251~259) 와 동일 구조, `<td>` 에 `pre-wrap` 스타일 추가 (FR-11).

---

### 2-5. `inspect-preview.html` (미리보기)

라인 299 부근, DBMS 기타 블록 뒤에 APP 기타 블록 동일 패턴으로 추가. `class="data-table"` 사용 (해당 파일 컨벤션).

---

### 2-6. `pdf-inspect-report.html` (PDF)

라인 236 부근, DBMS 기타 블록(`<th:block th:if="${dbmsEtc ...}">`) 뒤에 APP 기타 블록 동일 패턴으로 추가. `class="dt"` 사용. `<td>` 에 `pre-wrap` 인라인 스타일 (FR-11, PDF 엔진이 CSS 클래스 누락 가능성 대비).

---

## 3. 작업 순서 (단계별)

| Step | 작업 | 검증 수단 |
|------|------|-----------|
| 1 | 편집 화면 마크업 + maxlength 추가 (2-1 A) | 브라우저에서 7번 섹션 하단에 기타 영역 렌더링 확인 |
| 2 | 편집 화면 JS 저장/로드 확장 (2-1 B, C) | 콘솔 에러 없음, 저장 후 새로고침 시 값 복원 |
| 3 | 섹션 표시/숨김 제어 확장 (2-1 D) | 시스템 유형 전환 시 기타 영역 함께 show/hide |
| 4 | `DocumentController` 2곳 수정 (2-2) | 서버 재시작 후 상세 페이지 500 없음 |
| 5 | `InspectPdfService` 수정 (2-3) | PDF 다운로드 성공 |
| 6 | 상세/미리보기/PDF 템플릿 3곳 추가 (2-4, 2-5, 2-6) | 각 화면에서 기타 메모 표시, 개행 보존 |
| 7 | 전체 빌드 (`./mvnw clean compile`) | BUILD SUCCESS |
| 8 | 서버 재시작 (`bash server-restart.sh`) | `Started SwManagerApplication` |
| 9 | 수동 회귀 테스트 **T1~T15 전체** (아래 4) | 모든 케이스 PASS |
| 10 | codex 검증 | ✅ 승인 |
| 11 | git commit + push (사용자 "작업완료" 후) | master 에 반영 |

---

## 4. 회귀 테스트 항목

| # | 시나리오 | 기대 결과 |
|---|---------|-----------|
| T1 | 편집 화면에 진입 | 7번 섹션 아래 "기타" 텍스트영역 표시, placeholder `기타 사항 입력` |
| T2 | UPIS 유형 선택 | 7번 + 기타 영역 모두 표시 |
| T3 | UPIS_SW 유형 선택 | 7번 + 기타 영역 표시 상태 유지 (기존 로직 대로) |
| T4 | KRAS 유형 선택 | 7번 섹션(`appTitle`/`appCheckTable`) 비표시 → **기타 영역도 함께 숨김** (FR-7) |
| T5 | 유형 전환(UPIS→KRAS→UPIS) 반복 | 전환 시마다 기타 영역 표시 상태가 7번 섹션과 정확히 동기화 |
| T6 | 기타에 여러 줄 텍스트 입력 후 저장 → 재진입 | 텍스트 복원, 개행 유지, 카운터 숫자 동기화 |
| T7 | 기타에 500 자 입력 시도 | `maxlength` 에 의해 500 자에서 더 이상 입력 안됨 |
| T8 | 기타 빈 값으로 저장 | 상세 화면에서 기타 블록 **미표시** (FR-8) |
| T9 | 기타 값 입력 후 상세 보기 | 입력값 표시, 개행 보존 |
| T10 | 기타 값 입력 후 미리보기 | 입력값 표시, 개행 보존 |
| T11 | 기타 값 입력 후 PDF 다운로드 | PDF 에 입력값 표시, 개행 보존 |
| T12 | 과거 리포트(APP_ETC 없음) 상세/미리보기/PDF 조회 | 기타 블록 미표시, 에러 없음 |
| T13 | DBMS 기타와 APP 기타 모두 입력 후 저장/재조회 | 두 값 모두 독립적으로 저장·표시 |
| T14 | 7번 섹션 숨겨진 상태(KRAS)로 유형 변경 후 저장, 그 후 DB 조회 | DB 의 기존 `APP_ETC` 행이 **삭제됨** (의도된 동작) — FR-9 + delete-all-then-insert 저장 방식의 결과 |
| T15 | 기타 텍스트영역에 475 자 입력 | 카운터가 붉은색(`#e74a3b`)으로 변경되고 "475 / 500" 표시 (FR-10) |

### 저장 방식 확정 (T14 판정 기준)

**확인 결과**: `InspectReportService.java:62` 에서 리포트 저장 시 `checkResultRepository.deleteByReportId()` 로 기존 모든 행을 삭제한 뒤, 새 payload 의 행들을 삽입한다 → **delete-all-then-insert 방식**.

**해석 확정**:
- 유형을 UPIS → KRAS 로 바꾼 후 저장하면, FR-9 에 의해 `APP_ETC` 가 payload 에서 빠짐
- 동시에 전체 delete 가 일어나므로 기존 `APP_ETC` 행이 **삭제됨**
- 이는 **의도된 동작**: 유형이 바뀌면 그 섹션의 데이터도 무의미해지므로 정리하는 것이 데이터 정합성에 맞음
- 사용자 관점: KRAS 로 바꾸고 저장한 리포트를 다시 UPIS 로 돌려도 과거 APP_ETC 메모는 복원되지 않음. 이 부분은 운영 공지 필요 여부를 사용자에게 확인.

### 합의된 동작 (개발 착수 기준)

유형을 KRAS 로 변경 후 저장 시 기존 7번 섹션 기타 메모는 **삭제된다**. 이 동작은 기획서 v2 의 FR-7/FR-9 와 저장 방식(delete-all-then-insert) 의 자연스러운 결과로, 개발 착수 시점에 **승인된 동작**으로 확정한다.

---

## 5. 롤백 전략

### 원자적 롤백
변경이 **단일 커밋**에 묶이므로, 문제 발생 시:
```bash
git revert <commit-sha>
git push
bash server-restart.sh
```

### 데이터 롤백
- DB 스키마 변경 없음 → 데이터 롤백 불필요.
- 이미 저장된 `APP_ETC` 행은 단순히 **해석되지 않는** 상태로 남음 (다음 정리 기회에 `DELETE FROM inspect_check_result WHERE section = 'APP_ETC'` 로 제거 가능).

### 배포 중 장애 감지 지표
- 서버 기동 실패 → 자동 재시작 스크립트의 시간 초과 감지 → 롤백
- 편집 화면 JS 에러 → 브라우저 콘솔 모니터링 (수동)
- 상세/미리보기/PDF 500 에러 → `server.log` 체크

---

## 6. 빌드·재시작 영향

- **빌드 필요**: Java 파일 2개 수정(`DocumentController`, `InspectPdfService`)
- **서버 재시작 필요**: 예 (Spring Boot 프로세스 교체)
- **클라이언트 영향**: Thymeleaf 템플릿 즉시 반영 (개발 환경), 프로덕션은 재시작으로 반영
- **재시작 수단**: `bash server-restart.sh` (이전 커밋에서 경로 자동 인식으로 수정됨)

---

## 7. 예상 소요 시간

| 작업 | 시간 |
|------|------|
| 코드 작성 (6 파일) | 20 분 |
| 빌드 + 재시작 | 5 분 |
| 수동 회귀 테스트 (T1~T15) | 20 분 |
| codex 검증 | 5 분 |
| **합계** | **~45 분** |

---

## 8. 체크리스트 (개발 착수 시 즉시 참조)

- [ ] doc-inspect.html: 마크업 (2-1 A)
- [ ] doc-inspect.html: 저장 JS (2-1 B, `appVisible` 체크 포함)
- [ ] doc-inspect.html: 로드 JS (2-1 C)
- [ ] doc-inspect.html: 표시/숨김 (2-1 D, 모든 분기)
- [ ] DocumentController.java: 2곳 `appEtc` 추가
- [ ] InspectPdfService.java: 1곳 `appEtc` 추가
- [ ] inspect-detail.html: 블록 추가 + `pre-wrap`
- [ ] inspect-preview.html: 블록 추가 + `pre-wrap`
- [ ] pdf-inspect-report.html: 블록 추가 + `pre-wrap`
- [ ] `./mvnw clean compile` BUILD SUCCESS
- [ ] `bash server-restart.sh` 기동 성공
- [ ] 회귀 테스트 **T1~T15 전체** 통과 (특히 T14: FR-9 핵심 시나리오)
- [ ] codex 최종 검증 ✅

---

## 9. 승인 요청

본 개발계획서에 대한 codex 검토 및 사용자 최종승인을 요청합니다.
최종승인 시 개발팀이 코드 작성에 착수합니다.
