# Implementation Plan: exception-hierarchy-consistency

## Overview

총 6개 Step으로 진행한다. Step 1은 jqwik 의존성 추가(인프라, design.md Correctness Properties 절 참고), Step 2~4는 design.md Component 1(분기 재구성)을 각 타입 범주(숫자 Wrapper, Enum, Month/`java.time`)에 걸쳐 점진적으로 적용하며 Component 2~5(`parsePrimitiveOrWrapper`/`parseEnum`/`parseMonth`/`parseDateTime`)를 Red→Green→Refactor로 구현한다. Step 5는 Component 1의 최종 구조(축소된 `throws` 절)를 확정하고 Requirement 4~5의 기존 동작 회귀 여부를 검증한다. Step 6은 design.md Correctness Properties(Property 1~3)를 jqwik으로 구현한다. 마지막 태스크는 Component 6~8(Javadoc/README/CLAUDE.md 재동기화, Requirement 6)과 `specs/_baseline.md` 승격을 함께 다룬다.

## Checkbox States

작성 시점의 모든 태스크는 `[ ]`다 (SKILL.md 참고). `[ ]`→작성됨(실행 범위 아님), `[-]`→대기(실행 시작 시 이번 범위 전체를 일괄 마킹), `[~]`→진행 중(시작하는 즉시 개별 갱신), `[x]`→완료(검증까지 마친 뒤 개별 갱신). 전환은 항상 `[ ]`→`[-]`(일괄)→`[~]`→`[x]`(개별) 순서다.

## Definition of Done

모든 리프 태스크는 `_Requirements:` 줄 바로 위에 `**완료 조건:**`으로 관찰 가능한 완료 기준(Definition of Done)을 명시한다 — 실행 후 참/거짓으로 확인 가능한 조건이어야 한다(특정 명령의 성공 종료, 특정 테스트의 통과, 특정 파일/산출물의 존재 등). "적절히 동작한다", "잘 반영된다" 같은 주관적 서술은 완료 조건이 아니다. 이 조건은 그 태스크를 `[~]`→`[x]`로 갱신하기 전에 실제로 확인해야 하는 것이다(SKILL.md "실행 중 상태 갱신" 참고).

---

## Tasks

- [x] 1. Step 1: jqwik 의존성 추가

  - [x] 1.1 `gradle/libs.versions.toml`/`build.gradle.kts`에 jqwik 추가
    - `gradle/libs.versions.toml`의 `[versions]`에 `jqwik` 버전(Maven Central에서 확인한 최신 안정 버전, pre-release 제외)을, `[libraries]`에 `jqwik = { module = "net.jqwik:jqwik", version.ref = "jqwik" }`를 추가한다.
    - `build.gradle.kts`의 `dependencies` 블록에 `testImplementation(libs.jqwik)`를 추가한다(JUnit 5 플랫폼 엔진 연동은 `net.jqwik:jqwik` 단일 아티팩트에 포함되어 있고, 이미 선언된 `testRuntimeOnly(libs.junit.platform.launcher)`와 `tasks.test { useJUnitPlatform() }`로 실행된다).
    - design.md Baseline Alignment(G-1/G-4/G-5)에 따라 `implementation`이 아닌 `testImplementation`으로만 선언하고, 좌표·버전은 `build.gradle.kts`에 직접 쓰지 않는다.
    - **완료 조건:** `./gradlew dependencies --configuration testImplementation`의 출력에 `net.jqwik:jqwik`가 나타나고 성공 종료한다.
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 3.1, 3.2_

  - [x] 1.2 Step 1 검증
    - jqwik이 실제로 JUnit 5 플랫폼에서 실행되는지 확인하기 위한 최소 스모크 테스트(`@Property` 하나, 예: `@ForAll int n` → `n == n`)를 임시로 작성해 실행한 뒤 확인 후 제거한다(정식 Property 테스트는 Step 6에서 새로 작성한다).
    - `./gradlew compileTestJava`와 `./gradlew test`를 실행한다.
    - **완료 조건:** `./gradlew compileTestJava`와 `./gradlew test`가 모두 성공 종료하고, jqwik `@Property` 테스트가 최소 1건 실행된 것이 테스트 리포트(`build/test-results` 또는 콘솔 출력)에서 확인된다.
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 3.1, 3.2_

- [x] 2. Checkpoint — Step 1 완료 확인
  - jqwik 의존성이 정상적으로 추가되고 스모크 테스트가 통과했는지 확인한다. 문제가 있으면 작업을 멈추고 사용자에게 질문한다.

- [x] 3. Step 2: Requirement 1 — 숫자 Wrapper 타입 파싱 실패 → `NumberParsingException`

  - [x] 3.1 숫자 Wrapper 파싱 예외 변환 (`parsePrimitiveOrWrapper`, design.md Component 1 최초 분기 도입 + Component 2)
    - **Red:** Requirement 1.1("파싱 실패 시 `NumberParsingException`을 던진다")과 1.2(`getCause()`가 `InvocationTargetException`이 아닌 원본 `NumberFormatException`으로 보존됨)를 근거로, 숫자 Wrapper 필드(예: `Integer`)에 그 타입으로 파싱 불가능한 문자열이 담긴 byte 데이터를 `convert()`할 때 `NumberParsingException`이 던져지고 `getCause()`가 `NumberFormatException`인지 검증하는 실패 테스트를 작성한다. 현재 구현에서는 이 경로가 `FieldAccessException`을 던지므로 이 테스트는 실패해야 한다.
    - **Green:** design.md Component 2의 `parsePrimitiveOrWrapper(fieldType, value)` private 메서드를 `ConversionHelper`에 추가하고, `invokeSetValueByFieldType`의 숫자/Boolean Wrapper 분기(`!isVoid && isPrimitiveOrWrapper`)가 이 메서드를 호출하도록 바꿔 위 테스트를 통과시킨다. 이 시점에는 아직 `(!isVoid && isPrimitiveOrWrapper) || fieldType.isEnum()` 조건이 Step 3(태스크 4.1)에서 분리되기 전이므로, Enum 필드 파싱 실패도 일시적으로 `parsePrimitiveOrWrapper`를 거쳐 `NumberParsingException`이 되는 과도기 상태가 된다(Step 3 완료 시 `TypeConversionException`으로 정정된다) — 아래 Notes에도 동일하게 명시한다.
    - **Refactor:** 테스트가 계속 그린인 상태를 유지하며 구조를 정리한다.
    - **완료 조건:** Green에서 작성한 테스트가 Refactor 이후에도 계속 통과한다.
    - _Requirements: 1.1, 1.2_

  - [x] 3.2 Step 2 검증
    - `./gradlew test --tests "io.github.libedi.converter.ByteToObjectConverterTest"`를 실행해 기존 정상 변환 테스트(숫자 Wrapper 필드에 유효한 값이 담긴 케이스 포함)가 회귀 없이 계속 통과하는지 확인한다.
    - **완료 조건:** 위 명령이 성공 종료하고, 3.1에서 추가한 테스트와 기존 정상 변환 테스트가 모두 통과한다.
    - _Requirements: 1.3_

- [x] 4. Step 3: Requirement 3 — Enum 타입 파싱 실패 → `TypeConversionException`

  - [x] 4.1 Enum 파싱 예외 변환 (`parseEnum`, design.md Component 3)
    - **Red:** Requirement 3.1("하위 타입이 아닌 `TypeConversionException` 인스턴스를 던진다")과 3.2(`getCause()`가 `InvocationTargetException`이 아닌 원본 `IllegalArgumentException`으로 보존됨)를 근거로, Enum 타입 필드(예: 기존 `Week`)에 유효한 상수명이 아닌 문자열이 담긴 byte 데이터를 `convert()`할 때 던져진 예외의 `getClass()`가 정확히 `TypeConversionException.class`이고 `getCause()`가 `IllegalArgumentException`인지 검증하는 실패 테스트를 작성한다. 현재 구현에서는 이 경로가 `FieldAccessException`을 던지므로 이 테스트는 실패해야 한다.
    - **Green:** design.md Component 3의 `parseEnum(fieldType, value)` private 메서드를 추가하고, `invokeSetValueByFieldType`에서 Enum 분기를 숫자/Boolean Wrapper 분기와 분리해(`fieldType.isEnum()`을 독립된 `if`로) 이 메서드를 호출하도록 바꿔 위 테스트를 통과시킨다.
    - **Refactor:** 테스트가 계속 그린인 상태를 유지하며 구조를 정리한다.
    - **완료 조건:** Green에서 작성한 테스트가 Refactor 이후에도 계속 통과한다.
    - _Requirements: 3.1, 3.2_

  - [x] 4.2 Step 3 검증
    - `./gradlew test --tests "io.github.libedi.converter.ByteToObjectConverterTest"`를 실행해 기존 정상 변환 테스트(Enum 필드에 유효한 상수명이 담긴 케이스 포함)가 회귀 없이 계속 통과하는지 확인한다.
    - **완료 조건:** 위 명령이 성공 종료하고, 4.1에서 추가한 테스트와 기존 정상 변환 테스트가 모두 통과한다.
    - _Requirements: 3.3_

- [x] 5. Step 4: Requirement 2 — `java.time` date-time 타입(`Month` 포함) 파싱 실패 → `DateParsingException`

  - [x] 5.1 `Month` 파싱 예외 변환 (`parseMonth`, design.md Component 4)
    - **Red:** Requirement 2.2(`Month` 값이 정수로 파싱되지 않거나 1~12 범위를 벗어나면 `DateParsingException`)와 2.3(`getCause()`가 `InvocationTargetException`이 아닌 원본 예외로 보존됨)을 근거로, `Month` 필드에 (a) 정수로 파싱되지 않는 문자열, (b) 정수이지만 1~12 범위를 벗어난 값(예: `"13"`)이 담긴 byte 데이터를 `convert()`할 때 각각 `DateParsingException`이 던져지고 `getCause()`가 (a) `NumberFormatException`, (b) `DateTimeException`인지 검증하는 실패 테스트를 작성한다. 현재 구현에서는 (a) 정수 파싱 실패(예: `"abc"`)는 감싸지지 않은 raw `NumberFormatException`이 그대로 올라가고, (b) 범위 초과(예: `"13"`)는 `MethodUtils.invokeStaticMethod`가 `DateTimeException`을 `InvocationTargetException`으로 감싸 `FieldAccessException`이 되므로(design.md Component 1 설명 참고), 두 테스트 모두 실패해야 한다.
    - **Green:** design.md Component 4의 `parseMonth(value)` private 메서드를 추가하고(`Integer.parseInt`와 `Month.of(int)`를 각각 별도 `try`로 감싸 `NumberFormatException`/`DateTimeException`을 `DateParsingException`으로 변환), `invokeSetValueByFieldType`의 `Month` 분기가 이 메서드를 호출하도록 바꿔 위 테스트를 통과시킨다.
    - **Refactor:** 테스트가 계속 그린인 상태를 유지하며 구조를 정리한다.
    - **완료 조건:** Green에서 작성한 테스트가 Refactor 이후에도 계속 통과한다.
    - _Requirements: 2.2, 2.3_

  - [x] 5.2 `java.time` date-time 파싱 예외 변환 (`parseDateTime`, design.md Component 5)
    - **Red:** Requirement 2.1(`format` 패턴으로 파싱되지 않으면 `DateParsingException`)과 2.3(`getCause()`가 `InvocationTargetException`이 아닌 원본 예외로 보존됨)을 근거로, `format`이 지정된 `java.time` date-time 타입 필드(예: `LocalDate`, `format = "yyyyMMdd"`)에 그 패턴으로 파싱 불가능한 문자열이 담긴 byte 데이터를 `convert()`할 때 `DateParsingException`이 던져지고 `getCause()`가 `DateTimeParseException`인지 검증하는 실패 테스트를 작성한다. 현재 구현에서는 이 경로가 `FieldAccessException`을 던지므로 이 테스트는 실패해야 한다.
    - **Green:** design.md Component 5의 `parseDateTime(field, fieldType, value)` private 메서드를 추가하고(`MissingFormatException`은 기존과 동일하게 그대로 전파), `invokeSetValueByFieldType`의 `isJavaTimePackageClass` 분기가 이 메서드를 호출하도록 바꿔 위 테스트를 통과시킨다.
    - **Refactor:** 테스트가 계속 그린인 상태를 유지하며 구조를 정리한다.
    - **완료 조건:** Green에서 작성한 테스트가 Refactor 이후에도 계속 통과한다.
    - _Requirements: 2.1, 2.3_

  - [x] 5.3 Step 4 검증
    - `./gradlew test --tests "io.github.libedi.converter.ByteToObjectConverterTest"`를 실행해 기존 정상 변환 테스트(`Month`·`java.time` 날짜-시간 필드에 유효한 값이 담긴 케이스 포함)가 회귀 없이 계속 통과하는지 확인한다.
    - **완료 조건:** 위 명령이 성공 종료하고, 5.1·5.2에서 추가한 테스트와 기존 정상 변환 테스트가 모두 통과한다.
    - _Requirements: 2.4_

- [x] 6. Checkpoint — 3종 파싱 예외(숫자/Enum/`Month`·`java.time`) 구현 완료 확인
  - Step 2~4는 동일한 패턴(Red→Green→Refactor로 파싱 메서드 하나를 도입하고 "Step N 검증" 태스크로 회귀 여부를 그때그때 확인하는 구조)이 반복되므로, 각 Step마다 별도 Checkpoint를 두지 않고 세 Step을 마친 뒤 한 번에 사용자 확인을 받는다 — 회귀 자체는 이미 3.2/4.2/5.3의 Step별 검증에서 매번 확인된다.
  - Step 2~4에서 각 타입 범주가 의도한 예외 타입과 cause로 실패하고 정상 케이스는 회귀 없이 통과하는지 확인한다. 문제가 있으면 작업을 멈추고 사용자에게 질문한다.

- [x] 7. Step 5: Component 1 구조 정리 및 Requirement 4~5 회귀 검증

  - [x] 7.1 `invokeSetValueByFieldType`/`extractData` 시그니처 정리 (design.md Component 1 최종본)
    - `ConversionHelper#invokeSetValueByFieldType`의 `throws` 절에서 `NumberFormatException`/`InvocationTargetException`을 제거하고 `NoSuchMethodException, IllegalAccessException`만 남긴다(두 예외는 Step 2~4에서 각 파싱 메서드가 지역적으로 잡아 도메인 예외로 변환하므로 더 이상 이 메서드 밖으로 나가지 않는다).
    - `extractData`의 `throws` 절도 `IllegalAccessException, IOException, NumberFormatException, NoSuchMethodException, InvocationTargetException`에서 `IllegalAccessException, IOException, NoSuchMethodException`으로 정리한다.
    - **완료 조건:** `ConversionHelper.java`의 두 메서드 시그니처가 design.md Component 1 "변경 후" 코드와 일치하고, `./gradlew compileJava`가 성공 종료한다.
    - _Requirements: 4.1, 4.2_

  - [x] 7.2 Requirement 4 회귀 테스트 — 파싱과 무관한 리플렉션 오류는 `FieldAccessException` 유지
    - Requirement 4.1을 근거로, `valueOf(String)` 정적 메서드 자체가 없는 타입(`Character` 필드)에 값을 설정하려 할 때 `convert()`가 `FieldAccessException`을 던지는지 검증하는 테스트를 `ByteToObjectConverterTest.java`(또는 신규 테스트 클래스)에 추가한다. `lengthField`/`countField`로 지정된 필드가 대상 클래스에 존재하지 않는 시나리오는 requirements.md C-5에 따라 이 스펙 범위 밖(`docs/backlog.md` #BL-06으로 별도 기록됨)이므로 이 태스크에서 다루지 않는다.
    - **완료 조건:** 위 테스트가 `FieldAccessException`이 던져짐을 검증하며 통과한다.
    - _Requirements: 4.1_

  - [x] 7.3 Requirement 5 회귀 테스트 — 참조 타입 필드의 빈 값은 예외 없이 `null` 유지
    - Requirement 5.1을 근거로, 숫자 Wrapper·`Month`·Enum·`java.time` 날짜-시간 타입 각각에 대해 trim 후 빈 문자열이 되는 byte 데이터가 입력됐을 때 `convert()`가 예외를 던지지 않고 해당 필드를 `null`로 남기는지 검증하는 테스트를 추가한다(이미 `ByteToObjectConverterTest`의 `TestObject`에 각 타입 필드가 있으면 이를 활용한다).
    - **완료 조건:** 위 테스트가 예외 없이 완료되고 각 필드가 `null`임을 검증하며 통과한다.
    - _Requirements: 5.1_

  - [x] 7.4 Step 5 검증
    - `./gradlew test`를 실행한다.
    - **완료 조건:** `./gradlew test`가 성공 종료하고, 7.2·7.3에서 추가한 테스트를 포함한 전체 테스트가 통과한다.
    - _Requirements: 4.1, 4.2, 5.1_

- [x] 8. Checkpoint — Step 5(구조 정리·회귀 검증) 완료 확인
  - `throws` 절 축소가 design.md와 일치하고 Requirement 4~5의 기존 동작이 깨지지 않았는지 확인한다. 문제가 있으면 작업을 멈추고 사용자에게 질문한다.

- [x] 9. Step 6: Correctness Properties 구현 (jqwik)

  - [x] 9.1 Property 1 — 숫자 Wrapper 타입 파싱 실패는 항상 `NumberParsingException`
    - design.md Correctness Properties의 Property 1 구현 방법에 따라, 숫자 Wrapper 6종(`Byte`/`Short`/`Integer`/`Long`/`Float`/`Double`) 각각 필드를 정확히 하나만 가진 픽스처 클래스 6종을 두고, `@Provide`로 그중 하나를 고르는 provider와 해당 필드 길이(`N`) 이하의 ASCII 문자열 중 "trim 후 비어있지 않음"과 "해당 타입으로 파싱 불가능함"을 필터로 강제한 provider를 조합한 jqwik `@Property` + `@ForAll` 테스트를 작성한다. 생성된 문자열은 `StringUtils.rightPad`로 정확히 `N`자로 맞춰 `InputStream`을 구성한다(절단은 쓰지 않는다).
    - **Property 1: 숫자 Wrapper 타입 파싱 실패는 항상 `NumberParsingException`이다** — 파싱 불가능한 임의의 문자열이 입력되면 항상 `NumberParsingException`이 던져지고 `getCause()`가 `InvocationTargetException`이 아닌 원본 `NumberFormatException`이다.
    - **Validates: Requirements 1.1, 1.2**
    - **완료 조건:** 이 jqwik property 테스트가 jqwik 기본 실행 횟수(기본 `tries` 설정)만큼 실패 없이 통과한다.
    - _Requirements: 1.1, 1.2_

  - [x] 9.2 Property 2 — `java.time` date-time 타입(`Month` 포함) 파싱 실패는 항상 `DateParsingException`
    - design.md Correctness Properties의 Property 2 구현 방법에 따라, `LocalDate`(`format = "yyyyMMdd"`, 8바이트) 필드에는 길이 8자 이하 ASCII 문자열 중 "trim 후 비어있지 않음"과 "`yyyyMMdd` 패턴으로 파싱 불가능함"을 필터로 강제한 값을, `Month` 필드(2바이트)에는 (a) 길이 2자 이하 ASCII 문자열 중 "trim 후 비어있지 않음"과 "숫자로 파싱 불가능함"을 필터로 강제한 값과 (b) 2바이트로 표현 가능하면서 1~12를 벗어나는 정수(`13`~`99`)를 각각 `@ForAll`로 생성해 검증하는 jqwik `@Property` 테스트를 작성한다.
    - **Property 2: `java.time` date-time 타입(`Month` 포함) 파싱 실패는 항상 `DateParsingException`이다** — 파싱 불가능한 임의의 문자열/월 값이 입력되면 항상 `DateParsingException`이 던져지고 `getCause()`가 `InvocationTargetException`이 아닌 원본 예외(`DateTimeParseException`/`NumberFormatException`/`DateTimeException`)다.
    - **Validates: Requirements 2.1, 2.2, 2.3**
    - **완료 조건:** 이 jqwik property 테스트가 jqwik 기본 실행 횟수만큼 실패 없이 통과한다.
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 9.3 Property 3 — Enum 타입 파싱 실패는 항상 하위 타입이 아닌 `TypeConversionException`
    - design.md Correctness Properties의 Property 3 구현 방법에 따라, 테스트용 Enum(기존 `Week`)의 상수명 집합에 속하지 않는 임의의 문자열을 생성해 검증하는 jqwik `@Property` 테스트를 작성한다.
    - **Property 3: Enum 타입 파싱 실패는 항상 하위 타입이 아닌 `TypeConversionException`이다** — 유효하지 않은 상수명이 입력되면 항상 `getClass() == TypeConversionException.class`인 예외가 던져지고 `getCause()`가 `InvocationTargetException`이 아닌 원본 `IllegalArgumentException`이다.
    - **Validates: Requirements 3.1, 3.2**
    - **완료 조건:** 이 jqwik property 테스트가 jqwik 기본 실행 횟수만큼 실패 없이 통과한다.
    - _Requirements: 3.1, 3.2_

  - [x] 9.4 Step 6 검증
    - `./gradlew test`를 실행한다.
    - **완료 조건:** `./gradlew test`가 성공 종료하고, 9.1~9.3의 jqwik property 테스트가 모두 통과한다.
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 3.1, 3.2_

- [x] 10. Final Checkpoint — 전체 통합 검증
  - `./gradlew clean build`가 성공 종료하는지 확인한다(컴파일, 전체 테스트, javadoc 포함).
  - Step 2~4의 example-based 테스트와 Step 6의 jqwik property 테스트, Step 5의 회귀 테스트가 모두 하나의 빌드에서 함께 통과하는지 확인한다.
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 5.1_
  - 모든 검증이 통과하면 다음 태스크로 진행한다. 문제가 있으면 작업을 멈추고 사용자에게 질문한다.

- [x] 11. 관련 문서 업데이트 (모든 스펙에 필수 — 생략하지 않는다)

  - Requirement 6(문서를 실제 동작과 재동기화)에 해당하는 design.md Component 6~8과, `references/spec-promotion-rules.md` 기준으로 판단한 `specs/_baseline.md` 승격 대상을 함께 다룬다.

  - [x] 11.1 `NumberParsingException.java` Javadoc 재동기화 (design.md Component 6)
    - 발생 상황 목록에서 `<li>공백을 포함한 문자열인 경우</li>` 불릿을 제거한다(Requirement 5의 blank→null 처리로 인해 trim 후 빈 문자열은 이 예외를 발생시키지 않는다). 나머지 불릿(숫자 아닌 문자, 범위 초과, 잘못된 부동소수점 형식)은 유지한다.
    - **완료 조건:** `NumberParsingException.java`의 Javadoc에 공백 문자열을 발생 조건으로 서술한 불릿이 없고, `./gradlew javadoc`이 성공 종료한다.
    - _Requirements: 6.1_

  - [x] 11.2 `DateParsingException.java` Javadoc 재동기화 (design.md Component 6)
    - 발생 상황 목록과 예시 코드 블록에 `Month` 시나리오(정수 변환 실패, 1~12 범위 초과)를 추가한다.
    - **완료 조건:** `DateParsingException.java`의 Javadoc 발생 상황 목록에 `Month` 관련 항목이 포함되어 있고, 빈 문자열/공백을 발생 조건으로 서술한 부분이 없으며, `./gradlew javadoc`이 성공 종료한다.
    - _Requirements: 6.1_

  - [x] 11.3 `TypeConversionException.java` Javadoc 재동기화 (design.md Component 6)
    - 예시 코드 블록에 Enum 변환 실패 예시(`@ConvertData` 필드가 Enum 상수명이 아닌 값을 받는 경우 → `TypeConversionException`)를 추가한다.
    - **완료 조건:** `TypeConversionException.java`의 Javadoc 예시 코드 블록에 Enum 변환 실패 예시가 포함되어 있고, 빈 문자열/공백을 발생 조건으로 서술한 부분이 없으며, `./gradlew javadoc`이 성공 종료한다.
    - _Requirements: 6.1_

  - [x] 11.4 `README.md` 예외 처리 섹션 갱신 (design.md Component 7)
    - `TypeConversionException` 불릿 아래에 `NumberParsingException`/`DateParsingException`을 각각 이름과 발생 조건(design.md Component 7의 예시 마크업, 4칸 들여쓰기)으로 추가한다.
    - **완료 조건:** `README.md`에 `NumberParsingException`/`DateParsingException`이 이름으로 등장하고 각각의 발생 조건이 서술되어 있다.
    - _Requirements: 6.2_

  - [x] 11.5 `README_kr.md` 예외 처리 섹션 갱신 (design.md Component 7)
    - `README.md`와 동일한 구조를 한글 서술로 반영한다.
    - **완료 조건:** `README_kr.md`에 `NumberParsingException`/`DateParsingException`이 이름으로 등장하고 각각의 발생 조건이 한글로 서술되어 있다.
    - _Requirements: 6.2_

  - [x] 11.6 `CLAUDE.md` 예외 계층 설명 보강 (design.md Component 8)
    - 예외 계층 트리의 `TypeConversionException`/`DateParsingException`/`NumberParsingException` 항목 설명에, `TypeConversionException`이 Enum 변환 실패 시 직접(하위 타입 없이) 던져진다는 점과 `Month`가 `DateParsingException`에 포함된다는 점을 반영한다.
    - **완료 조건:** `CLAUDE.md`의 예외 계층 트리 설명에 Enum 직접 매핑과 `Month`→`DateParsingException` 매핑이 명시되어 있다.
    - _Requirements: 6.3_

  - [x] 11.7 `specs/_baseline.md`에 G-6/D-6 추가 — 파싱 실패는 `TypeConversionException` 계층 예외, 파싱과 무관한 리플렉션 오류는 `FieldAccessException`
    - 판단 근거: design.md Component 1~5가 도입한 "파싱 실패는 항상 `TypeConversionException` 계층의 예외로 던지고(전용 하위 타입이 있으면 그것을, 없으면 `TypeConversionException` 자체를), 파싱과 무관한 리플렉션 접근 오류는 `FieldAccessException`으로 남긴다"는 원칙은 여러 클래스에 걸친 예외 처리 체계의 구조적 패턴이며(판단 신호: "구조적 패턴을 새로 도입하거나 바꾸는 결정"), 향후 새 타입 지원을 추가하는 스펙(예: `BigDecimal` 지원)이 암묵적으로 따라야 할 전제다 — `references/spec-promotion-rules.md`의 승격 기준 1(재사용성)·2(일관성)에 해당한다.
    - `specs/_baseline.md`의 Global Rules 표에 `G-6`("byte[] → Object 변환에서 필드 값 파싱 실패는 항상 `TypeConversionException` 계층의 예외로 던지고(전용 하위 타입이 있으면 그것을, 없으면 `TypeConversionException` 자체를), 파싱과 무관한 리플렉션 접근 오류는 FieldAccessException으로 남긴다")을, Decision Log 표에 `D-6`(근거 한 줄 요약과 이 스펙 design.md로의 링크)를 추가한다. ID는 `G-N`/`D-N` 짝을 맞추는 baseline 관례에 따라 `D-6`으로 맞춘다(`D-2`~`D-5`는 결번으로 남긴다).
    - **완료 조건:** `specs/_baseline.md`의 Global Rules 표에 `G-6` 행, Decision Log 표에 `D-6` 행이 추가되어 있고 `D-6`의 "원본" 열이 이 스펙의 `design.md`를 링크한다.

  - [x] 11.8 `specs/_baseline.md`에 G-7/D-7 추가 — Correctness Property 구현 표준 도구로 jqwik 채택
    - 판단 근거: design.md Correctness Properties 절에서 "AutoParams(`@AutoSource`)는 shrinking을 지원하지 않아 property 검증에는 부족하다"는 트레이드오프를 근거로 jqwik을 채택했다 — `references/spec-promotion-rules.md`의 예시("Correctness Property 구현에는 jqwik을 쓴다" — 기준 3: 테스트 전략 정책)와 정확히 일치하는 승격 대상이다.
    - `specs/_baseline.md`의 Global Rules 표에 `G-7`("Correctness Property는 jqwik `@Property`/`@ForAll`로 구현한다 — AutoParams(`@AutoSource`)는 shrinking을 지원하지 않으므로 대체하지 않는다")을, Decision Log 표에 `D-7`(근거 한 줄 요약과 이 스펙 design.md로의 링크)를 추가한다. `G-7`과 번호를 맞춰 `D-7`을 쓴다.
    - **완료 조건:** `specs/_baseline.md`의 Global Rules 표에 `G-7` 행, Decision Log 표에 `D-7` 행이 추가되어 있고 `D-7`의 "원본" 열이 이 스펙의 `design.md`를 링크한다.

  - 이 태스크가 완료(`[x]`)되면 스펙 전체가 완료된 것이다. 완료된 스펙은 이후 편집하지 않는다 — 새로운 필요가 생기면 새 스펙을 만든다.

---

## Notes

- Step 2~5에서 각 파싱 메서드(`parsePrimitiveOrWrapper`/`parseEnum`/`parseMonth`/`parseDateTime`)를 추가하는 동안 `invokeSetValueByFieldType`의 `throws` 절은 과도기적으로 실제 필요보다 넓게 유지될 수 있다 — Step 5.1에서 design.md Component 1의 최종 시그니처로 한 번에 정리한다.
- 태스크 3.1(Step 2) 완료 직후부터 태스크 4.1(Step 3) 완료 전까지는, Enum 분기가 아직 숫자/Boolean Wrapper 분기와 분리되지 않아 Enum 필드 파싱 실패도 일시적으로 `parsePrimitiveOrWrapper`를 거쳐 `NumberParsingException`이 되는 과도기 상태다. Step 3이 끝나면 `TypeConversionException`으로 정정되므로 최종 상태에는 영향이 없다.
- jqwik 버전은 tasks.md 작성 시점의 최신 안정 버전을 가정한다. 실제 태스크 1.1 실행 시점에 더 새 버전이 나와 있으면 그 버전을 우선 시도한다.
- C-4(primitive 타입 필드의 blank 값 처리 결함)는 이 스펙 범위 밖이며 `docs/backlog.md`에 이미 별도 항목(#BL-05)으로 존재해야 한다 — 이 tasks.md의 어떤 태스크도 이를 다루지 않는다.
- C-5(`lengthField`/`countField` 미존재 시 raw `NullPointerException`)도 이 스펙 범위 밖이며 `docs/backlog.md`에 이미 별도 항목(#BL-06)으로 존재해야 한다 — 이 tasks.md의 어떤 태스크도 이를 다루지 않는다(태스크 7.2는 `Character` 필드 케이스만 검증한다).
