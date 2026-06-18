# **Byte to Object Converter**
***byte-to-object-converter*** 는 byte 배열의 데이터를 Object로 쉽게 변환해줍니다. 

아직도 기존 레거시 코드에서는 byte 배열 데이터를 전문으로 사용하고 있습니다. 이러한 byte 배열의 데이터를 Object로 변환시 데이터 파싱 및 필드 타입 변환에 많은 boilerplate 코드가 필요합니다.  
***byte-to-object-converter*** 는 이러한 번거로운 작업을 줄이고, 개발자가 비즈니스 도메인의 설계에 집중할 수 있게 도와줍니다.

## **지원 타입**
***byte-to-object-converter*** 는 Object 변환시 다양한 필드 타입을 지원합니다.  
지원하는 타입은 아래와 같습니다:
- `byte[]`
- `String`
- Enum Type
- `int` / `long` / `double` 등 `void` 를 제외한 Primitive Type / Wrapper 클래스
- `java.time` 패키지의 date-time 클래스
- 사용자 정의 타입 (Value Object가 아닌)
- `java.util.List`
- 사용자 정의 Value Object

| :exclamation: 중요 |
|:-------------------------|
| 대상 Object는 반드시 기본 생성자를 갖고 있어야 합니다. (private 접근자도 가능) |
| v2.0.0부터 Spring Framework 의존성이 제거되었습니다. 라이브러리는 이제 **Apache Commons Lang 3.14.0**을 사용합니다. |

## **사용 방법**
**`ByteToObjectConverter`** 를 사용하여 byte 배열의 데이터를 Object로 변환할 수 있습니다.  
**`ByteToObjectConverter`** 는 변환하려는 데이터의 캐릭터셋을 생성자 매개변수로 받습니다. 만약 현재 시스템과 연동 시스템이 동일한 캐릭터셋을 사용하다면, 기본 생성자를 사용할 수 있습니다.  
**`ByteToObjectConverter`** 는 다음과 같이 생성합니다.
~~~java
// 생성자 매개변수 : java.nio.charset.Charset
Charset dataCharset = Charset.forName("UTF-8");
ByteToObjectConverter converter = new ByteObjectConverter(dataCharset);

// 생성자 매개변수 : String
String dataCharset = "UTF-8";
ByteToObjectConverter converter = new ByteObjectConverter(dataCharset);

// 기본 생성자 : 시스템 기본 캐릭터셋
ByteToObjectConverter converter = new ByteObjectConverter();
~~~
그 후, byte 배열의 데이터를 `java.io.InputStream`으로 변환하고, 다음과 같이 **`ByteToObjectConverter.convert()`** 를 사용하여 원하는 Object로 변환합니다.
~~~java
InputStream inputStream = new ByteArrayInputStream(bytesData);
TargetObject object = converter.convert(inputStream, TargetObject.class);
~~~

반대로 Object를 byte 배열로 역변환할 수 있습니다. **`ByteToObjectConverter.deconvert()`** 를 **`DataAlignment`** 매개변수와 함께 사용하여 문자열 패딩을 제어합니다.
~~~java
TargetObject object = new TargetObject();
// ... object 필드 설정 ...
byte[] result = converter.deconvert(object, DataAlignment.LEFT);
~~~

기본 지원 타입이 아닌 사용자 정의 타입을 변환할 경우, **`ByteToObjectConverter`** 를 상속받아 사용자 정의 타입과 변환하는 방법을 정의할 수 있습니다.
~~~java
public class CustomTypeToObjectConverter extends ByteToObjectConverter {
    // Constructor ...

    @Override
    protected boolean hasAdditionalType(Class<?> fieldType) throws Exception {
        return CustomType.class.isAssignableFrom(fieldType);
    }

    @Override
    protected Object invokeAdditionalField(Class<?> fieldType, String value) throws Exception {
        return fieldType.getMethod("parse", String.class).invoke(null, value);
    }
}
~~~

byte 배열의 데이터를 Object 내 변환하려는 필드로 지정하기 위해 아래의 애노테이션을 사용합니다:
- **`@ConvertData`**
- **`@Iteration`**
- **`@Embeddable`**

### **1. `@ConvertData`**
#### ***변환할 필드를 지정하기 위한 애노테이션***
기본적으로 **`@CovertData`** 애노테이션으로 변환 필드를 지정합니다.  
필드 변환 순서는 클래스에 선언한 필드 순서를 따릅니다.  
변환할 byte 데이터의 길이는 **`@ConvertData`** 애노테이션의 **`value`** 속성으로 지정합니다. 만약, 필드의 데이터 길이가 다른 필드의 값으로 지정되어 있는 경우, **`lengthField`** 속성으로 데이터 길이가 지정된 필드명을 `String` 으로 지정하며, 지정된 필드의 타입은 반드시 `int`/`Integer` 여야 합니다.

역변환(Object를 byte 배열로) 시 `DataAlignment`를 사용하여 문자열 패딩을 제어할 수 있습니다:
- **`DataAlignment.LEFT`**: 좌측 정렬, 우측 패딩 (예: `"value     "`)
- **`DataAlignment.RIGHT`**: 우측 정렬, 좌측 패딩 (예: `"     value"`)
- **가변 길이** (`value=-1`): 패딩 미적용 (예: `"value"`)

변환 필드의 타입이 date-time 타입이면, **`format`** 속성으로 데이터의 포맷을 지정할 수 있습니다.

사용법은 아래와 같습니다:
~~~java
public class TargetObject {

    // 데이터의 길이가 14 byte인 문자열 데이터 (LEFT 정렬: "value     ")
    @ConvertData(14)
    String string;

    // 데이터의 길이가 4 byte인 int 형 데이터
    @ConvertData(4)
    int length;

    // 데이터의 길이가 length 필드의 값만큼 가진 byte[] 데이터
    @ConvertData(lengthField = "length")
    byte[] bytes;

    // 데이터의 길이가 8 byte인 날짜형 데이터
    @ConvertData(value = 8, format = "yyyyMMdd")
    LocalDate date;

    // 가변 길이의 문자열 데이터 (패딩 미적용)
    @ConvertData(-1)
    String variable;

    // ...
}
~~~

### **2. `@Iteration`**
#### ***`java.util.List` 타입을 가진 반복되는 필드 변환을 위한 애노테이션***
`java.util.List` 타입을 가진 반복되는 필드의 경우, **`@Iteration`** 애노테이션으로 지정합니다.  
반복 횟수는 고정 반복 횟수를 지정하는 **`value`** 속성과, 다른 필드값에 따라 반복되는 **`countField`** 속성으로 지정합니다. **`countField`** 는 필드명을 `String` 으로 지정하며, 해당 필드의 타입은 반드시 `int`/`Integer` 여야 합니다.  
**`@Iteration`** 애노테이션이 지정한 필드는 `java.util.List` 의 제네릭 타입을 반드시 지정해야 합니다. 해당 제네릭 타입의 클래스 내 필드는 반드시 **`@ConvertData`**, **`@Iteration`** 또는 **`@Embeddable`** 애노테이션이 지정되어 있어야 합니다.

사용법은 아래와 같습니다:
~~~java
import java.util.List;

public class TargetObject {
    // ...

    // 반복 횟수가 3으로 고정된 데이터
    @Iteration(3)
    List<VO> fixedIterationList;

    @ConvertData(4)
    int count;

    // count 필드의 값만큼 반복하는 데이터
    @Iteration(countField = "count")
    List<VO> fieldIterationList;

    // ...
}
~~~

### **3. `@Embeddable`**
#### ***사용자 정의 Value Object 필드를 변환하기 위한 애노테이션***
데이터의 응집도를 높이기 위해 대상 Object의 필드로 사용자 정의 Value Object를 사용할 수 있습니다.  
VO 내부 필드는 반드시 **`@ConvertData`**, **`@Iteration`** 또는 **`@Embeddable`** 애노테이션이 지정되어 있어야 합니다.

사용법은 아래와 같습니다:
~~~java
public class TargetObject {
    // ...

    @Embeddable
    CustomVo customVo;

    // ...
}

public class CustomVo {

    @ConvertData
    String voField1;

    // ...
}
~~~

### **4. `@Ignorable`**
#### ***역변환 시 null 필드를 스킵하기 위한 애노테이션***
**`@Ignorable`** 애노테이션은 Object를 byte 배열로 변환할 때(역변환) 사용됩니다. **`@Ignorable`** 로 표시된 필드의 값이 null이면, 고정 길이가 지정되어 있어도 해당 필드는 직렬화 중에 스킵됩니다. 필드가 null이 아니면 정상적으로 직렬화됩니다.

사용법은 아래와 같습니다:
~~~java
public class TargetObject {
    // ...

    // optional이 null이면, 역변환 시 이 필드는 스킵됨
    @Ignorable
    @ConvertData(10)
    String optional;

    // description이 null이 아니면 정상적으로 직렬화됨
    @Ignorable
    @ConvertData(lengthField = "descLength")
    String description;

    // ...
}
~~~

## **예외 처리**

v2.0.0부터 converter는 오류 처리를 위한 구조화된 예외 계층을 제공합니다:

- **`ConvertFailException`** - 모든 변환/역변환 실패에 대한 기본 추상 예외
  - **`ValidationException`** - 입력 검증 실패 시 발생 (예: null 입력)
  - **`InvalidAnnotationException`** - 애노테이션 설정이 잘못되었을 때 발생
  - **`TypeConversionException`** - 필드 타입 변환(파싱) 실패 시 발생
  - **`ReflectionException`** - 리플렉션 작업(필드 접근 또는 생성자 호출) 실패 시 발생

사용 예제:
~~~java
try {
    InputStream inputStream = new ByteArrayInputStream(bytesData);
    TargetObject object = converter.convert(inputStream, TargetObject.class);
} catch (ValidationException e) {
    // 입력 검증 오류 처리
    System.err.println("Invalid input: " + e.getMessage());
} catch (TypeConversionException e) {
    // 타입 변환 오류 처리
    System.err.println("Type conversion failed: " + e.getMessage());
} catch (ReflectionException e) {
    // 리플렉션 오류 처리
    System.err.println("Reflection error: " + e.getMessage());
} catch (ConvertFailException e) {
    // 기타 변환 오류 처리
    System.err.println("Conversion failed: " + e.getMessage());
}
~~~

## **최소 사양**
- Java 8 이상
- Apache Commons Lang 3.14.0 (자동으로 의존성에 포함됨)

## **설치**
- ### **Maven**
~~~xml
<dependency>
    <groupId>io.github.libedi</groupId>
    <artifactId>byte-to-object-converter</artifactId>
    <version>2.0.0</version>
</dependency>
~~~
- ### **Gradle**
~~~groovy
implementation 'io.github.libedi:byte-to-object-converter:2.0.0'
~~~

## **변경 사항**

### **v2.0.0** (최신 버전)
- **신규**: Object를 byte 배열로 변환하는 `deconvert()` 메서드 추가
- **신규**: 역변환 중 문자열 패딩을 제어하는 `DataAlignment` enum 추가 (LEFT/RIGHT)
- **신규**: 역변환 중 null 필드를 스킵하는 `@Ignorable` 애노테이션 추가
- **주요 변경**: Spring Framework 의존성 제거 (기존: Spring 5.0+)
- **개선**: Spring 유틸리티를 Apache Commons Lang 3.14.0으로 교체
- **개선**: 더 나은 오류 처리를 위해 구조화된 예외 계층 추가
