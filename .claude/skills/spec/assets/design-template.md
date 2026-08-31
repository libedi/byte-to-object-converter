# Design Document: <spec-name>

## Overview

<이 설계가 무엇을, 왜 하는지 2~4문장. requirements.md의 어떤 Requirement들을 어떤 순서로 다룰지 요약.>

---

## Architecture

<영향받는 디렉토리/파일, 전체적인 구조 변화를 설명. 필요하면 트리로:>

```
project/
├── path/to/changed-file.ext   ← <한 줄 설명>
└── path/to/new-file.ext       ← 신규 생성
```

<필요할 때만: 컴포넌트 관계나 흐름이 글로 설명하기 어려우면 mermaid 다이어그램 추가. 장식용으로 넣지 않는다.>

---

## Baseline Alignment

<`specs/_baseline.md`가 있고 이 스펙과 관련된 `G-`(전역 규칙)/`D-`(결정 기록) 항목이 있을 때만 작성한다. 관련 없는 항목까지 전부 나열하지 않는다. baseline이 없거나 관련 항목이 없으면 이 섹션 자체를 생략한다.>

| Baseline 항목 | 준수 여부 | 비고 |
|---|---|---|
| <G-1 또는 D-1> | 준수 / 이탈 | <이탈이면 사유 필수, 준수면 비워둬도 됨> |

---

## Components and Interfaces

### Component 1: <이름>

<이 컴포넌트가 담당하는 역할. 변경이 있다면 변경 전/후 비교.>

```<language>
// 변경 전
...
```

```<language>
// 변경 후
...
```

<선택 — 이 컴포넌트의 선택이 자명하지 않을 때만 한두 줄로. 모든 컴포넌트에 억지로 채우지 않는다.>
**왜 이렇게 했는가:** <다른 옵션과 비교했을 때 이 선택의 근거>

<반복: Component N개>

---

## Data Models

<최종 목표 상태의 설정/스키마/클래스 구조 등. 코드 스니펫이나 표로.>

---

## Error Handling

<시나리오별 대응 전략. requirements.md의 IF...THEN 조건들과 대응되는 경우가 많다.>

| 시나리오 | 대응 전략 |
|---|---|
| <실패/예외 상황> | <어떻게 처리하는가> |

---

## Correctness Properties

requirements.md의 각 Acceptance Criteria를 훑어 "임의의 입력에 대해 항상 성립해야 하는 보편적 속성"으로 표현 가능한 것이 있는지 검토한 결과. 이 검토는 매번 하되, 의미 있는 property가 없으면 아래처럼 그 판단 근거만 남기고 넘어간다.

<property가 없는 경우 — 예:>
> 이 스펙의 Acceptance Criteria는 대부분 정적 설정 검사/외부 서비스 연동이라 property로 표현해도 얻는 게 없다. 전부 example-based test로 충분하다.

<property가 있는 경우 — Property마다 반복:>

### Property 1: <이름>

<For any 형태로 서술: 임의의 입력에 대해 어떤 속성이 항상 성립해야 하는가.>

**Validates: Requirements N.M**

**구현:** jqwik (`@Property` + `@ForAll`). AutoParams(`@AutoSource`)는 shrinking을 지원하지 않아 이 용도로 쓰지 않는다.

---

## Testing Strategy

<단계별 검증 명령어와 확인 사항.>

```bash
# 검증 명령어
./gradlew ...
```

주요 확인 사항:
- <what to verify>

비즈니스 로직 구현은 tasks.md에서 Red → Green → Refactor 순서로 진행한다 (Red 단계 테스트는 위 Acceptance Criteria/Correctness Property에서 도출).
