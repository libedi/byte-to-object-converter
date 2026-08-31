# Project Baseline: byte-to-object-converter

feature spec들의 가드레일 역할을 하는 정본 문서다. **SDD(스펙 기반 개발)의 대상이 아니다** — 이 문서 자체에 대한 requirements.md/design.md/tasks.md 3단계 사이클을 만들지 않는다. 프로젝트 전역에 걸친 규칙·결정·용어만 담아 개별 feature spec이 반복해서 적지 않게 한다.

이 문서는 스펙 하나의 산출물이 아니라 계속 누적되는 문서다. 매 feature spec의 tasks.md 마지막 태스크("관련 문서 업데이트")에서 승격 기준(`references/spec-promotion-rules.md` 참고)에 해당하는 내용이 있는지 검토하고 여기에 반영한다. 모든 feature spec의 requirements.md/design.md는 이 문서를 링크로 참조하며, 여기 있는 내용을 자기 문서에 다시 옮겨 적지 않는다.

**ID 스킴**: `G-`는 Global Rules, `D-`는 Decision Log. feature spec 안에서만 쓰는 `C-`(스펙 한정 제약)와 섞지 않는다. 같은 결정이 강제 규칙과 그 근거를 둘 다 가질 때는 `G-`(규칙)와 `D-`(근거)를 짝으로 만들 수 있다(예: `G-2`/`D-2`처럼 번호를 맞추면 추적이 쉽다) — 규칙만 있고 별도 설명이 필요 없으면 `G-`만 있어도 된다.

## Overview

`byte-to-object-converter`(v2.0.0)는 byte array와 Object 사이의 양방향 변환을 제공하는 Java 라이브러리다. 레거시 시스템의 byte array 데이터 전문을 다루기 쉽게 해주며, `io.github.libedi` groupId로 Maven Central에 배포되는 공개 라이브러리다.

---

## Global Rules

프로젝트 전체에 항상 적용되는 규칙. 특정 기능이 아니라 "이 코드베이스를 다루는 모든 작업"에 적용되는 것만 담는다.

| ID | 규칙 |
|----|------|
| G-1 | 테스트 전용 의존성은 `testImplementation`/`testCompileOnly`/`testAnnotationProcessor`/`testRuntimeOnly` 등 test 스코프로만 선언하고 런타임 아티팩트에 노출하지 않는다. |
| G-2 | 공개 API(public/protected 시그니처)에 노출되지 않는 런타임 의존성은 `api`가 아니라 `implementation`으로 선언한다 — 추가 전에 실제로 노출되는지 확인한다. |
| G-3 | 서명 키·Central Portal 토큰 등 자격증명은 저장소 안 어떤 파일에도(템플릿 포함) 두지 않는다 — 프로퍼티의 존재 여부로만 필수 여부를 판단하고, 실제 값은 개발자 개인의 `~/.gradle/gradle.properties` 또는 CI 환경 변수에서만 읽는다. |
| G-4 | 의존성 좌표·버전은 `build.gradle.kts`에 직접 쓰지 않고 `gradle/libs.versions.toml` Version Catalog에 선언한다. |
| G-5 | Gradle 빌드 스크립트는 Kotlin DSL(`build.gradle.kts`, `settings.gradle.kts`)을 쓴다 — Groovy DSL로 새로 작성하지 않는다. (원본: `docs/backlog.md` 2026-08-22 세션 로그) |
| G-6 | byte[] → Object 변환에서 필드 값 파싱 실패는 항상 `TypeConversionException` 계층의 예외로 던지고(전용 하위 타입이 있으면 그것을, 없으면 `TypeConversionException` 자체를), 파싱과 무관한 리플렉션 접근 오류는 `FieldAccessException`으로 남긴다. |
| G-7 | Correctness Property는 jqwik `@Property`/`@ForAll`로 구현한다 — AutoParams(`@AutoSource`)는 shrinking을 지원하지 않으므로 대체하지 않는다. |

---

## Decision Log

프로젝트 전체에 영향을 끼치는 설계 결정만. 상세 근거는 원본 스펙에 있으므로 여기는 한 줄 요약과 링크만 남긴다.

| ID | 결정 | 근거 (요약) | 영향 범위 | 원본 |
|----|------|-------------|-----------|------|
| D-1 | Central Portal 배포 플러그인으로 `com.gradleup.nmcp`(`publishingType = USER_MANAGED`)를 채택 | OSSRH sunset 이후 Sonatype 공식 Gradle 플러그인이 없는 상태에서, Central Portal REST API를 직접 지원하고 SNAPSHOT 배포도 되는 커뮤니티 플러그인 중 활발히 유지보수되는 쪽을 선택 | 배포(publish) 파이프라인 전체 | [specs/01-dependency-plugin-modernization/design.md](01-dependency-plugin-modernization/design.md) Component 6 |
| D-6 | byte[] → Object 변환의 필드 값 파싱 실패는 `TypeConversionException` 계층 예외로, 파싱과 무관한 리플렉션 접근 오류는 `FieldAccessException`으로 구분해서 던진다 | 여러 클래스에 걸친 예외 처리 체계의 구조적 패턴이며, 향후 새 타입 지원을 추가하는 스펙(예: `BigDecimal` 지원)이 암묵적으로 따라야 할 전제이기 때문 | `ConversionHelper`의 값 파싱/예외 변환 로직 전체, 향후 새 타입 지원 스펙 | [specs/02-exception-hierarchy-consistency/design.md](02-exception-hierarchy-consistency/design.md) Component 1~5 |
| D-7 | Correctness Property 구현 표준 도구로 jqwik을 채택 | AutoParams(`@AutoSource`)는 무작위 예시 데이터를 생성할 뿐 shrinking을 지원하지 않아 진정한 property 검증에는 부족하기 때문 | 테스트 전략 전반(향후 Correctness Property를 다루는 모든 feature spec) | [specs/02-exception-hierarchy-consistency/design.md](02-exception-hierarchy-consistency/design.md) Correctness Properties |

feature spec의 design.md는 자신과 관련된 항목을 인용하며 준수/이탈 여부를 명시해야 한다.

---

## Glossary

2개 이상의 feature spec에서 반복 등장하는 용어만 담는다.

| 용어 | 정의 |
|------|------|
| **최신 안정 버전** | 각 스펙의 Acceptance Criteria에 명시된 하한 버전(그 스펙 작성일 기준 Maven Central 조회 결과) 이상이면서, pre-release/milestone(`-M1`, `-RC1` 등)이 아닌 가장 최근 버전. 실제 구현 시점에 더 새 버전이 나와 있으면 그 버전을 우선 시도한다. |
