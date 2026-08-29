# 리뷰 체크리스트

## 공통 (requirements.md, design.md, tasks.md)

### 1. 형식 준수
- `.claude/skills/spec/SKILL.md`의 규칙을 따르는지 확인
- 키워드=영문, 내용=한글 원칙 준수 (SKILL.md "언어 규칙" 참고)
- Acceptance Criteria가 EARS 패턴(`THE`/`SHALL`/`WHEN`/`IF`/`THEN`)만 쓰는지 — 자유 서술형 문장이 섞여 있지 않은지
- ID 접두사가 올바른 문서에만 등장하는지 — `C-`(스펙 한정 제약)는 requirements.md, `G-`/`D-`(전역)는 `specs/_baseline.md`, `_Requirements: N.M_` 역참조는 tasks.md (SKILL.md Gotchas 참고)
- 서식 규칙 준수 — 한 문단이 파일 안에서 한 줄로 되어 있는지, 열거형 내용이 리스트 문법(`-`/`1.`)으로 되어 있는지

### 2. 완성도
- 템플릿(`assets/*.md`) 대비 누락된 섹션이 있는지
- 불완전한 AC (조건 또는 기대 결과가 빠진 경우)
- "적절히", "필요 시", "등" 같은 비한정 표현이 Glossary에 정의되지 않은 채 그대로 쓰이고 있는지

### 3. 일관성
- `specs/_baseline.md`가 있을 때 그 안의 `G-`/`D-` 항목과 이 스펙의 내용이 상충하지 않는지
- requirements.md ↔ design.md ↔ tasks.md 간 요구사항 번호·용어가 일관되는지
- Glossary에 정의된 용어가 본문에서 일관되게 쓰이는지

### 4. 테스트 가능성
- AC가 자동화된 테스트로 검증 가능한 수준으로 구체적인지
- 수치·조건·경계값이 명확한지
- jqwik property로 전환 가능한 형태인지 (모호하면 지적)

### 5. 범위 적정성
- 하나의 Requirement가 너무 많은 관심사를 포함하지 않는지 (단일 책임)
- requirements.md ↔ design.md ↔ tasks.md 간 범위가 일치하는지 — design/tasks가 요구사항에 없는 것을 다루거나, 요구사항에 있는 것을 빠뜨리고 있지 않은지

## requirements.md 추가 항목

### 6. What/How 분리
- 구현 방법(특정 라이브러리·클래스명, 빌드 도구의 설정 키워드·스코프 이름, 검증 방법의 세부사항 등)이 섞여 있지 않은지
- 판별법: 표현을 지우거나 다른 구현으로 바꿔도 문장이 말하는 관찰 가능한 결과가 그대로 유지되는가? 유지되면 How이니 지적한다 (SKILL.md "requirements.md 작성" 참고)

### 7. 경계 조건
- AC가 정상 케이스만 다루고 있진 않은지 — 예외/에러 케이스를 다루는 `IF ... THEN` 문장이 있는지

### 8. AC 중복
- 여러 Requirement에 걸쳐 실질적으로 동일한 조건이 반복되지 않는지 — 반복된다면 하나로 통합하거나 참조로 정리되어 있는지

## design.md 추가 항목

### 9. 설계 결정의 근거
- 자명하지 않은 컴포넌트 선택에 "왜 이렇게 했는가"가 있는지
- 반대로 모든 컴포넌트에 억지로 채워져 있어 정말 자명한 선택까지 근거를 늘어놓고 있진 않은지

### 10. Baseline Alignment
- `specs/_baseline.md`가 있고 이 스펙과 관련된 `G-`/`D-` 항목이 있을 때, Baseline Alignment 섹션에서 준수/이탈 여부를 명시하고 있는지
- 이탈이라면 사유가 있는지

### 11. Correctness Properties 추적성
- requirements.md의 AC들을 훑어 property로 표현 가능한 것이 있는지 검토했는지 — property가 없다면 "왜 example-based test로 충분한지" 한 줄이라도 있는지
- Property마다 `Validates: N.M`이 실제로 존재하는 요구사항 번호를 가리키는지
- Property 구현에 jqwik(`@Property` + `@ForAll`)을 명시했는지 — AutoParams(`@AutoSource`)로 대체되어 있지 않은지

### 12. 기술 실현 가능성
- 사용하는 API/프레임워크의 실제 시그니처·동작과 일치하는지
- 구현이 불가능하거나 과도한 우회가 필요한 설계가 없는지

## tasks.md 추가 항목

### 13. 요구사항 역추적
- 모든 리프 태스크에 `_Requirements: N.M, ..._`이 있고, 그 번호가 requirements.md에 실제로 존재하는지 (지어낸 번호가 없는지 — SKILL.md Gotchas 참고)

### 14. 완료 조건
- 모든 리프 태스크에 관찰 가능한 완료 조건(`**완료 조건:**`)이 명시되어 있는지
- 그 조건이 실행 후 참/거짓으로 확인 가능한지 — "적절히 동작한다"처럼 주관적인 서술로 되어 있진 않은지

### 15. TDD 구조
- 비즈니스 로직을 구현하는 태스크가 Red → Green → Refactor로 쪼개져 있는지
- Red 단계의 테스트가 실제 Acceptance Criteria나 Correctness Property에서 도출됐는지 (테스트를 먼저 상상해서 만든 것처럼 보이지 않는지)
- 순수 설정/빌드/인프라 태스크에는 이 구조가 강제되지 않았는지 확인 (강제되어 있으면 과도함으로 WARN)

### 16. Checkpoint / 문서 업데이트
- Step 사이마다 Checkpoint 태스크가 있는지
- 마지막 태스크가 "관련 문서 업데이트"인지 (생략되어 있으면 BLOCK)
- 체크박스 상태(`[ ]`/`[-]`/`[~]`/`[x]`)가 SKILL.md의 전환 순서(`[ ]`→`[-]`→`[~]`→`[x]`)를 벗어나지 않는지
