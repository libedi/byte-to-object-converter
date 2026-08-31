---
name: skill-creator
description: 새로운 agent skill(SKILL.md 기반)을 만들거나 기존 skill을 개선할 때 사용한다. 사용자가 "이 작업을 skill로 만들자", "skill 만들어줘"라고 하거나, skill의 description이 잘 트리거되지 않는다고 하거나, SKILL.md의 frontmatter/구조/scripts·references 구성에 대해 물어볼 때 사용한다.
---

# Skill 만들기

`references/`에 있는 Agent Skills 공식 문서를 참고 자료로 삼아, 새 skill을 만들거나 기존 skill을 개선하는 과정을 안내한다. 문서 내용을 여기 다시 옮겨 적지 않았으니, 각 단계에서 필요한 문서만 그때그때 읽는다 — 한 번에 전부 읽지 않는다.

## Gotchas

- 기존 SKILL.md의 번호 매김 목록(`1. 2. 3...`) 중간에 `Edit`로 새 단계를 끼워 넣으면, 그 뒤 번호들이 자동으로 밀리지 않아 중복 번호가 남는다. 단계를 삽입/삭제한 뒤에는 파일 전체를 다시 읽어 번호 순서와 상호 참조(예: "N번에서 확인한...")가 깨지지 않았는지 확인할 것
- `scripts/trigger_eval.sh`는 `jq`와 `claude` CLI가 설치되어 있어야 동작한다 — 실행 전에 사용자 환경에 있는지 확인

## 참고 문서 (필요할 때만 읽기)

- `references/overview.md` — Skill 개념, progressive disclosure 원리
- `references/specification.md` — SKILL.md 형식/frontmatter 필드 상세 규칙, 디렉토리 구조
- `references/best-practices.md` — scope 잡는 법, 상세도 조절, gotchas/템플릿/체크리스트 패턴
- `references/using-scripts.md` — `scripts/` 디렉토리 설계, self-contained script 작성법
- `references/optimizing-descriptions.md` — description 필드 트리거 정확도 테스트/개선 방법
- `references/evaluating-skills.md` — eval 기반 품질 검증 및 반복 개선 방법

## Available scripts

- **`scripts/new_skill.sh <skill-name>`** — kebab-case 이름을 검증하고 `.claude/skills/<skill-name>/SKILL.md`를 name이 디렉토리명과 이미 일치한 상태로 생성
- **`scripts/trigger_eval.sh <skill-name> <queries.json> [runs]`** — description trigger rate 측정 (Step 7 참고)

## 워크플로우

### 1. 목적/재료 파악

- 새 skill을 만드는 경우: 어떤 반복 작업이나 전문지식을 캡슐화하려는지 파악한다. 가능하면 이 대화에서 방금 완료한 실제 작업을 재료로 삼는다 — 무엇이 통했는지, 사용자가 어떤 지점에서 방향을 바로잡았는지, 입출력 형식이 어땠는지를 그대로 반영한다(`best-practices.md`의 "Start from real expertise").
- 재료가 막연하면(LLM의 일반 지식에만 의존하면) 결과가 "에러를 적절히 처리하라" 같은 뻔한 일반론이 된다. 구체적 절차나 컨벤션이 없으면 채우지 말고 사용자에게 물어본다.
- 기존 skill을 개선하는 경우: 해당 `SKILL.md`와, 있다면 최근 실행에서 나온 실패 사례·수정 지시를 먼저 확인한다.

### 2. Scope 결정

- 하나의 skill은 하나의 응집된 작업 단위여야 한다. 너무 좁으면 한 작업에 여러 skill이 동시에 필요해지고, 너무 넓으면 트리거가 부정확해진다(`best-practices.md` "Design coherent units").
- `.claude/skills/`의 기존 skill 목록을 확인해 경계가 겹치지 않는지 본다.

### 3. 디렉토리/파일 구성

- 기본은 `SKILL.md` 하나로 시작한다. 500줄/5000토큰을 넘길 것 같거나, 특정 상황에서만 필요한 상세 내용이 있으면 `references/`로 분리한다(`specification.md`의 progressive disclosure).
- 매번 새로 작성하게 되는 반복 로직이 있으면 `scripts/`로 뺀다(`using-scripts.md`).
- 이 프로젝트의 배치 규칙을 따른다: `.claude/skills/<skill-name>/SKILL.md`. 디렉토리명은 frontmatter의 `name`과 정확히 같아야 한다(kebab-case). 새 skill을 만들 때는 `scripts/new_skill.sh <skill-name>`로 스캐폴딩하면 이름 검증과 디렉토리 생성이 한 번에 끝나 이 불일치를 원천적으로 방지할 수 있다.

### 4. Frontmatter 작성

- `name`: kebab-case, 64자 이하, 하이픈으로 시작/끝나지 않음, 부모 디렉토리명과 동일.
- `description`: "무엇을 하는지"와 "언제 쓰는지"를 모두 담아 1024자 이내로 작성한다. 사용자가 도메인 키워드를 직접 언급하지 않는 경우까지 커버하도록 명시적으로 적는다(`optimizing-descriptions.md`). 이 저장소의 기존 skill들처럼 "사용자가 X라고 하거나 Y하고 싶을 때 사용한다" 톤을 따른다.
- `license`, `compatibility`, `metadata`, `allowed-tools`는 필요할 때만 추가한다 — 대부분의 skill에는 필요 없다.

### 5. 본문 작성

- 특정 사례에 대한 답 하나가 아니라, "이런 종류의 문제를 어떻게 접근하는지" 절차를 적는다(`best-practices.md` "Favor procedures over declarations").
- agent가 이미 아는 내용(PDF가 뭔지, git이 뭔지)은 생략하고, 이 프로젝트/도메인이 아니면 모를 내용만 담는다.
- 해당하는 패턴만 골라 쓴다(전부 넣지 않음): Gotchas 섹션, 출력 포맷 템플릿, 체크리스트, validation loop, plan-validate-execute — 자세한 예시는 `best-practices.md` 참고.
- 삭제, force push, 외부 발행처럼 위험하거나 되돌리기 어려운 동작이 있으면 가드레일 섹션을 명시한다. 이 저장소의 `git-commit-push`, `git-worktree-create` skill이 참고 예시다.

### 6. 초안 검토 및 반영

- 작성한 `SKILL.md`를 사용자에게 보여주고, `name`/`description` 제약을 다시 확인한다.
- 가능하면 실제 시나리오 1개로 시험 실행해본다(`best-practices.md` "Refine with real execution"). 애매했던 지점은 SKILL.md에 gotcha로 반영한다.
- 기존 skill을 여러 차례 `Edit`로 수정했다면(특히 번호 목록 삽입/삭제), 최종 파일을 다시 읽어 정합성을 확인한다(Gotchas 참고).

### 7. (선택, 사용자가 명시적으로 원할 때만) description 트리거 검증 / 출력 품질 평가

- 참고문서 전체를 훑어 개선 후보를 찾을 때는 바로 다 적용하지 말고, "가벼운 개선(즉시 적용 가능)"과 "무거운 개선(eval 등 상당한 작업량)"으로 나눠 먼저 사용자에게 메뉴로 제시한 뒤, 사용자가 고른 항목만 적용한다.
- description이 잘 트리거되는지 체계적으로 검증하려면 `optimizing-descriptions.md`의 eval query 설계와 train/validation 분리 절차를 따른다. 쿼리셋(`queries.json`, `[{"query": "...", "should_trigger": true}, ...]` 형식)만 준비되면 `scripts/trigger_eval.sh <skill-name> <queries.json>`으로 바로 trigger rate를 측정할 수 있다.
- 출력 품질을 체계적으로 반복 개선하려면 `evaluating-skills.md`의 test case/assertion/grading 절차를 따른다.
- 셋 다 상당한 작업량이 드는 절차다. 사용자가 명시적으로 요청했거나 그 skill이 프로덕션급으로 반복 사용될 예정일 때만 제안한다 — 간단한 1회성 skill에는 과하다.

## 가드레일

- SKILL.md 본문에 이 프로젝트 코드로 알 수 있는 내용(파일 경로, 아키텍처 설명)을 다시 옮겨 적지 않는다 — `CLAUDE.md`가 이미 담당한다.
- `name`은 디렉토리명과 반드시 일치해야 하므로, 이름을 정한 뒤 디렉토리를 만들 때 오타에 주의한다.
- 확신 없는 일반론("에러를 적절히 처리하라" 등)으로 빈칸을 채우지 않는다 — 구체적 지식이 없으면 사용자에게 물어본다.
