# Requirements Document: <spec-name>

## Introduction

<이 스펙이 다루는 배경과 목표를 2~4문장으로. 왜 지금 이 작업이 필요한지, 무엇을 완료 상태로 볼지.>

---

## Glossary

전역 용어집: [specs/_baseline.md](../_baseline.md) 참고 (있는 경우). 거기 이미 정의된 용어는 다시 적지 않는다.

<이 스펙에서만 쓰는 용어만. 일반적인 용어는 넣지 않는다. 표현이 모호할 수 있는 용어("빠른", "대용량" 등)는 여기서 구체적 기준으로 정의한다. 다른 스펙에서도 반복될 법한 공통 용어라면 승격 후보로 남겨둔다.>

| 용어 | 정의 |
|------|------|
| **<Term>** | <definition> |

---

## Requirements

### Requirement 1: <제목>

**User Story:** As a <역할>, I want <원하는 기능/능력>, so that <얻는 이익>.

#### Acceptance Criteria

1. THE <System> SHALL <조건 없이 항상 성립하는 동작>.
2. WHEN <이벤트/조건>, THE <System> SHALL <반응>.
3. IF <예외/에러 조건>, THEN THE <System> SHALL <대응>.

---

### Requirement 2: <제목>

**User Story:** As a <역할>, I want <원하는 기능/능력>, so that <얻는 이익>.

#### Acceptance Criteria

1. ...

---

<Requirement N개를 필요한 만큼 반복. 번호(N.M)는 design.md/tasks.md에서 역참조되는 유일한 앵커이므로 임의로 재배치하지 않는다.>

## Constraints

<이 스펙에서만 적용되는 범위 제한. "이렇게는 하지 마라"류의 하드 룰이되, 프로젝트 전역이 아니라 이 작업 한정인 것만. 전역 제약은 여기 적지 않고 `specs/_baseline.md`를 참고 링크로 남긴다. 이 스펙 한정 제약이 없으면 이 섹션 자체를 생략한다.>

프로젝트 전역 제약: [specs/_baseline.md](../_baseline.md) 참고 (있는 경우)

| ID | 제약 |
|----|------|
| C-1 | <이 스펙에서만 적용되는 제약> |
