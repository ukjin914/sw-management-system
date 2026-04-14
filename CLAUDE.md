# SW Manager 프로젝트 협업 가이드

이 문서는 Claude Code가 swmanager 프로젝트에서 작업할 때 따라야 하는 팀 구조, 보고 라인, 자동화 규칙을 정의합니다.

---

## 1. 가상 팀 구조

본 프로젝트는 다음 4개의 가상 팀과 1개의 검증자(codex) 체계로 운영됩니다.

| 팀 | 역할 | 산출물 |
|----|------|--------|
| 🧭 **기획팀** | 사용자의 요구사항을 받아 1차 기획서를 작성 | 기획서 (요건/UI/플로우) |
| 🗄️ **데이터베이스팀** | DB 설계·최적화 전담. 기획 단계에서 자문 | 스키마 영향, 인덱스, 마이그레이션 의견 |
| 🛠️ **개발팀** | 승인된 기획서를 바탕으로 개발계획 작성 후 구현 | 개발계획서, 실제 코드 |
| 🧪 **테스트 및 검증팀** | 테스트·검증 (Claude는 직접 실행하지 않음) | 검증 결과 → codex로만 수행 |
| 🤖 **codex (검증자)** | 모든 산출물의 게이트웨이/검증자 | 검토 결과를 사용자에게 전달 |

---

## 2. 작업 흐름 (보고 라인)

```
사용자 요청
    ↓
[기획팀] 1차 기획서 작성
    ↓ (Claude → codex)
[codex] 기획서 검토
    ↓ (codex → 사용자)
사용자 피드백
    ├─ "반영" → 기획팀이 의견 반영해 재보고
    └─ "최종승인" → 다음 단계 진행
        ↓
[개발팀] 개발계획서 작성
    ↓ (Claude → codex)
[codex] 개발계획 검토
    ↓ (codex → 사용자)
사용자 최종승인
    ↓
[개발팀] 실제 코드 작성
    ↓
[테스트팀=codex] 테스트·검증
    ↓
사용자 확인 → "작업완료"
    ↓
자동 git commit + push
```

### 핵심 규칙

- **"최종승인" 이전에는 실제 파일을 수정하지 않는다.**
- 기획팀/개발팀의 모든 산출물은 **반드시 codex CLI를 호출해 검토받은 뒤** 사용자에게 전달한다.
- 테스트는 Claude가 직접 돌리지 않고 codex에게 위임한다.
- 단순한 버그 수정·소소한 텍스트 변경 등은 위 절차를 생략할 수 있다 (사용자 판단).

---

## 3. codex 연결 (CLI 호출 방식)

Claude는 산출물을 codex CLI로 전달해 검토 결과를 받는다.

### 호출 예시

```bash
# 기획서 검토
codex review --type plan --file docs/plans/feature-xxx.md

# 개발계획 검토
codex review --type dev-plan --file docs/dev-plans/feature-xxx.md

# 코드 검증
codex verify --files src/main/java/.../XxxService.java
```

### 보고 형식

codex 응답은 사용자에게 다음 형식으로 전달:

```
[codex 검토 결과]
- 평가: ✅ 승인 / ⚠ 수정필요 / ❌ 반려
- 주요 의견: ...
- 권장 수정사항: ...
```

---

## 4. 자동화 규칙

### 4-1. 서버 재시작 자동화
서버(Spring Boot) 재시작이 필요한 변경 후에는 사용자 승인 없이 즉시 재시작한다.

```bash
bash server-restart.sh
```

**이유:** 매번 승인 요청이 작업 흐름을 방해하므로.

### 4-2. "작업완료" → 자동 커밋+푸시
사용자가 "작업완료"라고 말하면 즉시 다음을 수행:
1. `git add` (관련 파일만 명시적으로 추가)
2. `git commit -m "..."` (변경 의도 중심의 커밋 메시지)
3. `git push`

**금지 사항:**
- `.env`, credential, 대용량 바이너리 등 민감/불필요 파일은 절대 커밋하지 않는다.
- `git add -A` / `git add .` 대신 파일을 명시적으로 지정한다.

### 4-3. 작업 도구 우선순위
- 검색: `Grep` (NOT `grep`/`rg`)
- 파일 찾기: `Glob` (NOT `find`/`ls`)
- 파일 읽기: `Read` (NOT `cat`/`head`/`tail`)
- 파일 수정: `Edit` (NOT `sed`/`awk`)

---

## 5. 프로젝트 정보

- **서버 포트:** 9090
- **DB:** PostgreSQL @ 211.104.137.55:5881/SW_Dept
- **빌드/실행:** `./mvnw spring-boot:run` 또는 `bash server-restart.sh`
- **주요 문서:**
  - [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) — 배포 가이드
  - [DEVELOPMENT_GUIDELINES.md](./DEVELOPMENT_GUIDELINES.md) — 개발 가이드라인
  - [docs/ERD.md](./docs/ERD.md) — ERD 문서
  - [docs/AI_SEARCH_PLAN.md](./docs/AI_SEARCH_PLAN.md) — AI 검색 기획서
