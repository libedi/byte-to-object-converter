# Requirements: v2.0 양방향 변환 기능

## 1. 개요 (Executive Summary)

byte-to-object-converter v2.0 업데이트는 **양방향 데이터 변환**을 지원하여 
기존 byte[] → Object 변환에 더해 Object → byte[] 역변환을 가능하게 한다.
동시에 **Spring 의존성을 제거**하여 라이브러리 독립성을 높인다.

---

## 2. 기능 요구사항 (FR)

### FR-1: 양방향 데이터 변환

#### FR-1.1 기존 기능: byte[] → Object 변환 (하위 호환성 유지)

**현황**: ✅ 완료 (기존 v1.x 구현 유지)

- **메서드**: `ByteToObjectConverter#convert(InputStream inputStream, Class<T> type) : T`
- **동작**: 입력 스트림의 바이트를 읽어 `@ConvertData`, `@Iteration`, `@Embeddable` 애노테이션 규칙에 따라 Object 생성
- **호환성**: 기존 사용자 코드는 변경 없이 동작
- **수용 기준**:
  - [ ] 기존 convert() API의 입출력이 동일하게 작동한다
  - [ ] 기존 테스트가 모두 통과한다

#### FR-1.2 신규 기능: Object → byte[] 역변환

**현황**: 🔄 기본 구현 완료, 정제 및 테스트 보강 필요

- **메서드**: `ByteToObjectConverter#deconvert(Object targetObject, DataAlignment alignment) : byte[]`
- **매개변수**:
  - `targetObject`: 직렬화할 Object (null이 아니어야 함)
  - `alignment`: 데이터 정렬 방식 (DataAlignment.LEFT 또는 DataAlignment.RIGHT)
- **동작**:
  - Object의 모든 애노테이션 필드를 읽어 바이트 배열로 변환
  - `@ConvertData` 필드: 지정된 길이만큼 직렬화
  - `@Iteration` 필드: List 원소 개수만큼 반복 직렬화
  - `@Embeddable` 필드: 재귀적으로 직렬화
  - `@Ignorable` 필드: null이면 건너뜀 (0 바이트), 값이 있으면 정상 직렬화
  - 길이 부족분: alignment 옵션에 따라 공백 패딩 적용
- **예외**: null targetObject 입력 시 ConvertFailException 발생
- **미완료 사항**:
  - [ ] validateArguments() 오류 메시지 버그 수정: "must be null" → "must not be null"
  - [ ] @ConvertData(value=-1) 시 역변환 동작 명확화 필요
- **수용 기준**:
  - [ ] deconvert() 메서드가 정의되고 공개된다
  - [ ] null targetObject 입력 시 ConvertFailException을 정확한 메시지로 발생시킨다
  - [ ] 모든 지원 타입이 올바르게 직렬화된다
  - [ ] 라운드-트립 변환(convert → deconvert → convert)이 원본과 동일한 결과를 생성한다

---

### FR-2: 데이터 정렬(패딩) 지원

**현황**: 🔄 기본 구현 완료, RIGHT 정렬 테스트 부재

- **타입**: `io.github.libedi.converter.DataAlignment` (public enum)
- **옵션**:
  - `LEFT`: 데이터를 왼쪽에 정렬, 오른쪽에 공백 패딩 추가
    - String: `"data____"` (StringUtils.rightPad 사용)
    - byte[]: 오른쪽에 공백 바이트 추가
  - `RIGHT`: 데이터를 오른쪽에 정렬, 왼쪽에 공백 패딩 추가
    - String: `"____data"` (StringUtils.leftPad 사용)
    - byte[]: 왼쪽에 공백 바이트 삽입
- **적용 범위**: 역변환(deconvert) 시에만 사용됨
- **현재 테스트**: LEFT 정렬만 검증됨
- **수용 기준**:
  - [ ] DataAlignment enum이 LEFT와 RIGHT 옵션을 제공한다
  - [ ] deconvert(object, DataAlignment.LEFT)가 왼쪽 정렬 패딩을 적용한다
  - [ ] deconvert(object, DataAlignment.RIGHT)가 오른쪽 정렬 패딩을 적용한다
  - [ ] String과 byte[] 타입 모두에 패딩이 정확히 적용된다

---

### FR-3: @Ignorable 애노테이션

**현황**: 🔄 기본 구현 완료, null이 아닌 필드 테스트 부재

- **위치**: `io.github.libedi.converter.annotation.Ignorable`
- **목적**: 역변환(deconvert) 시 선택적 필드 처리
- **적용**: `@ConvertData`와 함께 사용
- **동작**:
  - 역변환 중 필드 값이 null이고 `@Ignorable`이 표기되어 있으면 해당 필드를 건너뜀
  - 필드 값이 null이 아니면 `@ConvertData` 길이에 따라 정상 직렬화
  - `@Ignorable` 없이 null 필드는 ConvertFailException 발생
- **현재 테스트**: null 필드만 검증됨
- **수용 기준**:
  - [ ] @Ignorable이 마커 애노테이션으로 정의된다
  - [ ] @Ignorable 필드가 null이면 직렬화되지 않는다
  - [ ] @Ignorable 필드가 값을 가지면 정상적으로 직렬화된다
  - [ ] @Ignorable 없이 null 필드는 ConvertFailException을 발생시킨다

---

### FR-4: 예외 계층화 및 일원화

**현황**: 🔄 설계 완료, 구현 필요

- **기본 클래스**: `io.github.libedi.converter.exception.ConvertFailException extends RuntimeException`
- **목적**: 변환/역변환 오류를 명시적으로 분류하여 사용자가 오류 유형에 따라 처리 가능하도록 함

**예외 계층** (모든 예외는 RuntimeException을 상속):

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

**각 예외별 특징**:
- ✅ **모두 RuntimeException을 상속** (unchecked exception)
- ✅ **모두 message + cause 생성자 제공**
- ✅ **메시지 형식**: `"[ExceptionClassName]: [상세 메시지]"`
- ✅ **패키지**: `io.github.libedi.converter.exception`

**오류 유형별 발생**:

| 오류 유형 | 예외 클래스 | 발생 조건 |
|--------|-----------|---------|
| null InputStream/type | `NullInputException` | convert(null, ...) 호출 |
| null targetObject | `NullInputException` | deconvert(null, ...) 호출 |
| @ConvertData에 format 누락 | `MissingFormatException` | date-time 필드에서 format 없음 |
| @ConvertData value < -1 | `NegativeLengthException` | 음수 length 지정 |
| 날짜 parsing 실패 | `DateParsingException` | 형식 불일치 날짜 데이터 |
| 숫자 parsing 실패 | `NumberParsingException` | 형식 불일치 숫자 데이터 |
| 기본 생성자 호출 실패 | `ConstructorInvocationException` | 클래스에 no-arg 생성자 없음 |
| 필드 접근 실패 | `FieldAccessException` | private 필드 접근 실패 |

- **버그**:
  - [ ] DeconversionHelper.validateArguments() 오류 메시지 수정: "must be null" → "must not be null"
- **수용 기준**:
  - [ ] 모든 변환 오류가 ConvertFailException의 서브클래스로 발생한다
  - [ ] 모든 역변환 오류가 ConvertFailException의 서브클래스로 발생한다
  - [ ] null 입력 오류는 NullInputException으로 발생한다
  - [ ] annotation 설정 오류는 InvalidAnnotationException으로 발생한다
  - [ ] 타입 변환 실패는 TypeConversionException으로 발생한다
  - [ ] reflection 작업 실패는 ReflectionException으로 발생한다
  - [ ] 예외 메시지가 "[클래스명]: [상세메시지]" 형식을 따른다

---

### FR-5: Spring 의존성 제거

**현황**: ✅ 완료 (의존성 교체 완료)

- **변경사항**:
  - 제거: `spring-core` (provided scope)
  - 추가: `commons-lang3 3.14.0` (runtime scope)
- **영향 범위**:
  - 리플렉션 유틸: `ReflectionUtils` → `FieldUtils`, `MethodUtils`
  - 문자열 유틸: `StringUtils` → `StringUtils` (Commons Lang)
  - 클래스 유틸: `ClassUtils` → `ClassUtils` (Commons Lang)
- **라이브러리 사용자 영향**: Spring을 의존하지 않는 사용자도 사용 가능
- **수용 기준**:
  - [ ] pom.xml에서 spring-core 의존성이 제거된다
  - [ ] commons-lang3 3.14.0이 추가된다
  - [ ] 모든 Spring 유틸 호출이 Commons Lang 호출로 대체된다
  - [ ] 모든 테스트가 통과한다

---

## 3. 비기능 요구사항 (NFR)

### NFR-1: 버전 관리
- **현재 버전**: 1.1.4
- **업데이트 버전**: 2.0.0
- **위치**: pom.xml의 `<version>` 태그
- **수용 기준**:
  - [ ] pom.xml 버전이 2.0.0으로 업데이트된다

### NFR-2: Java 호환성
- **최소 버전**: Java 8
- **수용 기준**:
  - [ ] 코드가 Java 8 이상에서 컴파일되고 실행된다

### NFR-3: 테스트 커버리지
- **기존 테스트** (3개, ByteToObjectConverterTest.java에 존재):
  - `convert()`: Happy path (다양한 타입 포함)
  - `convert_whenNoDataThenReturnEmptyListField()`: Edge case (빈 List)
  - `deconvert()`: 라운드-트립 기본 테스트 (DataAlignment.LEFT만 테스트)
  
- **추가 필요 테스트**:
  - [ ] `deconvert_withDataAlignmentRIGHT()`: RIGHT 정렬 패딩 검증
  - [ ] `deconvert_withIgnorableNullField()`: null 필드 건너뜀 검증
  - [ ] `deconvert_withIgnorableNonNullField()`: null이 아닌 @Ignorable 필드 정상 직렬화
  - [ ] `deconvert_whenTargetObjectIsNull_thenThrowConvertFailException()`: null 입력 예외 및 메시지 검증
  - [ ] `convertInputStream_withLength()`: convertInputStream() 메서드 단독 테스트
  - [ ] 확장 포인트 테스트 (선택사항): 사용자 정의 타입 변환

- **수용 기준**:
  - [ ] 기존 3개 테스트가 모두 통과한다
  - [ ] 추가 5개 테스트가 모두 통과한다

### NFR-4: 하위 호환성
- **변경 금지**:
  - `convert()` API 시그니처
  - `@ConvertData`, `@Iteration`, `@Embeddable` 애노테이션 동작
  - 기존 공개 메서드
- **수용 기준**:
  - [ ] 기존 v1.x 코드가 변경 없이 동작한다

---

## 4. 지원 타입

**v2.0에서 지원하는 필드 타입** (v1.x와 동일):
- `byte[]`
- `String`
- Enum 타입
- Primitive 및 Wrapper 클래스 (int, long, double, boolean, Integer, Long 등)
- `java.time` 패키지 (LocalDate, LocalDateTime, LocalTime, ZonedDateTime, OffsetDateTime, OffsetTime, Year, YearMonth, Month)
- 사용자 정의 타입 (확장 포인트 via hasAdditionalType, invokeAdditionalField, changeAdditionalDataToString)
- `java.util.List<T>` (제네릭 타입 지정 필수)
- 사용자 정의 Value Object (nested VO, `@Embeddable`)

**향후 버전에서 지원 예정** (v2.1 이상):
- `java.nio.ByteBuffer`
- Netty `ByteBuf`

---

## 5. 제약사항 (Constraints)

### C-1: 기본 생성자 요구
- 변환/역변환 대상 클래스는 기본 생성자(no-arg constructor)를 반드시 포함
- private 접근자도 허용됨

### C-2: 필드 선언 순서
- 변환/역변환 필드의 순서는 클래스 필드 선언 순서를 따름
- 순서 변경 불가능

### C-3: 길이 필드 타입
- `lengthField`, `countField`로 참조되는 필드는 반드시 `int` 타입이어야 함

### C-4: List 제네릭 타입 지정
- `@Iteration` 필드는 반드시 `List<T>` 형태로 제네릭 타입을 명시해야 함

---

## 6. 구현 상태 정리

| 항목 | 상태 | 비고 |
|------|------|------|
| 아키텍처 리팩토링 (Helper 분리) | ✅ 완료 | |
| Spring 의존성 제거 & commons-lang3 추가 | ✅ 완료 | |
| `deconvert()` 메서드 구현 | 🔄 완료 | 버그 수정 필요 |
| `DataAlignment` enum | 🔄 완료 | RIGHT 정렬 테스트 필요 |
| `@Ignorable` 애노테이션 | 🔄 완료 | non-null 필드 테스트 필요 |
| 예외 계층 구조 설계 | ✅ 설계 완료 | 구현 필요: ConvertFailException, ValidationException, InvalidAnnotationException, TypeConversionException, ReflectionException 및 서브클래스 |
| 기본 테스트 (3개) | ✅ 완료 | convert, deconvert, empty list |
| **버그 수정: validateArguments 오류 메시지** | ❌ 미완료 | T-1: 우선순위 높음 |
| **예외 클래스 구현 (7개)** | ❌ 미완료 | T-2: 우선순위 높음 (ConvertFailException, ValidationException, InvalidAnnotationException, TypeConversionException, ReflectionException 및 서브클래스) |
| **pom.xml 버전 2.0.0 업데이트** | ❌ 미완료 | T-3 |
| **추가 테스트 (5개)** | ❌ 미완료 | T-4 |
| **README.md / README_kr.md 업데이트** | ❌ 미완료 | T-5 |
| **CLAUDE.md 업데이트** | ❌ 미완료 | T-6 |

---

## 7. 승인 기준

✅ requirements.md 승인 시 다음을 모두 만족해야 함:
- [ ] 모든 FR(FR-1 ~ FR-5)이 명확하게 기술되었는가?
- [ ] 각 FR마다 수용 기준(AC)이 객관적으로 정의되었는가?
- [ ] 기존 구현(완료), 정제 필요, 미구현이 명확하게 구분되었는가?
- [ ] 지원 타입 목록이 명확한가?
- [ ] 제약사항이 구체적인가?
- [ ] 기존 사용자에 미치는 영향이 명확한가?
