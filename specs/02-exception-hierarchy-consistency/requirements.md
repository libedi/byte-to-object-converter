# Requirements Document: exception-hierarchy-consistency

## Introduction

`byte-to-object-converter`는 v2.0에서 `NumberParsingException`/`DateParsingException`을 포함한 구조화된 예외 계층을 도입했고, 각 클래스의 Javadoc은 "숫자 파싱 실패 시 `NumberParsingException`이 발생한다", "날짜 파싱 실패 시 `DateParsingException`이 발생한다"는 구체적 시나리오를 서술하고 있다. 그런데 실제로는 이 두 클래스가 소스 전체에서 한 번도 생성(`new`)되지 않는다 — `ConversionHelper`가 리플렉션으로 값을 파싱할 때 발생한 실패는 대부분 `FieldAccessException`으로 감싸지고, 리플렉션을 거치지 않는 일부 경로(`Month` 타입)는 그마저도 감싸지지 않은 채 raw 예외로 올라간다.

이 스펙은 `byte[]` → `Object` 변환(`ByteToObjectConverter#convert`) 과정에서 필드 값 파싱이 실패할 때, 문서가 약속한 대로 `NumberParsingException`/`DateParsingException`/`TypeConversionException`이 실제로 던져지도록 동작을 바꾸는 것을 목표로 한다. `2.0.0`은 아직 Maven Central에 정식 배포된 적이 없어 이 동작 변경은 실제 소비자를 깨뜨리는 breaking change가 아니다. 완료 상태는 대표 재현 케이스를 포함한 자동화된 테스트로 새 예외 타입이 실제로 던져짐을 확인하고, 관련 Javadoc·README·CLAUDE.md 서술이 이 동작과 다시 일치하는 상태다.

---

## Glossary

전역 용어집: [specs/_baseline.md](../_baseline.md) 참고. 이 스펙 자체(What)가 강제하는 전역 규칙(G-)·결정(D-) 항목은 없다 — 구현 방식(How)에 따라 관련 항목이 드러날 수 있으며, 그 경우 design.md의 Baseline Alignment에서 다룬다.

이 스펙에서만 쓰는 용어:

| 용어 | 정의 |
|------|------|
| **파싱 실패** | `@ConvertData`로 읽은 byte 데이터를 Charset으로 문자열로 변환하고 앞뒤 공백을 trim한 뒤, 그 trim된 문자열이 비어 있지 않은데도 필드의 실제 타입(숫자 Wrapper 타입, `Month`, Enum 타입, `java.time` 패키지의 date-time 타입) 값으로 바뀌지 못하는 상황. trim 결과가 빈 문자열인 경우(Requirement 5 참고)나, 값을 읽어올 필드·메서드 자체에 접근할 수 없는 등 파싱과 무관한 리플렉션 오류(Requirement 4 참고)는 포함하지 않는다. |
| **숫자 Wrapper 타입** | `Byte`/`byte`, `Short`/`short`, `Integer`/`int`, `Long`/`long`, `Float`/`float`, `Double`/`double`. `Character`/`char`는 숫자 타입이 아니고 `Boolean`/`boolean`은 `Boolean.valueOf(String)`이 어떤 문자열에도 예외 없이 값을 반환해 파싱 실패가 존재하지 않으므로 둘 다 제외한다. |

---

## Requirements

### Requirement 1: 숫자 Wrapper 타입 파싱 실패 시 `NumberParsingException`

**User Story:** As a 이 라이브러리의 사용자, I want 숫자 필드의 byte 데이터가 유효한 숫자 형식이 아닐 때 `NumberParsingException`을 받는 것, so that Javadoc에 문서화된 대로 구체적인 예외 타입으로 catch해 실패 원인을 정확히 구분하고 처리할 수 있다.

#### Acceptance Criteria

1. WHEN `@ConvertData`로 지정된 숫자 Wrapper 타입 필드를 변환할 때 파싱 실패가 발생하면, THE Converter SHALL `NumberParsingException`을 던진다.
2. THE Converter SHALL 던져진 `NumberParsingException`의 `getCause()`가, 리플렉션 중간 예외(`InvocationTargetException` 등) 없이 원래의 숫자 형식 파싱 실패 예외(예: `NumberFormatException`)이도록 보존한다.
3. WHEN 문자열 값이 필드 타입에 유효한 숫자 형식이면, THE Converter SHALL 기존과 동일하게 변환에 성공하고 `NumberParsingException`을 던지지 않는다.

---

### Requirement 2: `java.time` 패키지 date-time 타입(`Month` 포함) 파싱 실패 시 `DateParsingException`

**User Story:** As a 이 라이브러리의 사용자, I want 날짜-시간 필드(달력 개념인 `Month` 포함)의 byte 데이터가 유효한 날짜/월 값이 아닐 때 `DateParsingException`을 받는 것, so that 날짜 관련 데이터 오류를 필드 타입만 보고 일관되게 예측하고 처리할 수 있다.

#### Acceptance Criteria

1. WHEN `@ConvertData`로 지정된 `java.time` 패키지 date-time 타입(`Month` 제외 — AC 2.2 참고) 필드에서 파싱 실패가 발생하면(즉, 값이 `format` 속성에 지정된 패턴으로 파싱되지 않으면), THE Converter SHALL `DateParsingException`을 던진다.
2. WHEN `@ConvertData`로 지정된 `java.time.Month` 타입 필드에서 파싱 실패가 발생하면(즉, 값이 정수로 파싱되지 않거나 정수로 파싱되더라도 유효한 월 범위(1~12)를 벗어나면), THE Converter SHALL `DateParsingException`을 던진다.
3. THE Converter SHALL 던져진 `DateParsingException`의 `getCause()`가, 리플렉션 중간 예외(`InvocationTargetException` 등) 없이 원래의 날짜-시간 파싱/검증 실패 예외(예: `DateTimeParseException`, `NumberFormatException`, `DateTimeException`)이도록 보존한다.
4. WHEN 문자열 값이 필드 타입에 유효한 값(`format` 지정 타입은 그 패턴으로 유효하게 파싱, `Month`는 1~12 범위의 월 값)이면, THE Converter SHALL 기존과 동일하게 변환에 성공하고 `DateParsingException`을 던지지 않는다.

---

### Requirement 3: Enum 타입 파싱 실패 시 `TypeConversionException`

**User Story:** As a 이 라이브러리의 사용자, I want Enum 필드의 byte 데이터가 해당 Enum의 유효한 상수명이 아닐 때 `TypeConversionException`을 받는 것, so that `TypeConversionException.java`가 이미 문서화한 "문자열을 Enum 값으로 변환 실패" 시나리오가 실제 동작과 일치한다.

#### Acceptance Criteria

1. WHEN `@ConvertData`로 지정된 Enum 타입(`java.time.Month` 제외 — Requirement 2 참고) 필드에서 파싱 실패가 발생하면(즉, 값이 해당 Enum의 유효한 상수명이 아니면), THE Converter SHALL 하위 타입이 아닌 `TypeConversionException` 인스턴스를 던진다.
2. THE Converter SHALL 던져진 `TypeConversionException`의 `getCause()`가, 리플렉션 중간 예외(`InvocationTargetException` 등) 없이 원래의 Enum 값 변환 실패 예외(예: `IllegalArgumentException`)이도록 보존한다.
3. WHEN 문자열 값이 해당 Enum의 유효한 상수명이면, THE Converter SHALL 기존과 동일하게 변환에 성공하고 예외를 던지지 않는다.

---

### Requirement 4: 파싱과 무관한 리플렉션 오류는 회귀 없이 `FieldAccessException`으로 유지

**User Story:** As a 이 라이브러리의 사용자, I want 값 파싱 실패가 아닌 다른 리플렉션 오류(예: 대상 필드에 접근할 수 없음, `Character` 필드처럼 `valueOf(String)` 메서드 자체가 없는 타입)는 계속 `FieldAccessException`으로 받는 것, so that Requirement 1~3의 변경이 기존에 검증된 리플렉션 오류 처리 동작을 깨뜨리지 않는다. (대상 객체/VO의 기본 생성자 호출 실패는 이미 별도의 `ConstructorInvocationException`으로 처리되고 있으며, `lengthField`/`countField`로 지정된 필드가 존재하지 않는 경우는 현재도 `FieldAccessException`이 아닌 감싸지지 않은 `NullPointerException`이 발생한다 — 둘 다 이 스펙과 무관하다.)

#### Acceptance Criteria

1. IF 필드 값 변환 과정에서 파싱 실패가 아닌 필드 접근 관련 리플렉션 오류(대상 필드 접근 실패, 대상 타입에 파싱에 쓰일 메서드 자체가 없음 등)가 발생하면, THEN THE Converter SHALL 기존과 동일하게 `FieldAccessException`을 던진다.
2. THE Converter SHALL Requirement 1~3에서 정의한 파싱 실패 시나리오 이외의 모든 필드 변환 동작에서, 이 스펙 적용 이전과 동일한 결과(정상 변환 성공 또는 이 스펙이 다루지 않는 기존 예외 타입)를 낸다.

---

### Requirement 5: 참조 타입 필드의 빈 값(공백)은 예외 없이 `null` 처리 유지

**User Story:** As a 이 라이브러리의 사용자, I want 참조 타입 필드의 byte 데이터가 trim 후 빈 문자열이 되는 경우 예외 없이 해당 필드가 `null`로 남는 기존 동작을 유지하는 것, so that Requirement 1~3의 변경이 빈 값 처리라는 별개의 기존 동작을 건드리지 않는다.

#### Acceptance Criteria

1. IF `@ConvertData`로 지정된 참조 타입(primitive가 아닌) 필드(`byte[]` 제외 — `String`을 포함해 숫자 Wrapper 타입·`Month`·Enum 타입·`java.time` 날짜-시간 타입 모두 해당)의 byte 데이터를 문자열로 변환하고 trim한 결과가 빈 문자열이면, THEN THE Converter SHALL `NumberParsingException`/`DateParsingException`/`TypeConversionException`/`FieldAccessException`을 던지지 않고 해당 필드를 `null`로 둔다.

---

### Requirement 6: 문서를 실제 동작과 재동기화

**User Story:** As a 이 라이브러리를 소비하는 개발자, I want Javadoc·README·CLAUDE.md의 예외 처리 설명이 실제로 던져지는 예외 타입과 일치하는 것, so that catch 절을 작성할 때 문서만 보고도 올바른 예외 타입을 신뢰할 수 있다.

#### Acceptance Criteria

1. THE Converter SHALL `NumberParsingException.java`/`DateParsingException.java`/`TypeConversionException.java`의 Javadoc에 Requirement 1~5에서 확정된 실제 발생 조건에 없는 시나리오(예: 빈 문자열로 인한 파싱 실패)를 서술하지 않는 상태를 유지한다.
2. THE Converter SHALL `README.md`/`README_kr.md`의 예외 처리 섹션이 `NumberParsingException`/`DateParsingException`을 각각 이름으로 언급하며 Requirement 1~5에서 확정된 발생 조건을 명시한 상태를 유지한다.
3. THE Converter SHALL `CLAUDE.md`의 예외 계층 설명이 `NumberParsingException`/`DateParsingException`을 각각 이름으로 언급하며 Requirement 1~5에서 확정된 발생 조건을 명시한 상태를 유지한다.

---

## Constraints

프로젝트 전역 제약: [specs/_baseline.md](../_baseline.md) 참고. 아래는 이 스펙 한정 제약(C-)이며, 구현 방식에 따라 적용되는 전역 규칙(G-1/G-4/G-5)은 design.md의 Baseline Alignment에서 다룬다.

| ID | 제약 |
|----|------|
| C-1 | 이 스펙은 `byte[]` → `Object` 변환(`ByteToObjectConverter#convert`, `ConversionHelper`) 방향만 다룬다. `Object` → `byte[]` 역변환(`ByteToObjectConverter#deconvert`, `DeconversionHelper`) 방향에서 유사하게 발생할 수 있는 리플렉션 예외 래핑 문제(예: 날짜 포맷팅 실패)는 이 스펙의 범위 밖이며, 필요하면 별도 스펙에서 다룬다. |
| C-2 | 이 스펙은 예외 클래스의 이름·상속 관계·생성자 시그니처를 변경하지 않는다 — 어떤 상황에 어떤 기존 클래스가 던져지는지만 바꾼다. |
| C-3 | 이 스펙은 사용자 정의 타입 변환(`hasAdditionalType`/`invokeAdditionalField` 확장 지점)의 예외 처리 방식을 변경하지 않는다 — 이미 `TypeConversionException`으로 올바르게 감싸지고 있음을 확인했다. |
| C-4 | primitive 타입 필드(`int`/`long`/`double` 등)에 trim 후 빈 문자열이 들어오는 경우의 동작은 이 스펙에서 다루지 않는다. 현재 이 경우 `null`이 `Field#set`에 전달되어 `IllegalArgumentException`이 감싸지지 않은 채 raw로 올라가는 별개의 기존 결함이 있으며, 이는 이 스펙과 무관하게 `docs/backlog.md`에 별도 항목으로 기록한다. |
| C-5 | `lengthField`/`countField`로 지정된 필드가 대상 클래스에 존재하지 않는 경우의 동작은 이 스펙에서 다루지 않는다. 현재 이 경우 `FieldUtils.getField`가 `null`을 반환하고 `FieldUtils.readField`가 `NullPointerException`을 던진다. 이는 `ReflectiveOperationException`이 아니어서 감싸지지 않은 채 raw로 올라가는 별개의 기존 결함이며, 이 스펙과 무관하게 `docs/backlog.md`에 별도 항목으로 기록한다. |
