# Tasks: v2.0 양방향 변환 기능 - 구현 작업 순서

## 1. 개요

본 문서는 requirements.md와 design.md에서 정의한 v2.0 기능을 구현하기 위한 작업 순서를 정의합니다.

**작업 기간 예상**: 약 3~4주 (5-6개 작업, 각 3~5일)
**우선순위**: 의존성 순서에 따라 우선순위 결정

---

## 2. 작업 의존성 다이어그램

```
T-1 (버그 수정)
    ↓
T-2 (예외 클래스 구현)
    ├── T-4 (추가 테스트)
    │   ├── T-5 (README 업데이트)
    │   └── T-6 (CLAUDE.md 업데이트)
    └── T-3 (pom.xml 버전 업데이트)

* T-1과 T-2는 병렬 처리 가능 (독립적)
* T-3은 T-2 후 언제든지 가능
* T-4는 T-2 완료 후 시작
* T-5, T-6은 T-4 완료 후 시작
```

---

## 3. 상세 작업 목록

### T-1: 버그 수정 (우선순위: 🔴 높음)

**파일**: `src/main/java/io/github/libedi/converter/DeconversionHelper.java`

**작업 내용**: validateArguments() 메서드의 오류 메시지 수정

**현재 코드** (line ~68):
```java
private void validateArguments(final Object targetObject) {
    if (targetObject == null) {
        throw new ConvertFailException("targetObject must be null.");  // ❌ 버그
    }
}
```

**수정 후**:
```java
private void validateArguments(final Object targetObject) {
    if (targetObject == null) {
        throw new NullInputException("targetObject must not be null.");  // ✅ 수정
    }
}
```

**체크리스트**:
- [ ] 메서드 위치 확인 (DeconversionHelper.java:66-70)
- [ ] "must be null" → "must not be null" 변경
- [ ] ConvertFailException → NullInputException 변경 (T-2 완료 후)
- [ ] git diff 확인

**테스트**:
- [ ] 기존 테스트 통과 확인

**소요 시간**: ~15분

---

### T-2: 예외 클래스 구현 (우선순위: 🔴 높음)

**파일**: `src/main/java/io/github/libedi/converter/exception/` 폴더

**작업 내용**: 예외 계층 구조 구현

**구현 대상** (7개 클래스):

1. **ConvertFailException** (기존, 수정 필요)
   - 추상 클래스로 변경
   - message + cause 생성자 제공
   - 메시지 형식 표준화: "[ExceptionClassName]: [메시지]"

2. **ValidationException** (신규)
   - extends ConvertFailException
   - 기본 검증 예외

3. **NullInputException** (신규)
   - extends ValidationException
   - null 입력 전용

4. **InvalidAnnotationException** (신규)
   - extends ConvertFailException
   - annotation 설정 오류

5. **MissingFormatException** (신규)
   - extends InvalidAnnotationException
   - format 속성 누락

6. **NegativeLengthException** (신규)
   - extends InvalidAnnotationException
   - 음수 length 값

7. **TypeConversionException** (신규)
   - extends ConvertFailException
   - 타입 변환 실패

8. **DateParsingException** (신규)
   - extends TypeConversionException
   - 날짜 parsing 실패

9. **NumberParsingException** (신규)
   - extends TypeConversionException
   - 숫자 parsing 실패

10. **ReflectionException** (신규)
    - extends ConvertFailException
    - reflection 작업 실패

11. **FieldAccessException** (신규)
    - extends ReflectionException
    - 필드 접근 실패

12. **ConstructorInvocationException** (신규)
    - extends ReflectionException
    - 생성자 호출 실패

**구현 가이드**:

```java
// 생성자 패턴 (모든 예외 동일)
public class NullInputException extends ValidationException {
    public NullInputException(String message) {
        super(message);
    }
    
    public NullInputException(String message, Throwable cause) {
        super(message, cause);
    }
}

// ConvertFailException의 메시지 형식화
public abstract class ConvertFailException extends RuntimeException {
    public ConvertFailException(String message) {
        super(buildMessage(this.getClass().getSimpleName(), message));
    }
    
    public ConvertFailException(String message, Throwable cause) {
        super(buildMessage(this.getClass().getSimpleName(), message), cause);
    }
    
    private static String buildMessage(String className, String message) {
        return className + ": " + message;
    }
}
```

**체크리스트**:
- [ ] ConvertFailException.java 생성/수정
- [ ] ValidationException.java 생성
- [ ] NullInputException.java 생성
- [ ] InvalidAnnotationException.java 생성
- [ ] MissingFormatException.java 생성
- [ ] NegativeLengthException.java 생성
- [ ] TypeConversionException.java 생성
- [ ] DateParsingException.java 생성
- [ ] NumberParsingException.java 생성
- [ ] ReflectionException.java 생성
- [ ] FieldAccessException.java 생성
- [ ] ConstructorInvocationException.java 생성
- [ ] 메시지 형식 "[ClassName]: [메시지]" 통일
- [ ] 모든 생성자(message, message+cause) 구현
- [ ] 패키지 위치: io.github.libedi.converter.exception 확인

**테스트**:
- [ ] 컴파일 성공
- [ ] 예외 계층 구조 확인 (instanceof로 검증)

**소요 시간**: ~2시간

---

### T-3: pom.xml 버전 업데이트 (우선순위: 🟡 중간)

**파일**: `pom.xml`

**작업 내용**: 라이브러리 버전을 2.0.0으로 업데이트

**현재 코드** (line ~10):
```xml
<version>1.1.4</version>
```

**수정 후**:
```xml
<version>2.0.0</version>
```

**체크리스트**:
- [ ] pom.xml 버전 찾기
- [ ] 1.1.4 → 2.0.0 변경
- [ ] 의존성 변경 사항 확인:
  - [ ] spring-core 제거 (이미 완료되어 있을 것으로 예상)
  - [ ] commons-lang3 3.14.0 추가 (이미 완료되어 있을 것으로 예상)
- [ ] mvn verify 실행 (Maven 문법 확인)

**소요 시간**: ~10분

---

### T-4: 추가 테스트 구현 (우선순위: 🟡 중간)

**파일**: `src/test/java/io/github/libedi/converter/ByteToObjectConverterTest.java`

**작업 내용**: requirements.md NFR-3에서 정의한 5개 추가 테스트 작성

#### T-4.1: DataAlignment.RIGHT 정렬 테스트

**테스트명**: `deconvert_withDataAlignmentRIGHT()`

**검증**:
- DataAlignment.RIGHT 사용 시 왼쪽 정렬 패딩 적용 확인
- STRING 필드에 대해 좌측 패딩 확인
- byte[] 필드에 대해 좌측 패딩 확인

**예상 결과**:
```
필드 크기: 10, 데이터: "abc"
→ "       abc" (왼쪽에 7개 공백)
```

#### T-4.2: @Ignorable 필드 null 테스트 (존재 확인)

**테스트명**: `deconvert_withIgnorableNullField()`

**검증**:
- @Ignorable + null 필드는 건너뜀
- 해당 필드의 바이트가 0개

**기존 테스트**: 이미 deconvert() 테스트에 포함되어 있을 가능성

#### T-4.3: @Ignorable 필드 non-null 테스트

**테스트명**: `deconvert_withIgnorableNonNullField()`

**검증**:
- @Ignorable + null이 아닌 필드는 정상 직렬화
- @ConvertData의 길이만큼 패딩 적용

**예상 결과**:
```
@Ignorable @ConvertData(10) private String optional = "data";
→ "data______" (10바이트로 패딩)
```

#### T-4.4: null targetObject 예외 테스트

**테스트명**: `deconvert_whenTargetObjectIsNull_thenThrowNullInputException()`

**검증**:
- deconvert(null, ...) 호출 시 NullInputException 발생
- 예외 메시지 확인: "NullInputException: targetObject must not be null"

**기존 테스트**: 없음 (신규 작성 필요)

#### T-4.5: convertInputStream() 단독 테스트

**테스트명**: `convertInputStream()`

**검증**:
- convertInputStream(InputStream, int length) 단독 작동 확인
- 지정된 길이만큼 읽고 trim 처리 확인

**기존 테스트**: 없음 (신규 작성 필요)

**체크리스트**:
- [ ] T-4.1: RIGHT 정렬 테스트 작성
- [ ] T-4.2: @Ignorable null 테스트 확인 (필요시 작성)
- [ ] T-4.3: @Ignorable non-null 테스트 작성
- [ ] T-4.4: null 예외 테스트 작성
- [ ] T-4.5: convertInputStream() 테스트 작성
- [ ] 모든 테스트 통과 확인
- [ ] mvn test 실행 (전체 테스트 통과)

**테스트 실행**:
```bash
mvn test -Dtest=ByteToObjectConverterTest
```

**소요 시간**: ~3시간

---

### T-5: README 업데이트 (우선순위: 🟢 낮음)

**파일**: 
- `README.md`
- `README_kr.md` (있을 경우)

**작업 내용**: v2.0 신규 기능 문서화

**추가할 섹션**:

1. **deconvert() 사용법**
   - 기본 사용 예시
   - DataAlignment 선택 설명
   - 라운드-트립 변환 예시

2. **DataAlignment 설명**
   - LEFT vs RIGHT 패딩 차이
   - 언제 어떻게 사용할지

3. **@Ignorable 설명**
   - 목적: null 필드 선택적 처리
   - 사용 예시

4. **Spring 의존성 제거 안내**
   - v2.0부터 Spring 미필요
   - 기존 사용자는 영향 없음

5. **예외 처리 가이드**
   - ConvertFailException과 서브클래스 설명
   - 사용자별 처리 방법

**체크리스트**:
- [ ] README.md 열기
- [ ] deconvert() 섹션 추가
- [ ] DataAlignment 섹션 추가
- [ ] @Ignorable 섹션 추가
- [ ] Spring 의존성 제거 안내 추가
- [ ] 예외 처리 가이드 추가
- [ ] 코드 예시 정확성 확인
- [ ] README_kr.md 동일 내용 추가 (있을 경우)

**소요 시간**: ~1.5시간

---

### T-6: CLAUDE.md 업데이트 (우선순위: 🟢 낮음)

**파일**: `CLAUDE.md`

**작업 내용**: v2.0 변경 사항 반영

**추가/수정할 내용**:

1. **Architecture 섹션 수정**
   - Helper 분리 설명 업데이트
   - 예외 계층 구조 추가

2. **Key Configuration 섹션**
   - commons-lang3 3.14.0 추가

3. **Common Tasks 섹션**
   - "양방향 변환 사용" 새로운 섹션 추가

4. **Dependencies 섹션**
   - Spring 제거 안내
   - commons-lang3 3.14.0 추가

5. **References 섹션**
   - v2.0 설계 문서 링크 추가

**체크리스트**:
- [ ] CLAUDE.md 열기
- [ ] Architecture 섹션 수정
- [ ] Configuration 섹션 수정
- [ ] Common Tasks 섹션 추가
- [ ] Dependencies 섹션 수정
- [ ] References 섹션 수정

**소요 시간**: ~1시간

---

## 4. 작업 우선순위 및 일정

### 1단계 (Week 1)
- **T-1**: 버그 수정 (15분) — 우선순위 🔴
- **T-2**: 예외 클래스 구현 (2시간) — 우선순위 🔴

### 2단계 (Week 2)
- **T-4**: 추가 테스트 (3시간) — 우선순위 🟡
- **T-3**: pom.xml 버전 업데이트 (10분) — 우선순위 🟡

### 3단계 (Week 3)
- **T-5**: README 업데이트 (1.5시간) — 우선순위 🟢
- **T-6**: CLAUDE.md 업데이트 (1시간) — 우선순위 🟢

**총 소요 시간**: 약 8시간 (실제 개발 기간은 2~3주)

---

## 5. 전체 체크리스트

### 구현 완료 확인
- [ ] T-1: 버그 수정 완료
- [ ] T-2: 예외 클래스 12개 구현 완료
- [ ] T-3: pom.xml 버전 2.0.0 업데이트
- [ ] T-4: 5개 테스트 추가 및 통과
- [ ] T-5: README 업데이트
- [ ] T-6: CLAUDE.md 업데이트

### 빌드 및 테스트
- [ ] `mvn clean install` 성공
- [ ] 모든 테스트 통과 (`mvn test`)
- [ ] Javadoc 생성 성공 (`mvn javadoc:javadoc`)

### 문서화
- [ ] README.md 예시 코드 실행 가능 확인
- [ ] CLAUDE.md 정확성 확인

### 최종 검증
- [ ] git status 확인 (의도하지 않은 파일 변경 없음)
- [ ] git diff 검토 (의도한 변경만 포함)
- [ ] 코드 스타일 확인 (기존 스타일 유지)

---

## 6. 각 단계별 완료 기준

### T-1 완료 기준
```
- [ ] DeconversionHelper.validateArguments() 메서드 버그 수정
- [ ] 오류 메시지: "targetObject must not be null"
- [ ] 예외 타입: NullInputException
- [ ] 기존 테스트 통과
```

### T-2 완료 기준
```
- [ ] 7개 예외 클래스 구현 완료
- [ ] 예외 계층 구조 정확함
- [ ] 메시지 형식: "[ClassName]: [message]"
- [ ] 모든 생성자 구현 (message, message+cause)
- [ ] 컴파일 성공
```

### T-3 완료 기준
```
- [ ] pom.xml 버전 2.0.0
- [ ] mvn verify 성공
```

### T-4 완료 기준
```
- [ ] 5개 테스트 추가
- [ ] 모든 테스트 통과
- [ ] mvn test -Dtest=ByteToObjectConverterTest 성공
```

### T-5 완료 기준
```
- [ ] README deconvert() 섹션 추가
- [ ] README DataAlignment 섹션 추가
- [ ] README @Ignorable 섹션 추가
- [ ] README 예외 처리 가이드 추가
- [ ] 코드 예시 정확성 확인
```

### T-6 완료 기준
```
- [ ] CLAUDE.md Architecture 업데이트
- [ ] CLAUDE.md Dependencies 업데이트
- [ ] CLAUDE.md Common Tasks 신규 섹션 추가
```

---

## 7. 위험 요소 및 대응 방안

| 위험 요소 | 영향 | 대응 방안 |
|---------|------|---------|
| 기존 코드와의 호환성 | 높음 | T-4에서 기존 테스트 통과 확인 필수 |
| 예외 메시지 형식 불일치 | 중간 | 모든 예외에서 "[ClassName]: [메시지]" 형식 통일 |
| 테스트 커버리지 부족 | 중간 | T-4에서 5개 테스트 추가로 보완 |
| 문서 오류 | 낮음 | T-5, T-6에서 코드와 문서 동기화 확인 |

---

## 8. 성공 기준

모든 다음 조건을 만족할 때 v2.0 구현 완료:

1. ✅ `mvn clean install` 성공
2. ✅ 모든 테스트 통과 (기존 + 신규 5개)
3. ✅ 예외 계층 구조 구현 완료 (7개 클래스)
4. ✅ README 업데이트 (4개 섹션)
5. ✅ CLAUDE.md 업데이트
6. ✅ requirements.md 모든 수용 기준 충족
7. ✅ design.md 모든 설계 항목 구현

---

## 9. 변경 로그 및 커밋 메시지

### T-1 커밋 메시지
```
Fix: 예외 메시지 오류 수정 in DeconversionHelper

DeconversionHelper.validateArguments()의 오류 메시지를 
"must be null"에서 "must not be null"로 수정.
또한 ConvertFailException을 NullInputException으로 변경.

- DeconversionHelper: validateArguments() 메서드 수정
```

### T-2 커밋 메시지
```
Feat: 예외 계층 구조 추가 for v2.0

ConvertFailException을 추상 기본 클래스로 하는 예외 계층을 구현.
사용자가 오류 유형에 따라 명시적으로 처리 가능하도록 함.

추가 예외 클래스:
- ValidationException, NullInputException
- InvalidAnnotationException, MissingFormatException, NegativeLengthException
- TypeConversionException, DateParsingException, NumberParsingException
- ReflectionException, FieldAccessException, ConstructorInvocationException

모든 예외는 RuntimeException을 상속하며, 메시지 형식은
"[ExceptionClassName]: [message]"로 통일.
```

### T-3 커밋 메시지
```
Chore: v2.0.0 버전 업데이트

pom.xml 버전을 1.1.4에서 2.0.0으로 업데이트.
```

### T-4 커밋 메시지
```
Test: v2.0 신규 기능 테스트 추가

DataAlignment.RIGHT 정렬 테스트, @Ignorable non-null 테스트,
null targetObject 예외 테스트, convertInputStream() 테스트 등 5개 추가.

- DataAlignment.RIGHT 패딩 정렬 검증
- @Ignorable 필드 null/non-null 처리 검증
- null 입력 시 NullInputException 발생 검증
- convertInputStream() 메서드 단독 동작 검증
```

### T-5 커밋 메시지
```
Docs: v2.0 신규 기능 README 업데이트

- deconvert() 사용 예시 추가
- DataAlignment LEFT/RIGHT 설명 추가
- @Ignorable 애노테이션 설명 추가
- Spring 의존성 제거 안내 추가
- 예외 처리 가이드 추가
```

### T-6 커밋 메시지
```
Docs: v2.0 반영 CLAUDE.md 업데이트

- Architecture 섹션에 Helper 분리 및 예외 계층 추가
- Dependencies 섹션에 commons-lang3 3.14.0 추가
- Common Tasks에 양방향 변환 사용 가이드 추가
```

---

## 10. 질문 및 승인

**이 tasks.md를 승인하시겠습니까?**

승인 전 확인 사항:
- [ ] 작업 순서가 합리적인가?
- [ ] 소요 시간 예상이 적절한가?
- [ ] 모든 필수 작업이 포함되었는가?
- [ ] 의존성 관계가 정확한가?
- [ ] 체크리스트가 충분히 상세한가?
