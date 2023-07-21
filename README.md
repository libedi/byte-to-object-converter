# **Byte to Object Converter**
***byte-to-object-converter*** simplifies the process of converting data from a byte array into an Object. 

If you're still using byte array data telegrams in your old legacy code, converting such byte array data to Object can require plenty of boilerplate code for parsing and field type conversion.  
***byte-to-object-converter*** minimizes these tedious tasks and allows developers to focus on designing their business domain.

## **Support Type**
***byte-to-object-converter*** supports various field types when converting Object.  
Supported types are:
- `byte[]`
- `String`
- Enum Type
- Primitive Type or Wrapper class such as `int` / `long` / `double` except `void`
- `java.time` package's date-time class
- User-defined type (not Value Object)
- `List`
- Custom Value Object

| :exclamation: important |
|:-------------------------|
| The target Object must have a default constructor. (private accessors are also available) |

## **How to use**
**`ByteToObjectConverter`** is a tool that enables the conversion of data from a byte array to an Object.  
To use **`ByteToObjectConverter`** , provide the charset of the data you want to convert as a constructor parameter. If the current system and the connected system use the same character set, you can use the default constructor.  
**`ByteToObjectConverter`** is created as follows:
~~~java
// Constructor Parameter : java.nio.charset.Charset
Charset dataCharset = Charset.forName("UTF-8");
ByteToObjectConverter converter = new ByteObjectConverter(dataCharset);

// Constructor Parameter : String
String dataCharset = "UTF-8";
ByteToObjectConverter converter = new ByteObjectConverter(dataCharset);

// Default Constructor : System default character set
ByteToObjectConverter converter = new ByteObjectConverter();
~~~
Once you've done that, convert the data in the byte array into an `InputStream`. Then, use **`ByteToObjectConverter.convert()`** to transform the data into the desired Object, as shown below.
~~~java
InputStream inputStream = new ByteArrayInputStream(bytesData);
CustomObject object = converter.convert(inputStream, CustomObject.class);
~~~
For converting user-defined types that are not default supported types, you can inherit **`ByteToObjectConverter`** and define the method to convert between the user-defined type.
~~~java
import org.springframework.util.ClassUtils;

public class CustomTypeToObjectConverter extends ByteToObjectConverter {
    // Constructor ...

    @Override
    protected boolean hasAdditionalType(Class<?> fieldType) throws Exception {
        return ClassUtils.isAssignable(CustomType.class, fieldType);
    }

    @Override
    protected Object invokeAdditionalField(Class<?> fieldType, String value) throws Exception {
        return ReflectionUtils.invokeMethod(fieldType.getMethod("parse", String.class), null, value);
    }
}
~~~

Use the following annotations to specify the data in the byte array as the field you want to convert in Object:
- **`@ConvertData`**
- **`@Iteration`**
- **`@Embeddable`**

### **1. `@ConvertData`**
#### ***Annotation to specify which fields to transform***
By default, you define transform fields using the **`@ConvertData`** annotation.  
The order of field conversion follows the sequence in which the fields are declared within the class.  
The length of the byte data to be converted is determined by the **`value`** attribute of the **`@ConvertData`** annotation. If the data length of the field is specified as the value of another field, you can set the data length using the **`lengthField`** attribute, providing the field name as `String`, and ensuring that the specified field's type is an integer.
For fields with date-time type conversions, you can specify the data format using the **`format`** attribute.

Usage is as follows:
~~~java
public class CustomType {

    // String data with a length of 14 bytes
    @ConvertData(14)
    String string;

    // integer type data with a length of 4 bytes
    @ConvertData(4)
    int length;

    // byte[] data with a length equal to the value of the length field
    @ConvertData(lengthField = "length")
    byte[] bytes;

    // Date-type data with a length of 8 bytes
    @ConvertData(value = 8, format = "yyyyMMdd")
    LocalDate date;

    // ...
}
~~~

### **2. `@Iteration`**
#### ***Annotation for converting repeated fields with `List` type***
For `List` type fields with repeated elements, use the **`@Iteration`** annotation.  
The number of repetitions is determined either by the **`value`** attribute, which sets a fixed number of repetitions, or by the **`countField`** attribute, which repeats based on the value of another field. The **`countField`** specifies the field name as `String`, and the field type must be an integer.  
Fields marked with the **`@Iteration`** annotation must specify the generic type of `List`. Fields in the class of that generic type must be annotated with **`@ConvertData`**, **`@Iteration`** or **`@Embeddable`**.

Usage is as follows:
~~~java
public class CustomType {
    // ...

    // Data with a fixed number of iterations of 3
    @Iteration(3)
    List<VO> fixedIterationList;

    @ConvertData(4)
    int count;

    // Data that repeats as many times as the value of the count field
    @Iteration(countField = "count")
    List<VO> fieldIterationList;

    // ...
}
~~~

### **3. `@Embeddable`**
#### ***Annotations to transform custom Value Object fields***
To enhance data cohesion, you can utilize a user-defined value object (VO) as a field of the target object.  
Fields within the value object must be annotated with **`@ConvertData`**, **`@Iteration`** or **`@Embeddable`**.

Usage is as follows:
~~~java
public class CustomType {
    // ...

    @Embeddable
    CustomVo customVo;

    // ...
}
~~~

## **Requirements**
- Java 8 or later
- Spring 5.0+

## **Installation**
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
