# Design: v2.0 양방향 변환 기능

## 1. 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────┐
│         ByteToObjectConverter (public facade)                │
│  - convert(InputStream, Class<T>) : T                       │
│  - deconvert(Object, DataAlignment) : byte[]                │
│  - convertInputStream(InputStream, int) : String            │
│  - 확장 포인트: hasAdditionalType, invokeAdditionalField,   │
│                 changeAdditionalDataToString               │
└────────────────┬──────────────────────────────────────────┘
                 │
      ┌──────────┴──────────┐
      │                     │
      v                     v
┌──────────────────┐  ┌──────────────────────┐
│ ConversionHelper │  │ DeconversionHelper   │
│ (byte[] → Object)│  │ (Object → byte[])    │
└────────┬─────────┘  └──────────┬───────────┘
         │                       │
         └───────────┬───────────┘
                     │
                     v
           ┌─────────────────────┐
           │ AbstractCommonHelper │
           │ (리플렉션 공용 유틸) │
           └─────────────────────┘

지원 컴포넌트
├── DataAlignment (enum)
│   ├── LEFT: "data____" (오른쪽 패딩)
│   └── RIGHT: "____data" (왼쪽 패딩)
├── ConvertFailException (RuntimeException)
└── Annotations
    ├── @ConvertData (필드 변환 규칙)
    ├── @Iteration (List 반복)
    ├── @Embeddable (nested Value Object)
    └── @Ignorable (역변환 시 null 필드 무시)
```

---

## 2. 핵심 컴포넌트

### 2.1 ByteToObjectConverter (Public Facade)

**책임**: 사용자 API 제공, Helper 객체 생성 및 위임

**설계 원칙**:
- Template method pattern으로 확장 포인트 제공
- Converter 인스턴스마다 독립적인 Helper 객체 생성
- Charset을 Constructor에서 전달받아 Helper에 주입

**공개 메서드**:
1. `convert(InputStream, Class<T>)` — ConversionHelper 위임
2. `deconvert(Object, DataAlignment)` — DeconversionHelper 위임
3. `convertInputStream(InputStream, int)` — InputStream에서 특정 길이 문자열 읽기

**확장 포인트** (override 가능):
1. `hasAdditionalType(Class<?>)` — 사용자 정의 타입 여부 판정
2. `invokeAdditionalField(Class<?>, String)` — 사용자 정의 타입 변환 (byte[] → Object 방향)
3. `changeAdditionalDataToString(Object)` — 사용자 정의 타입 역변환 (Object → byte[] 방향)

**특징**:
- 확장 포인트는 Function/BiFunction으로 래핑되어 Helper에 전달
- Exception 발생 시 ConvertFailException으로 변환

---

### 2.2 ConversionHelper (byte[] → Object)

**책임**: byte 스트림 읽기 및 Object 필드 매핑

**핵심 메서드**:
- `convert(InputStream, Class<T>)` — 전체 변환 프로세스 조율
- `extractData(Field, InputStream, ...)` — 필드별 데이터 추출 및 타입 변환
- `extractIteratedData()` — List 필드 반복 처리
- `extractEmbeddedData()` — nested Value Object 재귀 처리
- `invokeSetValueByFieldType()` — 필드 타입별 값 설정

**동작 흐름**:
```
1. 입력값 검증 (InputStream, Class null 체크)
2. 대상 Object 생성 (기본 생성자 호출)
3. 클래스 필드 목록 순회
4. @ConvertData/@Iteration/@Embeddable 필드만 처리
5. 각 필드에 대해:
   - 지정된 길이의 바이트 읽기 (또는 lengthField로 동적 길이)
   - 필드 타입에 따라 값 변환
   - 반사(Reflection)로 필드에 값 설정
6. 변환된 Object 반환
```

**타입 변환 로직**:
- `byte[]` → 바이트 그대로 설정
- `String` → charset으로 decode, trim
- Primitive/Wrapper → String으로 decode 후 valueOf()
- Enum → String으로 decode 후 valueOf()
- `java.time.*` → format 속성으로 DateTimeFormatter 생성하여 parse()
- 사용자 정의 타입 → `invokeAdditionalField()` 함수 호출
- `List<T>` → `@Iteration` 처리로 N번 반복 변환
- nested Value Object → `@Embeddable` 처리로 재귀 호출

**@ConvertData(value=-1) 처리 (가변 길이)**:
- value=-1로 설정된 필드는 InputStream에 남은 모든 바이트를 읽음
- 길이 제약 없음
- 여러 필드에 value=-1이 있으면 첫 번째만 유효 (이후는 빈 데이터)

---

### 2.3 DeconversionHelper (Object → byte[])

**책임**: Object 필드 읽기 및 byte 배열로 직렬화

**핵심 메서드**:
- `deconvert(Object, DataAlignment)` — 전체 역변환 프로세스 조율
- `deconvertDataByField()` — 필드별 데이터 직렬화
- `extractIteratedData()` — List 필드 반복 처리
- `extractEmbeddedData()` — nested Value Object 재귀 처리
- `deconvertFieldDataToBytes()` — 필드 타입별 byte[] 변환

**동작 흐름**:
```
1. 입력값 검증 (targetObject null 체크)
2. 클래스 필드 목록 순회 (선언 순서)
3. @ConvertData/@Iteration/@Embeddable 필드만 처리
4. 각 필드에 대해:
   - @Ignorable + null 필드: 건너뛰기 (0 바이트)
   - @Iteration 필드: countField 또는 value만큼 반복 처리
   - @Embeddable 필드: 재귀 호출
   - 일반 필드: 타입에 따라 String/byte[]로 변환
5. DataAlignment 적용 (패딩)
6. 모든 바이트 연결하여 반환
```

**타입 변환 로직** (Object → String):
- `byte[]` → 그대로 반환 (패딩만 적용)
- `String` → 그대로 반환
- Primitive/Wrapper → toString()
- Enum → toString()
- `Month` → getValue() 호출
- `java.time.*` → format 속성으로 DateTimeFormatter 생성하여 format()
- 사용자 정의 타입 → `changeAdditionalDataToString()` 함수 호출

**@Ignorable 처리**:
- `@Ignorable` + null: 해당 필드 건너뜀 (0 바이트)
- `@Ignorable` + null 아님: 정상 직렬화
- `@Ignorable` 없음 + null: ConvertFailException 발생

---

### 2.4 AbstractCommonHelper (공용 유틸)

**책임**: 리플렉션 기반 공용 기능 제공

**주요 메서드**:
- `isTargetField(Field)` — `@ConvertData/@Iteration/@Embeddable` 여부 판정
- `getGenericType(Field)` — `List<T>` 제네릭 타입 추출
- `getCount()` — `@Iteration` 반복 횟수 조회 (value 또는 countField)
- `isJavaTimePackageClass()` — `java.time.*` 클래스 여부
- `makeAccessible()` — Constructor 접근 가능하게 설정

---

### 2.5 DataAlignment (Enum)

**책임**: 데이터 정렬 및 패딩 전략 정의

**옵션**:

| 옵션  | 설명 | String 예 | byte[] 예 | 사용 시기 |
|-------|------|-----------|-----------|---------|
| LEFT  | 왼쪽 정렬, 오른쪽 패딩 | `"data____"` | 오른쪽에 0x20 추가 | 대부분의 레거시 시스템 |
| RIGHT | 오른쪽 정렬, 왼쪽 패딩 | `"____data"` | 왼쪽에 0x20 삽입 | 우정렬 시스템 |

**구현 기술** (다음은 사용될 commons-lang3 메서드입니다):
- `stringPaddingFunction`: `StringUtils::rightPad` (LEFT) 또는 `StringUtils::leftPad` (RIGHT)
- `bytesPaddingFunction`: `ArrayUtils::addAll` (LEFT) 또는 `ArrayUtils::insert` (RIGHT)
- `applyPad(String, int)` — String 정렬
- `applyPad(byte[], int, Charset)` — byte[] 정렬 (동적 패딩 생성)

**동작 방식**:
```
필드 크기: 10, 데이터: "abc"

LEFT 정렬:
  결과: "abc       " (오른쪽에 7개 공백)
  
RIGHT 정렬:
  결과: "       abc" (왼쪽에 7개 공백)
```

---

### 2.6 예외 계층 구조

**설계 원칙**: 사용자가 오류 유형에 따라 명시적으로 처리할 수 있도록 예외를 계층화

**상속 관계** (모든 예외는 RuntimeException을 상속):

```
RuntimeException
    ↓
ConvertFailException (abstract, extends RuntimeException)
    ├── ValidationException (extends ConvertFailException)
    │   └── NullInputException (extends ValidationException)
    │
    ├── InvalidAnnotationException (extends ConvertFailException)
    │   ├── MissingFormatException (extends InvalidAnnotationException)
    │   └── NegativeLengthException (extends InvalidAnnotationException)
    │
    ├── TypeConversionException (extends ConvertFailException)
    │   ├── DateParsingException (extends TypeConversionException)
    │   └── NumberParsingException (extends TypeConversionException)
    │
    └── ReflectionException (extends ConvertFailException)
        ├── FieldAccessException (extends ReflectionException)
        └── ConstructorInvocationException (extends ReflectionException)
```

**핵심 특징**:
- ✅ 모든 예외는 **RuntimeException을 상속** (checked exception 아님)
- ✅ 모든 예외는 **ConvertFailException을 상속** (catch할 수 있음)
- ✅ 특정 예외만 catch 가능 (세밀한 처리 가능)

**패키지 및 클래스 명세**:

모든 예외는 `io.github.libedi.converter.exception` 패키지에 위치:

| 예외 클래스 | 상속 관계 | 발생 원인 | 메시지 형식 |
|---------|---------|---------|----------|
| `ConvertFailException` | extends RuntimeException | (기본 예외, 직접 발생하지 않음) | "[ExceptionClassName]: [메시지]" |
| `NullInputException` | extends ValidationException | null InputStream/targetObject | "NullInputException: inputStream must not be null" |
| `ValidationException` | extends ConvertFailException | (기본 검증 예외) | "[ExceptionClassName]: [메시지]" |
| `MissingFormatException` | extends InvalidAnnotationException | @ConvertData에 format 누락 | "MissingFormatException: Date format is required for field 'xxx'" |
| `NegativeLengthException` | extends InvalidAnnotationException | @ConvertData(value < -1) | "NegativeLengthException: length must be >= -1, got '-5'" |
| `InvalidAnnotationException` | extends ConvertFailException | (기본 annotation 예외) | "[ExceptionClassName]: [메시지]" |
| `DateParsingException` | extends TypeConversionException | 날짜 parsing 실패 | "DateParsingException: Unable to parse '2024-13-32' as LocalDate with format 'yyyy-MM-dd'" |
| `NumberParsingException` | extends TypeConversionException | 숫자 parsing 실패 | "NumberParsingException: Unable to parse 'abc' as Integer" |
| `TypeConversionException` | extends ConvertFailException | (기본 타입변환 예외) | "[ExceptionClassName]: [메시지]" |
| `FieldAccessException` | extends ReflectionException | 필드 접근 실패 | "FieldAccessException: Cannot read field 'name' from 'MyClass'" |
| `ConstructorInvocationException` | extends ReflectionException | 기본 생성자 없음 | "ConstructorInvocationException: Cannot instantiate 'MyClass': no-arg constructor not found" |
| `ReflectionException` | extends ConvertFailException | (기본 reflection 예외) | "[ExceptionClassName]: [메시지]" |
| `ConstructorInvocationException` | 기본 생성자 호출 실패 | "ConstructorInvocationException: Cannot instantiate '...'" | 생성자 확인 |

**공통 특징**:
- 모두 RuntimeException 상속 (unchecked exception)
- 메시지 형식: "[예외클래스명]: [상세 메시지]"
- cause 포함: `new TypeConversionException("메시지", cause)`

---

## 2.7 Annotation 속성별 처리 로직

### @ConvertData 속성 처리

**길이 결정 알고리즘**:
1. `value ≠ 0`: value를 길이로 사용 (고정 길이)
2. `value = 0`: `lengthField` 속성으로 지정된 필드에서 길이값 읽기 (동적 길이)
3. `value = -1`: 제약 없음 - 변환시 남은 모든 바이트, 역변환시 패딩 없음 (가변 길이)

**동작 흐름**:

| 시나리오 | @ConvertData 설정 | 변환 (byte[] → Object) | 역변환 (Object → byte[]) |
|--------|------------------|----------------------|------------------------|
| 고정 길이 | `@ConvertData(10)` | 스트림에서 10바이트 읽기 | 10바이트로 패딩 적용 |
| 동적 길이 | `@ConvertData(value=0, lengthField="len")` | length 필드값만큼 읽기 | length 필드값으로 패딩 적용 |
| 가변 길이 | `@ConvertData(-1)` | 스트림 끝까지 모두 읽기 | 실제 길이만큼 (패딩 무시) |

**format 속성** (date-time 타입에만 적용):
- java.time.* 클래스 필드에서 필수
- DateTimeFormatter 생성에 사용
- 예: `@ConvertData(value=19, format="yyyy-MM-dd HH:mm:ss")`
- format이 누락되면 ConvertFailException 발생

**형식별 상세 처리 흐름**:

```
[고정 길이]
@ConvertData(10)
private String name;

변환:   바이트[0:10] → StringUtils.trim() → String
역변환: String → StringUtils.rightPad(value, 10) → 10바이트

[동적 길이]
@ConvertData(value=0, lengthField="dataLen")
private String content;
@ConvertData(4)
private int dataLen;

변환:   dataLen필드값 읽기 → 바이트[0:dataLen] → String
역변환: String → StringUtils.rightPad(value, dataLen) → dataLen바이트

[가변 길이]
@ConvertData(-1)
private String description;

변환:   바이트[0:끝] → StringUtils.trim() → String
역변환: String → 패딩 없음 → 실제 문자열의 바이트

[날짜/시간 형식]
@ConvertData(value=10, format="yyyy-MM-dd")
private LocalDate startDate;

변환:   바이트[0:10] → StringUtils.trim() → LocalDate.parse(value, formatter)
역변환: LocalDate → startDate.format(formatter) → StringUtils.rightPad(value, 10)
```

---

### @Iteration 속성 처리

**반복 횟수 결정 알고리즘**:
1. `value ≠ 0`: value를 반복 횟수로 사용 (고정 반복)
2. `value = 0`: `countField` 속성으로 지정된 필드에서 반복 횟수값 읽기 (동적 반복)

**동작 흐름**:

| 시나리오 | @Iteration 설정 | 변환 (byte[] → Object) | 역변환 (Object → byte[]) |
|--------|-----------------|----------------------|------------------------|
| 고정 반복 | `@Iteration(2)` | 2번 반복 → List<T> 요소 변환 | 2번 반복 → List 요소 역변환 |
| 동적 반복 | `@Iteration(value=0, countField="cnt")` | cnt필드값만큼 반복 변환 | cnt필드값만큼 반복 역변환 |

**처리 규칙**:
- List 크기가 지정된 반복 횟수보다 작으면: 부족한 부분은 기본 생성자로 생성한 요소로 채움 (역변환만)
- List가 null이면: 빈 List로 처리 (변환 시 available=0 체크)

**형식별 상세 처리 흐름**:

```
[고정 반복]
@Iteration(3)
private List<Item> items;

변환:   i=0,1,2 반복 → InputStream에서 Item 변환 → List에 추가
역변환: i=0,1,2 반복 → items.get(i) 역변환 → 바이트 연결

[동적 반복]
@Iteration(value=0, countField="itemCount")
private List<Item> items;
@ConvertData(4)
private int itemCount;

변환:   itemCount값 읽기 → i=0..itemCount반복 → Item 변환
역변환: i=0..itemCount반복 → items.get(i) 또는 new Item() 역변환
        (List 크기 < itemCount인 경우 기본생성자로 Item 생성)
```

---

### @Embeddable 처리

**목적**: 중첩된 Value Object 필드를 재귀적으로 처리

**규칙**:
- nested VO도 @ConvertData/@Iteration/@Embeddable을 가져야 함
- null 필드는 자동으로 기본 생성자로 인스턴스 생성 (역변환시)
- Value Object 내부 필드는 동일한 규칙으로 처리

**형식별 처리 흐름**:

```
[basic Embeddable]
@Embeddable
private Address address;

public static class Address {
    @ConvertData(50)
    private String street;
    @ConvertData(20)
    private String city;
}

변환:   address가 null이면 "street" 50바이트 + "city" 20바이트 = 70바이트 읽기
       → new Address() 생성 후 내부 필드 채우기
역변환: address가 null이면 new Address() 생성
       → street, city 각각 50, 20 바이트로 패딩해서 연결
```

---

## 3. Spring 의존성 제거 전략

v2.0에서는 Spring Framework 의존성을 완전히 제거하고 Apache Commons Lang3 3.14.0으로 대체합니다.

### 3.1 의존성 교체 매핑

| Spring 유틸 클래스 | Commons Lang3 대체 | 용도 | 영향 위치 |
|------------------|-----------------|------|---------|
| `org.springframework.util.StringUtils` | `org.apache.commons.lang3.StringUtils` | 문자열 trim, blank 체크, 패딩 | ConversionHelper, DeconversionHelper, DataAlignment |
| `org.springframework.util.ReflectionUtils` | `org.apache.commons.lang3.reflect.FieldUtils`, `MethodUtils` | 필드/메서드 접근, 접근성 설정 | ConversionHelper, DeconversionHelper, AbstractCommonHelper |
| `org.springframework.util.ClassUtils` | `org.apache.commons.lang3.ClassUtils` | 클래스 타입 체크, 원시타입 변환 | ConversionHelper, DeconversionHelper, AbstractCommonHelper |

### 3.2 상세 교체 가이드

**StringUtils 교체**:
- `StringUtils.trim()` → `StringUtils.trim()` (동일 시그니처)
- `StringUtils.isEmpty()` / `isBlank()` → `StringUtils.isEmpty()` / `isBlank()` (동일)
- `StringUtils.rightPad()` → `StringUtils.rightPad()` (동일 시그니처)
- `StringUtils.leftPad()` → `StringUtils.leftPad()` (동일 시그니처)

**ReflectionUtils 교체**:
- `ReflectionUtils.getField(class, name)` → `FieldUtils.getField(class, name, true)`
- `ReflectionUtils.setField(field, target, value)` → `FieldUtils.writeField(field, target, value, true)`
- `ReflectionUtils.getAccessibleMethod()` → `MethodUtils.getAccessibleMethod()`
- `ReflectionUtils.invokeMethod()` → `MethodUtils.invokeMethod()`

**ClassUtils 교체**:
- `ClassUtils.isAssignable(type, superType)` → `ClassUtils.isAssignable(type, superType)` (동일)
- `ClassUtils.isPrimitiveOrWrapper()` → `ClassUtils.isPrimitiveOrWrapper()` (동일)
- `ClassUtils.primitiveToWrapper()` → `ClassUtils.primitiveToWrapper()` (동일)

**ArrayUtils (신규 추가)**:
- `ArrayUtils.addAll()` — 배열 연결 (LEFT 정렬)
- `ArrayUtils.insert()` — 배열에 요소 삽입 (RIGHT 정렬)

### 3.3 pom.xml 의존성 변경

```xml
<!-- 제거 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>5.0.0.RELEASE</version>
    <scope>provided</scope>
</dependency>

<!-- 추가 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.14.0</version>
</dependency>
```

### 3.4 대체의 영향 범위

**변경 대상 파일**:
- ConversionHelper.java
- DeconversionHelper.java
- AbstractCommonHelper.java
- DataAlignment.java

**변경 불필요**:
- ByteToObjectConverter.java (Spring 사용하지 않음)
- 모든 Annotation 클래스 (Spring 사용하지 않음)
- ConvertFailException.java (Spring 사용하지 않음)

### 3.5 호환성 영향

**사용자 관점**:
- ByteToObjectConverter 공개 API는 변경 없음
- 사용자는 Spring을 의존하지 않아도 됨
- 기존 Spring 프로젝트도 계속 사용 가능

**테스트 관점**:
- 테스트 코드도 commons-lang3로 통일
- Spring Test Utilities 제거

---

## 4. 양방향 변환 흐름

### 4.1 변환 흐름 (byte[] → Object)

```
InputStream
     ↓
ConversionHelper.convert()
     ↓
1. 대상 Object 인스턴스 생성 (기본 생성자)
     ↓
2. 필드 목록 순회 (선언 순서)
     ↓
3. 필드별 처리
   ├─ @ConvertData
   │  └─ 지정 길이만큼 읽어서 타입별 변환
   ├─ @Iteration
   │  └─ count만큼 반복해서 List<T> 요소 생성
   ├─ @Embeddable
   │  └─ 재귀 호출로 nested VO 변환
   └─ 다른 annotation 없음: 무시
     ↓
4. Object 반환
     ↓
Target Object (필드 모두 채워짐)
```

### 4.2 역변환 흐름 (Object → byte[])

```
Object
     ↓
DeconversionHelper.deconvert()
     ↓
1. targetObject 유효성 검증
     ↓
2. 필드 목록 순회 (선언 순서)
     ↓
3. 필드별 처리
   ├─ @Ignorable 필드 + null
   │  └─ 0 바이트 (건너뛰기)
   ├─ @ConvertData 필드
   │  └─ 지정 길이만큼 String/byte[] 변환 후 패딩 적용
   ├─ @Iteration 필드
   │  └─ count만큼 반복해서 각 요소를 재귀 호출
   ├─ @Embeddable 필드
   │  └─ 재귀 호출로 nested VO 직렬화
   └─ 다른 annotation 없음: 무시
     ↓
4. DataAlignment 적용
   ├─ LEFT: 오른쪽 패딩
   └─ RIGHT: 왼쪽 패딩
     ↓
5. 모든 바이트 연결
     ↓
byte[] (직렬화된 데이터)
```

---

## 5. 확장 포인트 (Template Method Pattern)

### 5.1 사용자 정의 타입 지원

**시나리오**: 표준 타입이 아닌 커스텀 클래스(예: `Money`, `PhoneNumber`)를 직렬화/역직렬화하고 싶을 때

**개념적 구현 흐름** (다음은 설계 방식을 보여주는 예시입니다):

```
// 1. Converter 확장
class CustomConverter extends ByteToObjectConverter {
    
    // 2. 사용자 정의 타입 판정
    protected boolean hasAdditionalType(Class<?> fieldType) {
        return Money.class.isAssignableFrom(fieldType);
    }

    // 3. 변환: String → Money
    protected Object invokeAdditionalField(Class<?> fieldType, String value) {
        return Money.parse(value);  // "1000" → Money(1000)
    }

    // 4. 역변환: Money → String
    protected String changeAdditionalDataToString(Object fieldData) {
        return ((Money) fieldData).toAmountString();  // Money(1000) → "1000"
    }
}
```

**호출 흐름**:
1. 필드 타입 확인 → `hasAdditionalType()` 호출
2. 변환 (byte[] → Object) → `invokeAdditionalField()` 호출
3. 역변환 (Object → byte[]) → `changeAdditionalDataToString()` 호출

**메서드별 책임**:

| 메서드 | 방향 | 입력 | 출력 | 용도 |
|------|------|-----|------|------|
| `hasAdditionalType(Class<?>)` | 양방향 | 필드 타입 | boolean | 사용자 정의 타입 여부 판정 |
| `invokeAdditionalField(Class<?>, String)` | → Object | 필드타입, 문자열값 | Object | 문자열을 커스텀 타입으로 변환 |
| `changeAdditionalDataToString(Object)` | → String | 커스텀 타입 객체 | String | 커스텀 타입을 문자열로 변환 |

---

### 5.2 구현시 고려사항

**메서드 호출 순서 (매우 중요)**:

1. **`hasAdditionalType()` 호출 (항상 먼저)**
   - fieldType을 입력받아 사용자 정의 타입인지 판정
   - true 반환 → invokeAdditionalField 또는 changeAdditionalDataToString 사용
   - false 반환 → 표준 타입 처리로 폴백 (StringUtils, ClassUtils 등)

2. **`invokeAdditionalField()` 호출 (변환 방향에서만)**
   - hasAdditionalType() == true인 경우만 호출
   - convert() 중에만 호출 (deconvert() 중에는 호출 안 함)

3. **`changeAdditionalDataToString()` 호출 (역변환 방향에서만)**
   - hasAdditionalType() == true인 경우만 호출
   - deconvert() 중에만 호출 (convert() 중에는 호출 안 함)

**예외 처리**:

```
hasAdditionalType() 또는 invokeAdditionalField() 또는 changeAdditionalDataToString()에서
Exception 발생 시 → TypeConversionException으로 래핑

예:
try {
    return invokeAdditionalField(Money.class, "1000");
} catch (Exception e) {
    throw new TypeConversionException("Unable to convert to Money type", e);
}
```

**null 처리**:

| 상황 | 처리 |
|-----|------|
| invokeAdditionalField() 반환값이 null | 필드 값으로 null 설정 (권장: 예외 발생) |
| changeAdditionalDataToString() 입력값이 null | TypeConversionException 발생 |
| changeAdditionalDataToString() 반환값이 null | 기본값(빈 문자열)으로 처리 |

**타입 안전성**:

```
// 좋은 예
protected boolean hasAdditionalType(Class<?> fieldType) {
    return Money.class.isAssignableFrom(fieldType);
}

// 나쁜 예 (ClassCastException 유발 가능)
protected Object invokeAdditionalField(Class<?> fieldType, String value) {
    Money money = (Money) fieldType.getConstructor(String.class).newInstance(value);
    // 실제로는 fieldType이 항상 Money는 아니므로 위험
}

// 좋은 예
protected Object invokeAdditionalField(Class<?> fieldType, String value) {
    if (Money.class.isAssignableFrom(fieldType)) {
        return Money.parse(value);
    }
    return null;  // 또는 예외 발생
}
```

**다중 사용자 정의 타입 지원**:

```
protected boolean hasAdditionalType(Class<?> fieldType) {
    return Money.class.isAssignableFrom(fieldType) ||
           PhoneNumber.class.isAssignableFrom(fieldType);
}

protected Object invokeAdditionalField(Class<?> fieldType, String value) {
    if (Money.class.isAssignableFrom(fieldType)) {
        return Money.parse(value);
    } else if (PhoneNumber.class.isAssignableFrom(fieldType)) {
        return PhoneNumber.parse(value);
    }
    return null;
}

protected String changeAdditionalDataToString(Object fieldData) {
    if (fieldData instanceof Money) {
        return ((Money) fieldData).toAmountString();
    } else if (fieldData instanceof PhoneNumber) {
        return ((PhoneNumber) fieldData).toFormattedString();
    }
    return null;
}
```

---

## 6. 데이터 정렬 및 패딩 메커니즘

### 6.1 LEFT 정렬 (기본값)

레거시 시스템이 오른쪽 패딩을 기대하는 경우

```
@ConvertData(10)
private String name = "John";

deconvert(..., DataAlignment.LEFT)
→ "John      " (10 바이트)

구현: StringUtils.rightPad("John", 10) = "John      "
역변환 결과: J o h n [space][space][space][space][space][space]
```

---

### 6.2 RIGHT 정렬

왼쪽 정렬을 기대하는 시스템

```
@ConvertData(10)
private String name = "John";

deconvert(..., DataAlignment.RIGHT)
→ "      John" (10 바이트)

구현: StringUtils.leftPad("John", 10) = "      John"
역변환 결과: [space][space][space][space][space][space]J o h n
```

---

### 6.3 가변 길이 (value=-1)

**특징**: 패딩을 적용하지 않고 실제 데이터만 직렬화

```
@ConvertData(-1)
private String description = "Hello World";

변환 (byte[] → Object):
  InputStream의 남은 모든 바이트를 읽음
  예: 50 바이트 읽을 수 있어도 실제로는 전부 읽음

역변환 (Object → byte[]):
  deconvert(..., DataAlignment.LEFT)
  → "Hello World" (11 바이트, 패딩 없음)
  
  deconvert(..., DataAlignment.RIGHT)
  → "Hello World" (11 바이트, 패딩 없음)
  
  DataAlignment은 무시됨
```

**용도**:
- 마지막 필드가 가변 길이일 때
- 필드 길이가 사전에 정해지지 않을 때

---

## 7. 에러 처리 전략

### 7.1 ValidationException (입력값 검증 오류)

| 오류 유형 | 발생 시점 | 구체적 예외 | 메시지 | 복구 |
|---------|---------|-----------|--------|------|
| null InputStream | `convert()` 호출 | `NullInputException` | "NullInputException: inputStream must not be null" | 호출자가 null 체크 |
| null type | `convert()` 호출 | `NullInputException` | "NullInputException: type must not be null" | 호출자가 null 체크 |
| null targetObject | `deconvert()` 호출 | `NullInputException` | "NullInputException: targetObject must not be null" | 호출자가 null 체크 |
| null @Embeddable 필드 | `deconvert()` 진행 중 | (예외 없음) | — | 자동으로 기본 생성자 호출 |

**사용자 처리 예시**:
```java
try {
    converter.convert(null, MyClass.class);
} catch (NullInputException e) {
    // 입력값 검증 실패 처리
    log.error("입력값 오류: {}", e.getMessage());
}
```

---

### 7.2 InvalidAnnotationException (Annotation 설정 오류)

| 오류 유형 | 발생 시점 | 구체적 예외 | 메시지 | 복구 |
|---------|---------|-----------|--------|------|
| format 누락 | `convert/deconvert()` | `MissingFormatException` | "MissingFormatException: Date format is required for field '...' with type '...'" | @ConvertData에 format 추가 |
| 음수 length | `convert()` | `NegativeLengthException` | "NegativeLengthException: @ConvertData length must be >= -1, got '...'" | @ConvertData의 value 수정 |
| countField 누락/불일치 | `convert/deconvert()` | `InvalidAnnotationException` | "InvalidAnnotationException: countField 'xxx' not found or not int type" | @Iteration countField 확인 |
| lengthField 누락/불일치 | `convert/deconvert()` | `InvalidAnnotationException` | "InvalidAnnotationException: lengthField 'xxx' not found or not int type" | @ConvertData lengthField 확인 |

**사용자 처리 예시**:
```java
try {
    converter.convert(inputStream, MyClass.class);
} catch (MissingFormatException e) {
    // annotation 설정 오류 처리
    log.error("annotation 설정 오류: {}", e.getMessage());
}
```

---

### 7.3 TypeConversionException (타입 변환 실패)

| 오류 유형 | 발생 시점 | 구체적 예외 | 메시지 | 복구 |
|---------|---------|-----------|--------|------|
| 날짜 parsing 실패 | `convert/deconvert()` | `DateParsingException` | "DateParsingException: Unable to parse '2024-13-32' as LocalDate with format 'yyyy-MM-dd'" | 데이터 형식 확인 |
| 숫자 parsing 실패 | `convert()` | `NumberParsingException` | "NumberParsingException: Unable to parse 'abc' as Integer" | 데이터 형식 확인 |
| Enum 값 찾기 실패 | `convert()` | `TypeConversionException` | "TypeConversionException: No enum constant 'INVALID' in enum 'Week'" | 데이터 유효성 확인 |
| 사용자 정의 타입 parsing 실패 | `convert()` | `TypeConversionException` | "TypeConversionException: Unable to convert '1000-invalid' to Money type" | 사용자 정의 타입 구현 확인 |

**사용자 처리 예시**:
```java
try {
    converter.convert(inputStream, MyClass.class);
} catch (DateParsingException e) {
    // 날짜 데이터 오류 처리
    log.error("날짜 형식 오류: {}", e.getMessage());
} catch (TypeConversionException e) {
    // 기타 타입 변환 오류 처리
    log.error("타입 변환 실패: {}", e.getMessage());
}
```

---

### 7.4 ReflectionException (Reflection 작업 실패)

| 오류 유형 | 발생 시점 | 구체적 예외 | 메시지 | 복구 |
|---------|---------|-----------|--------|------|
| 필드 접근 실패 | 언제든 | `FieldAccessException` | "FieldAccessException: Cannot read field 'name' from class 'MyClass'" | 필드 접근성 확인 |
| 생성자 호출 실패 | `convert/deconvert()` | `ConstructorInvocationException` | "ConstructorInvocationException: Cannot instantiate 'MyClass': no-arg constructor not found" | 기본 생성자 확인 |
| 메서드 호출 실패 | `convert/deconvert()` | `ReflectionException` | "ReflectionException: Cannot invoke method 'valueOf' on class 'Integer'" | 메서드 시그니처 확인 |

**사용자 처리 예시**:
```java
try {
    converter.convert(inputStream, MyClass.class);
} catch (ConstructorInvocationException e) {
    // 클래스 설계 오류 (기본 생성자 없음)
    log.error("클래스 설계 오류: {}", e.getMessage());
} catch (ReflectionException e) {
    // 기타 reflection 오류
    log.error("reflection 작업 실패: {}", e.getMessage());
}
```

---

### 7.5 예외 계층 활용 가이드

**일반적 처리 (낮은 수준의 처리)**:
```java
try {
    byte[] result = converter.deconvert(obj, DataAlignment.LEFT);
} catch (ConvertFailException e) {
    // 모든 변환 오류를 일괄 처리
    log.error("변환 실패: {}", e.getMessage(), e);
}
```

**세밀한 처리 (높은 수준의 처리)**:
```java
try {
    Object result = converter.convert(inputStream, MyClass.class);
} catch (NullInputException e) {
    // 입력값 검증 오류
    return ResponseEntity.badRequest().body("입력값이 없습니다");
} catch (InvalidAnnotationException e) {
    // 설정 오류 (개발팀 수정 필요)
    log.error("annotation 설정 오류, 개발팀에 알려주세요", e);
    return ResponseEntity.status(500).body("시스템 오류");
} catch (TypeConversionException e) {
    // 데이터 오류 (사용자 입력 검증 필요)
    log.warn("데이터 형식이 맞지 않습니다: {}", e.getMessage());
    return ResponseEntity.badRequest().body("데이터 형식을 확인해주세요");
} catch (ReflectionException e) {
    // 클래스 설계 오류 (개발팀 수정 필요)
    log.error("클래스 설계 오류", e);
    return ResponseEntity.status(500).body("시스템 오류");
}
```

**각 예외 유형별 대응 전략**:
- `ValidationException`: 호출자의 입력값 검증 필요 → 사용자 UI에서 수정 요청
- `InvalidAnnotationException`: 개발팀의 annotation 설정 오류 → 개발팀이 즉시 수정
- `TypeConversionException`: 데이터가 정해진 형식과 맞지 않음 → 데이터 소스 확인
- `ReflectionException`: 클래스 설계 오류 → 개발팀이 설계 수정

---

## 8. 주요 설계 결정

| 결정 | 근거 |
|------|------|
| Helper 분리 (Conversion/Deconversion) | 단일 책임 원칙, 테스트 용이성 |
| Template method pattern | 확장성 (사용자 정의 타입 지원) |
| Runtime exception (ConvertFailException) | 가독성, 선택적 처리 |
| Charset 주입 | 다국어 지원, 유연성 |
| 필드 선언 순서 보장 | 레거시 시스템 호환성 |
| @Ignorable annotation | null 필드 안전 처리 |
| DataAlignment enum | 패딩 전략 확장 용이 |
| Spring 제거 | 라이브러리 독립성, 의존성 감소 |

---

## 9. v2.0 vs v1.x 호환성

| 항목 | v1.x | v2.0 | 호환성 |
|------|------|------|--------|
| `convert()` API | O | O (동일) | ✅ 100% |
| `@ConvertData` | O | O (동일) | ✅ 100% |
| `@Iteration` | O | O (동일) | ✅ 100% |
| `@Embeddable` | O | O (동일) | ✅ 100% |
| `deconvert()` | X | O | ✅ 신규 (비파괴) |
| `@Ignorable` | X | O | ✅ 신규 (선택) |
| `DataAlignment` | X | O | ✅ 신규 (선택) |
| Spring 의존성 | O | X | ✅ 더 독립적 |

**결론**: v1.x 코드는 변경 없이 작동. v2.0 신규 기능만 선택적 사용.

---

## 10. 승인 체크리스트

✅ design.md 검토 시 다음을 확인하세요:
- [ ] 아키텍처 다이어그램이 명확한가?
- [ ] 각 컴포넌트의 책임이 명확한가?
- [ ] Annotation 속성별 처리가 상세히 설명되었는가? (@ConvertData, @Iteration, @Embeddable, 가변길이)
  - [ ] @ConvertData(value=-1) 양방향 동작이 명확한가? (변환: 모든 바이트 읽기, 역변환: 패딩 없음)
- [ ] Spring 의존성 제거 전략이 명확한가?
- [ ] 변환/역변환 흐름이 이해하기 쉬운가?
- [ ] 확장 포인트(사용자 정의 타입) 설명이 충분한가?
  - [ ] 메서드 호출 순서 명확한가?
  - [ ] 구현시 고려사항 (예외 처리, null 처리, 타입 안전성)이 설명되었는가?
- [ ] DataAlignment 및 가변 길이 동작이 명확한가?
- [ ] 에러 처리 전략이 충분히 설명되었는가?
  - [ ] 예외 계층 구조가 명확한가? (ConvertFailException → 7가지 서브클래스)
  - [ ] 각 예외별 발생 원인과 복구 방법이 명확한가?
  - [ ] 사용자 처리 예시가 제시되었는가?
  - [ ] 세밀한 처리(각 예외별)와 일반적 처리(기본 예외) 두 가지 방식이 모두 설명되었는가?
- [ ] v1.x 호환성이 보장되는가?
- [ ] 설계와 requirements.md의 모든 요구사항이 대응되었는가?
