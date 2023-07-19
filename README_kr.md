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
- `List`
- 사용자 정의 Value Object

| :exclamation: important |
|:-------------------------|
| 대상 Object는 반드시 기본 생성자를 갖고 있어야 합니다. (private 접근자도 가능) |

## **사용 방법**
**`ByteToObjectConverter`** 를 사용하여 byte 배열의 데이터를 Object로 변환할 수 있습니다.  
**`ByteToObjectConverter`** 는 생성자 매개변수로 변환하려는 데이터의 문자셋을 받습니다.  
그 후, byte 배열의 데이터를 `InputStream`으로 변환하고, 다음과 같이 **`ByteToObjectConverter.convert()`** 를 사용하여 원하는 Object로 변환합니다.
~~~java
Charset dataCharset = Charset.forName("UTF-8");
ByteToObjectConverter converter = new ByteObjectConverter(dataCharset);

InputStream inputStream = new ByteArrayInputStream(bytesData);
CustomType object = converter.convert(inputStream, CustomType.class);
~~~

byte 배열의 데이터를 Object 내 변환하려는 필드로 지정하기 위해 아래의 애노테이션을 사용합니다:
- **`@ConvertData`**
- **`@Iteration`**
- **`@Embeddable`**

### **1. `@ConvertData`**
#### ***변환할 필드를 지정하기 위한 애노테이션***
기본적으로 **`@CovertData`** 애노테이션으로 변환 필드를 지정합니다.  
필드 변환 순서는 클래스에 선언한 필드 순서를 따릅니다.  
변환할 byte 데이터의 길이는 **`@ConvertData`** 애노테이션의 **`value`** 속성으로 지정합니다. 만약, 필드의 데이터 길이가 다른 필드의 값으로 지정되어 있는 경우, **`lengthField`** 속성으로 데이터 길이가 지정되 필드명을 `String` 으로 지정하며, 지정된 필드의 타입은 반드시 `int` 여야 합니다.  
변환 필드의 타입이 date-time 타입이면, **`format`** 속성으로 데이터의 포맷을 지정할 수 있습니다.

사용법은 아래와 같습니다:
~~~java
public class CustomType {

    // 데이터의 길이가 14 byte인 문자열 데이터
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

    // ...
}
~~~

### **2. `@Iteration`**
#### ***`List` 타입을 가진 반복되는 필드 변환을 위한 애노테이션***
`List` 타입을 가진 반복되는 필드의 경우, **`@Iteration`** 애노테이션으로 지정합니다.  
반복 횟수는 고정 반복 횟수를 지정하는 **`value`** 속성과, 다른 필드값에 따라 반복되는 **`countField`** 속성으로 지정합니다. **`countField`** 는 필드명을 `String` 으로 지정하며, 해당 필드의 타입은 반드시 `int` 여야 합니다.  
**`@Iteration`** 애노테이션이 지정한 필드는 `List` 의 제네릭 타입을 반드시 지정해야 합니다. 해당 제네릭 타입의 클래스 내 필드는 반드시 **`@ConvertData`**, **`@Iteration`** 또는 **`@Embeddable`** 애노테이션이 지정되어 있어야 합니다.

사용법은 아래와 같습니다:
~~~java
public class CustomType {
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
public class CustomType {
    // ...

    @Embeddable
    CustomVo customVo;

    // ...
}
~~~

## **최소 사양**
- Java 8 이상
- Spring 5.0 이상

## **설치**
- ### **Maven**
~~~xml
<dependency>
    <groupId>io.github.libedi</groupId>
    <artifactId>byte-to-object-converter</artifactId>
    <version>1.1.3</version>
</dependency>
~~~
- ### **Gradle**
~~~groovy
implementation 'io.github.libedi:byte-to-object-converter:1.1.3'
~~~
