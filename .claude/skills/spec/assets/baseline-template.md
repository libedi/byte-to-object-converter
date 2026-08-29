# Project Baseline: <project name>

feature spec들의 가드레일 역할을 하는 정본 문서다. **SDD(스펙 기반 개발)의 대상이 아니다** — 이 문서 자체에 대한 requirements.md/design.md/tasks.md 3단계 사이클을 만들지 않는다. 프로젝트 전역에 걸친 규칙·결정·용어만 담아 개별 feature spec이 반복해서 적지 않게 한다.

이 문서는 스펙 하나의 산출물이 아니라 계속 누적되는 문서다. 매 feature spec의 tasks.md 마지막 태스크("관련 문서 업데이트")에서 승격 기준(`references/spec-promotion-rules.md` 참고)에 해당하는 내용이 있는지 검토하고 여기에 반영한다. 모든 feature spec의 requirements.md/design.md는 이 문서를 링크로 참조하며, 여기 있는 내용을 자기 문서에 다시 옮겨 적지 않는다.

**ID 스킴**: `G-`는 Global Rules, `D-`는 Decision Log. feature spec 안에서만 쓰는 `C-`(스펙 한정 제약)와 섞지 않는다. 같은 결정이 강제 규칙과 그 근거를 둘 다 가질 때는 `G-`(규칙)와 `D-`(근거)를 짝으로 만들 수 있다(예: `G-2`/`D-2`처럼 번호를 맞추면 추적이 쉽다) — 규칙만 있고 별도 설명이 필요 없으면 `G-`만 있어도 된다.

## Overview

<이 프로젝트가 무엇인지 1~2문장. CLAUDE.md 등 기존 설명이 있으면 그걸 압축해서 쓴다 — 새로 지어내지 않는다.>

---

## Global Rules

<프로젝트 전체에 항상 적용되는 규칙. 특정 기능이 아니라 "이 코드베이스를 다루는 모든 작업"에 적용되는 것만. "이렇게는 하지 마라"는 하드 룰이든, "항상 이렇게 동작한다"는 EARS 문장이든 상관없다 — 둘을 억지로 구분하지 않는다. 검증 가능한 형태로 쓴다.>

| ID | 규칙 |
|----|------|
| G-1 | <constraint 또는 EARS 문장> |

---

## Decision Log

<프로젝트 전체에 영향을 끼치는 설계 결정만. 특정 스펙 하나의 구현 디테일은 그 스펙의 design.md에만 남기고 여기로 옮기지 않는다. 상세 근거는 원본 스펙에 있으므로 여기는 한 줄 요약과 링크만 남긴다 — 중복해서 다시 설명하지 않는다.>

| ID | 결정 | 근거 (요약) | 영향 범위 | 원본 |
|----|------|-------------|-----------|------|
| D-1 | <무엇을 결정했는가> | <한 줄 요약> | <어떤 컴포넌트/영역에 적용되는가> | [specs/\<spec-name\>/design.md](<spec-name>/design.md) |

feature spec의 design.md는 자신과 관련된 항목을 인용하며 준수/이탈 여부를 명시해야 한다 (`assets/design-template.md`의 "전역 Baseline과의 관계" 참고).

---

## Glossary

<2개 이상의 feature spec에서 반복 등장하는 용어만. 스펙 하나에서만 쓰는 용어는 그 스펙 자체의 Glossary에 둔다.>

| 용어 | 정의 |
|------|------|
| **<Term>** | <definition> |
