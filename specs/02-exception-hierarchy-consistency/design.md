# Design Document: exception-hierarchy-consistency

## Overview

`ConversionHelper#invokeSetValueByFieldType`가 숫자 Wrapper/`Month`/Enum/`java.time` date-time 타입 각각을 파싱하는 지점에서, 리플렉션 호출 실패(`InvocationTargetException`)와 파싱 자체의 실패(`NumberFormatException`/`DateTimeParseException`/`DateTimeException`/`IllegalArgumentException`)를 분리해 각 타입 전용 예외(`NumberParsingException`/`DateParsingException`/`TypeConversionException`)로 즉시 변환한다(Requirement 1~3). 그 결과 파싱과 무관한 리플렉션 오류만 `convertDataByField`의 기존 catch 블록까지 전파되어 계속 `FieldAccessException`이 된다(Requirement 4). 빈 값 → `null` 처리(Requirement 5)는 기존 위치(모든 파싱 분기보다 앞)를 그대로 유지해 영향을 받지 않는다. 마지막으로 `NumberParsingException.java`/`DateParsingException.java`/`TypeConversionException.java`의 Javadoc과 `README.md`/`README_kr.md`/`CLAUDE.md`의 예외 처리 설명을 이 실제 동작에 맞춰 재동기화한다(Requirement 6).

---

## Architecture

이 스펙은 `ConversionHelper` 내부의 값 파싱 로직 재구성과 관련 문서 갱신만 다룬다. 새 파일은 만들지 않는다.

```
byte-to-object-converter/
├── src/main/java/io/github/libedi/converter/
│   ├── ConversionHelper.java                      ← invokeSetValueByFieldType 분기를 4개의 전용 파싱 메서드로 재구성
│   └── exception/
│       ├── DateParsingException.java              ← Javadoc에 Month 시나리오 추가
│       └── TypeConversionException.java           ← Javadoc에 Enum 변환 실패 예시 추가
├── README.md                                       ← 예외 처리 섹션에 NumberParsingException/DateParsingException 이름으로 추가
├── README_kr.md                                    ← 위와 동일(한글판)
└── CLAUDE.md                                       ← 예외 계층 트리에 Enum/Month가 어느 타입으로 매핑되는지 보강
```

`AbstractCommonHelper.isJavaTimePackageClass`, `ConversionHelper` 나머지 메서드(`extractData`, `extractFieldData` 등), `DeconversionHelper`, 애노테이션 클래스는 변경하지 않는다(C-1: 역변환 방향은 범위 밖).

---

## Baseline Alignment

Correctness Properties(아래 참고) 구현을 위해 jqwik을 `testImplementation`으로 새로 추가하는 것이 아래 전역 규칙과 관련된다.

| Baseline 항목 | 준수 여부 | 비고 |
|---|---|---|
| G-1 (테스트 전용 의존성은 test 스코프로만 선언) | 준수 | jqwik은 `testImplementation`으로만 선언하고 런타임 아티팩트에 노출하지 않는다. |
| G-4 (의존성 좌표·버전은 `gradle/libs.versions.toml` Version Catalog에 선언) | 준수 | jqwik 좌표·버전을 `build.gradle.kts`에 직접 쓰지 않고 `gradle/libs.versions.toml`에 선언한다. |
| G-5 (Gradle 빌드 스크립트는 Kotlin DSL 사용) | 준수 | jqwik 추가는 기존 `build.gradle.kts`/`settings.gradle.kts`(Kotlin DSL) 구조를 그대로 따르며 Groovy DSL을 새로 도입하지 않는다. |

---

## Components and Interfaces

### Component 1: `ConversionHelper#invokeSetValueByFieldType` 분기 재구성

기존에는 숫자 Wrapper 타입과 Enum 타입이 `(!isVoid && isPrimitiveOrWrapper) || isEnum` 하나의 조건으로 묶여 같은 `MethodUtils.invokeStaticMethod(type, "valueOf", value)` 호출을 공유했고, 실패 시 예외가 구분되지 않은 채 `convertDataByField`의 `catch (IOException | ReflectiveOperationException e)`까지 전파되어 전부 `FieldAccessException`이 됐다. `Month`는 리플렉션을 거치지 않는 `Integer.parseInt(value)` 호출이 아예 감싸지지 않아 raw `NumberFormatException`이 그대로 올라갔다.

```java
// 변경 전
private Object invokeSetValueByFieldType(final Field field, final byte[] bytes)
        throws NumberFormatException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    final Class<?> fieldType = field.getType();
    if (ClassUtils.isAssignable(fieldType, byte[].class)) {
        return bytes;
    }
    final String value = StringUtils.trim(new String(bytes, dataCharset));
    if (StringUtils.isBlank(value)) {
        return null;
    }
    if (ClassUtils.isAssignable(fieldType, String.class)) {
        return value;
    }
    if (hasAdditionalTypeFunction.apply(fieldType)) {
        return invokeAdditionalFieldFunction.apply(fieldType, value);
    }
    if (ClassUtils.isAssignable(fieldType, Month.class)) {
        return MethodUtils.invokeStaticMethod(fieldType, "of", Integer.parseInt(value));
    }
    if (!ClassUtils.isAssignable(fieldType, Void.class) && ClassUtils.isPrimitiveOrWrapper(fieldType)
            || fieldType.isEnum()) {
        final Class<?> type = ClassUtils.primitiveToWrapper(fieldType);
        return MethodUtils.invokeStaticMethod(type, "valueOf", value);
    }
    if (isJavaTimePackageClass(fieldType)) {
        final String format = field.getAnnotation(ConvertData.class).format();
        if (StringUtils.isBlank(format)) {
            throw new MissingFormatException("Date format must not be empty.");
        }
        return MethodUtils.invokeStaticMethod(fieldType, "parse", value, DateTimeFormatter.ofPattern(format));
    }
    return null;
}
```

```java
// 변경 후
private Object invokeSetValueByFieldType(final Field field, final byte[] bytes)
        throws NoSuchMethodException, IllegalAccessException {
    final Class<?> fieldType = field.getType();
    if (ClassUtils.isAssignable(fieldType, byte[].class)) {
        return bytes;
    }
    final String value = StringUtils.trim(new String(bytes, dataCharset));
    if (StringUtils.isBlank(value)) {
        return null;
    }
    if (ClassUtils.isAssignable(fieldType, String.class)) {
        return value;
    }
    if (hasAdditionalTypeFunction.apply(fieldType)) {
        return invokeAdditionalFieldFunction.apply(fieldType, value);
    }
    if (ClassUtils.isAssignable(fieldType, Month.class)) {
        return parseMonth(value);
    }
    if (fieldType.isEnum()) {
        return parseEnum(fieldType, value);
    }
    if (!ClassUtils.isAssignable(fieldType, Void.class) && ClassUtils.isPrimitiveOrWrapper(fieldType)) {
        return parsePrimitiveOrWrapper(fieldType, value);
    }
    if (isJavaTimePackageClass(fieldType)) {
        return parseDateTime(field, fieldType, value);
    }
    return null;
}
```

원래 하나였던 조건을 `Month` → Enum → 숫자/Boolean Wrapper → `java.time` 순서의 개별 `if`로 나눈 것은 동작 변경이 아니라(`Month`는 이미 그 위에서 먼저 매치되므로 순서 무관, Enum과 숫자/Boolean Wrapper는 원래도 서로소 집합) 각 분기가 반환하는 예외 타입을 분기별 전용 메서드로 지역화하기 위한 재구성이다. 이 메서드의 `throws` 절에서 `NumberFormatException`/`InvocationTargetException`이 빠진 것은 두 예외가 더 이상 이 메서드 밖으로 나가지 않고(아래 Component 2~4에서 지역적으로 잡아 도메인 예외로 변환), `NoSuchMethodException`/`IllegalAccessException`(예: `Character` 필드처럼 `valueOf(String)` 자체가 없는 타입 — `parsePrimitiveOrWrapper` 내부에서 던져짐)은 그대로 던져 `convertDataByField`가 여전히 `FieldAccessException`으로 감싸도록 남겨뒀기 때문이다(Requirement 4).

이 메서드를 호출하는 `extractData`의 `throws` 절도 동일한 이유로 `IllegalAccessException, IOException, NumberFormatException, NoSuchMethodException, InvocationTargetException`에서 `IllegalAccessException, IOException, NoSuchMethodException`으로 정리한다.

---

### Component 2: primitive/Wrapper 타입 파싱 (`parsePrimitiveOrWrapper`)

```java
private Object parsePrimitiveOrWrapper(final Class<?> fieldType, final String value)
        throws NoSuchMethodException, IllegalAccessException {
    final Class<?> type = ClassUtils.primitiveToWrapper(fieldType);
    try {
        return MethodUtils.invokeStaticMethod(type, "valueOf", value);
    } catch (final InvocationTargetException e) {
        throw new NumberParsingException("Failed to parse number for field type: " + fieldType.getName(), e.getCause());
    }
}
```

`Character`/`Boolean`도 이 메서드를 함께 거치지만 실질적으로 `NumberParsingException`을 던지는 건 실제 숫자 Wrapper 타입뿐이다: `Character`는 `valueOf(String)` 정적 메서드 자체가 없어 `parsePrimitiveOrWrapper` 내부에서 `MethodUtils.invokeStaticMethod`가 `NoSuchMethodException`을 던지고(잡지 않으므로 그대로 전파되어 `FieldAccessException`으로 귀결), `Boolean.valueOf(String)`은 어떤 문자열에도 예외 없이 값을 반환하므로 `InvocationTargetException` 분기에 도달할 일이 없다(Glossary "숫자 Wrapper 타입" 정의와 일치).

---

### Component 3: Enum 파싱 (`parseEnum`)

```java
private Object parseEnum(final Class<?> fieldType, final String value)
        throws NoSuchMethodException, IllegalAccessException {
    try {
        return MethodUtils.invokeStaticMethod(fieldType, "valueOf", value);
    } catch (final InvocationTargetException e) {
        throw new TypeConversionException("Failed to convert value to enum constant: " + fieldType.getName(), e.getCause());
    }
}
```

`NumberParsingException`/`DateParsingException`이 아니라 `TypeConversionException`을 직접 `new`하는 것이 Requirement 3.1("하위 타입이 아닌 `TypeConversionException` 인스턴스")의 핵심이다.

---

### Component 4: `Month` 파싱 (`parseMonth`)

```java
private Object parseMonth(final String value) {
    final int monthValue;
    try {
        monthValue = Integer.parseInt(value);
    } catch (final NumberFormatException e) {
        throw new DateParsingException("Failed to parse month value: " + value, e);
    }
    try {
        return Month.of(monthValue);
    } catch (final DateTimeException e) {
        throw new DateParsingException("Invalid month value: " + monthValue, e);
    }
}
```

**왜 이렇게 했는가:** 변경 전 코드는 `Month.of(int)`를 `MethodUtils.invokeStaticMethod(fieldType, "of", ...)`로 리플렉션 호출했지만, 이 분기에 도달하는 `fieldType`은 이미 `ClassUtils.isAssignable(fieldType, Month.class)`로 확인된 뒤이므로(`Month`는 `enum`이라 하위 타입이 존재할 수 없음) 항상 `Month.class` 하나뿐이다. 리플렉션을 거칠 이유가 없어 `Month.of(int)`를 직접 호출하도록 바꿨다 — `InvocationTargetException` 언랩 없이 `DateTimeException`을 바로 잡을 수 있어 Requirement 2.3(cause 보존)도 더 단순해진다. `Integer.parseInt(value)` 실패와 `Month.of(int)` 실패를 각각 별도 `try`로 감싸 두 원인(`NumberFormatException`, `DateTimeException`) 모두 Requirement 2.2가 요구하는 대로 `DateParsingException`으로 귀결시킨다.

---

### Component 5: `java.time` date-time 파싱 (`parseDateTime`)

```java
private Object parseDateTime(final Field field, final Class<?> fieldType, final String value)
        throws NoSuchMethodException, IllegalAccessException {
    final String format = field.getAnnotation(ConvertData.class).format();
    if (StringUtils.isBlank(format)) {
        throw new MissingFormatException("Date format must not be empty.");
    }
    try {
        return MethodUtils.invokeStaticMethod(fieldType, "parse", value, DateTimeFormatter.ofPattern(format));
    } catch (final InvocationTargetException e) {
        throw new DateParsingException("Failed to parse date-time value: " + value, e.getCause());
    }
}
```

`MissingFormatException`(포맷 자체가 없는 애노테이션 설정 오류)은 파싱 실패가 아니라 기존처럼 그대로 던진다 — Requirement 2와 무관하며 이 스펙에서 동작을 바꾸지 않는다.

---

### Component 6: 예외 클래스 Javadoc 재동기화

- `NumberParsingException.java`: 발생 상황 목록의 `<li>공백을 포함한 문자열인 경우</li>` 불릿을 제거(또는 실제 동작에 맞게 수정)한다 — `ConversionHelper.java`의 blank→null 처리(Requirement 5)로 인해 trim 후 빈 문자열은 이 예외를 발생시키지 않으므로, 현재 Javadoc은 Requirement 6.1이 금지하는 "실제 발생 조건에 없는 시나리오"를 서술하고 있다. 나머지 불릿(숫자 아닌 문자, 범위 초과, 잘못된 부동소수점 형식)은 실제 발생 조건과 일치하므로 유지한다.
- `DateParsingException.java`: 발생 상황 목록과 예시에 `Month`(정수 변환 실패·1~12 범위 초과) 시나리오를 추가해 Requirement 2.2를 반영한다.
- `TypeConversionException.java`: "문자열을 Enum 값으로 변환 실패" 불릿이 이제 실제로 이 클래스가 직접 던져지는 시나리오이므로, 예시 코드 블록에 Enum 변환 실패 예시(`&#64;ConvertData` 필드가 Enum 상수명이 아닌 값을 받는 경우 → `TypeConversionException`)를 추가한다.

---

### Component 7: `README.md`/`README_kr.md` 예외 처리 섹션 갱신

현재 두 README는 `TypeConversionException` 불릿 하나만 나열하고 `NumberParsingException`/`DateParsingException`을 이름으로 언급하지 않는다(Requirement 6.2 미충족). `TypeConversionException` 불릿 아래에 두 하위 클래스를 각각 이름과 발생 조건으로 추가한다.

```markdown
- **`ConvertFailException`** - Base abstract exception for all conversion/deconversion failures
  - **`TypeConversionException`** - Raised when field type conversion (parsing) fails (e.g., Enum constant lookup)
    - **`NumberParsingException`** - Raised when a numeric wrapper field's value is not a valid number
    - **`DateParsingException`** - Raised when a `java.time` date-time field's value (including `Month`) is not a valid date/month
```

(기존 README.md:197/200, README_kr.md:197/200과 동일한 부모 2칸/자식 4칸 들여쓰기 구조를 따른다. `ConvertFailException`/`TypeConversionException` 줄은 이미 README에 있으므로 실제 편집에서는 `TypeConversionException` 불릿 아래에 `NumberParsingException`/`DateParsingException` 두 줄만 4칸 들여쓰기로 삽입한다.)

`README_kr.md`는 동일 구조를 한글 서술로 반영한다.

---

### Component 8: `CLAUDE.md` 예외 계층 설명 보강

`CLAUDE.md`의 예외 계층 트리는 이미 `NumberParsingException`/`DateParsingException`을 이름으로 나열하고 있어 Requirement 6.3의 최소 조건은 충족하지만, `TypeConversionException`이 Enum 변환 실패 시 직접(하위 타입 없이) 던져진다는 점과 `Month`가 `DateParsingException`에 포함된다는 점이 드러나지 않는다. 트리 항목 설명을 아래처럼 보강한다.

```
├── TypeConversionException — failed to convert field value (e.g., Enum constant lookup)
│   ├── DateParsingException — failed to parse date-time value (java.time types, including Month)
│   └── NumberParsingException — failed to parse numeric wrapper value
```

---

## Data Models

이 스펙은 클래스 구조·애노테이션·필드 데이터 모델을 변경하지 않는다(C-2: 예외 클래스의 이름·상속 관계·생성자 시그니처도 그대로). 변경 대상은 `ConversionHelper` 내부 파싱 분기 로직과 문서뿐이다.

---

## Error Handling

| 시나리오 | 대응 전략 |
|---|---|
| 숫자 Wrapper 타입 필드 값이 유효한 숫자 형식이 아님 | `parsePrimitiveOrWrapper`에서 `InvocationTargetException`을 잡아 `NumberParsingException(message, cause)`로 변환(cause는 원본 `NumberFormatException`) |
| `Month` 필드 값이 정수로 파싱되지 않음 | `parseMonth`에서 `NumberFormatException`을 직접 잡아 `DateParsingException(message, cause)`로 변환 |
| `Month` 필드 값이 정수이지만 1~12 범위를 벗어남 | `parseMonth`에서 `Month.of(int)`의 `DateTimeException`을 잡아 `DateParsingException(message, cause)`로 변환 |
| `java.time` date-time 필드 값이 `format` 패턴으로 파싱되지 않음 | `parseDateTime`에서 `InvocationTargetException`을 잡아 `DateParsingException(message, cause)`로 변환(cause는 원본 `DateTimeParseException` 등) |
| Enum 필드 값이 해당 Enum의 유효한 상수명이 아님 | `parseEnum`에서 `InvocationTargetException`을 잡아 `TypeConversionException(message, cause)`로 변환(cause는 원본 `IllegalArgumentException`) |
| `Character` 필드처럼 `valueOf(String)` 정적 메서드 자체가 없는 타입 | `parsePrimitiveOrWrapper` 내부에서 `MethodUtils.invokeStaticMethod`가 던지는 `NoSuchMethodException`을 지역적으로 잡지 않고 그대로 전파 → `convertDataByField`가 기존과 동일하게 `FieldAccessException`으로 감쌈 |
| `lengthField`/`countField`로 지정된 필드가 존재하지 않음 | C-5: 이 스펙 범위 밖 — 기존과 동일하게 감싸지지 않은 `NullPointerException`이 raw로 올라감(`FieldAccessException`이 아님), `docs/backlog.md` #BL-06으로 별도 기록 |
| 참조 타입 필드의 trim 결과가 빈 문자열 | 모든 파싱 분기보다 앞서 있는 기존 `StringUtils.isBlank(value)` 체크가 그대로 `null` 반환(변경 없음) |
| primitive 타입 필드에 trim 결과가 빈 문자열 (`Field#set`에 `null` 전달) | C-4: 이 스펙 범위 밖, `docs/backlog.md`에 별도 항목으로 존재 |
| 대상 객체/VO의 기본 생성자 호출 실패 | 이 스펙 범위 밖(Requirement 4 User Story 단서 참고) — 기존과 동일하게 `ConstructorInvocationException` |
| Enum/숫자 Wrapper/`Month`/`java.time` 값이 모두 유효 | 기존과 동일하게 정상 변환, 새 예외 없음 |

---

## Correctness Properties

Requirement 1~3은 "타입 범주(숫자 Wrapper/`Month`+`java.time`/Enum)와 무관하게 파싱 불가능한 임의의 문자열이 들어오면 항상 지정된 예외 타입과 보존된 cause로 귀결된다"는 보편적 속성으로 표현 가능하다. Requirement 4·5는 "이 스펙이 건드리지 않는 경로의 기존 동작이 그대로 유지되는가"라는 회귀 성격의 요구라 임의 입력에 대한 보편 속성보다는 대표 케이스 기반 example-based test(기존 `ByteToObjectConverterTest`의 리플렉션 오류·빈 값 케이스 확장)로 충분하다.

### Property 1: 숫자 Wrapper 타입 파싱 실패는 항상 `NumberParsingException`이다

숫자 Wrapper 타입 집합(`Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`) 중 임의의 하나를 필드 타입으로 갖는 필드에, 그 타입으로 파싱 불가능한(trim 후 비어있지 않은) 임의의 문자열이 입력되면, 변환 시 항상 `NumberParsingException`이 던져지고 그 `getCause()`는 `InvocationTargetException`이 아닌 원본 파싱 예외(`NumberFormatException`)다.

**Validates: Requirements 1.1, 1.2**

**구현:** jqwik (`@Property` + `@ForAll`). `convert()`는 필드를 선언 순서대로 순차 처리하므로, 숫자 Wrapper 6종을 한 픽스처 클래스에 같이 두면 `@ForAll`로 어떤 타입을 고르든 항상 첫 번째 필드에서만 예외가 발생해 나머지 5종은 검증되지 않는다. 대신 타입별로 필드를 정확히 하나만 가진 픽스처 클래스 6종(예: `ByteFieldFixture`, `ShortFieldFixture`, ..., `DoubleFieldFixture`)을 두고, `@Provide`로 그 6종 픽스처 클래스(생성 함수) 중 하나를 고르는 provider와 해당 타입에 대해 파싱 불가능한 문자열을 생성하는 provider를 조합한다. 후자의 생성기는 각 픽스처의 `@ConvertData(value = N)` 필드 길이 `N`을 넘지 않는 길이(`N`자 이하)의 ASCII 문자열만 생성하도록 `@ForAll`의 문자 범위/길이 제약(예: jqwik `Arbitraries.strings().ascii().ofMaxLength(N)`)을 두고, 거기에 "trim 후 비어있지 않음"과 "해당 타입으로 파싱 불가능함" 두 조건을 필터(`.filter(...)`)로 강제한다 — 절단(`substring` 등)은 우연히 파싱 가능한 접두어를 남기거나 공백만 남겨 blank→null 경로로 새는 등 property를 거짓 실패시킬 수 있으므로 쓰지 않는다. 생성된 문자열은 `N`보다 짧을 수 있으므로 `StringUtils.rightPad`로만 정확히 `N`자(ASCII이므로 `N`바이트와 동일)로 맞춰 `InputStream`을 구성한다. AutoParams(`@AutoSource`)는 shrinking을 지원하지 않아 이 용도로 쓰지 않는다.

### Property 2: `java.time` date-time 타입(`Month` 포함) 파싱 실패는 항상 `DateParsingException`이다

`format`이 지정된 `java.time` date-time 타입 필드에 그 포맷으로 파싱 불가능한 임의의 문자열이 입력되거나, `Month` 필드에 정수로 파싱되지 않거나 1~12 범위를 벗어나는 임의의 값이 입력되면, 변환 시 항상 `DateParsingException`이 던져지고 그 `getCause()`는 `InvocationTargetException`이 아닌 원본 예외(`DateTimeParseException`/`NumberFormatException`/`DateTimeException`)다.

**Validates: Requirements 2.1, 2.2, 2.3**

**구현:** jqwik (`@Property` + `@ForAll`). `LocalDate`(`format = "yyyyMMdd"`, 필드 길이 8바이트) 필드에는 Property 1과 같은 원칙 — 길이 8자 이하의 ASCII 문자열만 생성하고, "trim 후 비어있지 않음"과 "`yyyyMMdd` 패턴으로 파싱 불가능함"을 필터로 강제한 뒤 `StringUtils.rightPad`로만 정확히 8자를 맞춘다(절단 없음) — 로 생성한 값을 검증한다. `Month` 필드(`@ConvertData(2)`, 2바이트)에는, (a) 길이 2자 이하의 ASCII 문자열 중 "trim 후 비어있지 않음"과 "숫자로 파싱 불가능함"을 필터로 강제한 뒤 `rightPad`로 정확히 2자를 맞춘 값과, (b) 필드 길이 2바이트가 표현 가능한 범위 안에서 1~12를 벗어나는 정수(즉 `13`~`99`, 이미 2자이므로 패딩 불필요)를 `@ForAll`로 생성해 각각 검증한다 — 3자리 이상 값은 애초에 2바이트 필드에 담을 수 없으므로 생성 범위에서 제외한다.

### Property 3: Enum 타입 파싱 실패는 항상 하위 타입이 아닌 `TypeConversionException`이다

Enum 타입(`Month` 제외) 필드에 그 Enum의 유효한 상수명이 아닌 임의의 문자열이 입력되면, 변환 시 항상 `getClass() == TypeConversionException.class`인 예외가 던져지고 그 `getCause()`는 `InvocationTargetException`이 아닌 원본 예외(`IllegalArgumentException`)다.

**Validates: Requirements 3.1, 3.2**

**구현:** jqwik (`@Property` + `@ForAll`). 테스트용 Enum(예: 기존 `Week`)의 상수명 집합에 속하지 않는 임의의 문자열을 생성해 검증한다.

프로젝트에는 아직 jqwik 의존성이 없다(`build.gradle.kts`/`gradle/libs.versions.toml` 확인 결과 `testImplementation`에 JUnit5/Mockito/AssertJ/AutoParams만 선언돼 있다) — tasks.md에 `gradle/libs.versions.toml`과 `build.gradle.kts`에 jqwik(JUnit5 엔진 연동 포함, 예: `net.jqwik:jqwik`)을 `testImplementation`으로 추가하는 태스크를 포함한다.

---

## Testing Strategy

```bash
# 전체 빌드 + 테스트
./gradlew clean build

# 테스트만 실행
./gradlew test

# 단일 테스트 클래스만 실행
./gradlew test --tests "io.github.libedi.converter.ByteToObjectConverterTest"
```

주요 확인 사항:
- Requirement 1~3의 각 대표 케이스(숫자 Wrapper 파싱 실패, `Month` 파싱 실패 2종, `java.time` 파싱 실패, Enum 파싱 실패)에서 지정된 예외 타입과 보존된 cause가 던져지는지 example-based test로 확인.
- 위 Correctness Properties 1~3을 jqwik `@Property`로 구현해 임의 입력에 대한 보편 성립을 확인.
- 기존 `ByteToObjectConverterTest#convert` 등 정상 변환 케이스가 회귀 없이 계속 통과하는지 확인(Requirement 1.3, 2.4, 3.3).
- `Character` 필드 등 기존 `FieldAccessException` 케이스가 계속 `FieldAccessException`으로 유지되는지 확인(Requirement 4). (`lengthField`/`countField` 미존재는 C-5에 따라 범위 밖 — `docs/backlog.md` #BL-06 참고)
- 참조 타입 필드의 빈 값이 예외 없이 `null`로 남는 기존 케이스가 회귀 없이 유지되는지 확인(Requirement 5).
- `./gradlew javadoc`이 성공하고, `README.md`/`README_kr.md`/`CLAUDE.md`에 `NumberParsingException`/`DateParsingException`이 이름으로 등장하는지 확인(Requirement 6).
- `NumberParsingException.java`/`DateParsingException.java`/`TypeConversionException.java` 세 Javadoc 모두에 빈 문자열/공백 기인 파싱 실패를 발생 조건으로 서술한 부분이 없는지 확인(Requirement 6.1).

비즈니스 로직 구현은 tasks.md에서 Red → Green → Refactor 순서로 진행한다(Red 단계 테스트는 위 Acceptance Criteria/Correctness Property에서 도출).
