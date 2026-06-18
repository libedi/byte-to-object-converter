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
- `java.util.List`
- Custom Value Object

| :exclamation: Important |
|:-------------------------|
| The target Object must have a default constructor. (private accessors are also available) |
| Since v2.0.0, Spring Framework dependency has been removed. The library now uses **Apache Commons Lang 3.14.0** for utility functions. |

## **How to use**
**`ByteToObjectConverter`** is a tool that enables the conversion of data from a byte array to an Object.  
To use **`ByteToObjectConverter`** , provide the character set of the data you want to convert as a constructor parameter. If the current system and the connected system use the same character set, you can use the default constructor.  
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
Once you've done that, convert the data in the byte array into an `java.io.InputStream`. Then, use **`ByteToObjectConverter.convert()`** to transform the data into the desired Object, as shown below.
~~~java
InputStream inputStream = new ByteArrayInputStream(bytesData);
TargetObject object = converter.convert(inputStream, TargetObject.class);
~~~

Conversely, you can convert an Object back to a byte array using **`ByteToObjectConverter.deconvert()`** with a **`DataAlignment`** parameter to control string padding.
~~~java
TargetObject object = new TargetObject();
// ... set object fields ...
byte[] result = converter.deconvert(object, DataAlignment.LEFT);
~~~

For converting user-defined types that are not default supported types, you can inherit **`ByteToObjectConverter`** and define the method to convert between the user-defined type.
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

Use the following annotations to specify the data in the byte array as the field you want to convert in Object:
- **`@ConvertData`**
- **`@Iteration`**
- **`@Embeddable`**

### **1. `@ConvertData`**
#### ***Annotation to specify which fields to transform***
By default, you define transform fields using the **`@ConvertData`** annotation.  
The order of field conversion follows the sequence in which the fields are declared within the class.  
The length of the byte data to be converted is determined by the **`value`** attribute of the **`@ConvertData`** annotation. If the data length of the field is specified as the value of another field, you can set the data length using the **`lengthField`** attribute, providing the field name as `String`, and ensuring that the specified field's type is an integer.

For deconversion (Object to byte array), you can control string padding using `DataAlignment`:
- **`DataAlignment.LEFT`**: Left-aligns data with right padding (e.g., `"value     "`)
- **`DataAlignment.RIGHT`**: Right-aligns data with left padding (e.g., `"     value"`)
- **Variable length** (`value=-1`): No padding applied (e.g., `"value"`)

For fields with date-time type conversions, you can specify the data format using the **`format`** attribute.

Usage is as follows:
~~~java
public class TargetObject {

    // String data with a length of 14 bytes (LEFT alignment: "value     ")
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

    // String data with variable length (no padding)
    @ConvertData(-1)
    String variable;

    // ...
}
~~~

### **2. `@Iteration`**
#### ***Annotation for converting repeated fields with `java.util.List` type***
For `java.util.List` type fields with repeated elements, use the **`@Iteration`** annotation.  
The number of repetitions is determined either by the **`value`** attribute, which sets a fixed number of repetitions, or by the **`countField`** attribute, which repeats based on the value of another field. The **`countField`** specifies the field name as `String`, and the field type must be an integer.  
Fields marked with the **`@Iteration`** annotation must specify the generic type of `java.util.List`. Fields in the class of that generic type must be annotated with **`@ConvertData`**, **`@Iteration`** or **`@Embeddable`**.

Usage is as follows:
~~~java
import java.util.List;

public class TargetObject {
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
#### ***Annotation to skip null fields during deconversion***
The **`@Ignorable`** annotation is used when converting an Object to a byte array (deconversion). When a field marked with **`@Ignorable`** has a null value, the field is skipped during serialization even if a fixed length is specified. If the field is not null, it is serialized normally.

Usage is as follows:
~~~java
public class TargetObject {
    // ...

    // If optional is null, this field is skipped during deconversion
    @Ignorable
    @ConvertData(10)
    String optional;

    // If description is not null, it is serialized normally
    @Ignorable
    @ConvertData(lengthField = "descLength")
    String description;

    // ...
}
~~~

## **Exception Handling**

Since v2.0.0, the converter provides a structured exception hierarchy for error handling:

- **`ConvertFailException`** - Base abstract exception for all conversion/deconversion failures
  - **`ValidationException`** - Raised when input validation fails (e.g., null input)
  - **`InvalidAnnotationException`** - Raised when annotation configuration is incorrect
  - **`TypeConversionException`** - Raised when field type conversion (parsing) fails
  - **`ReflectionException`** - Raised when reflection operations (field access or constructor invocation) fail

Usage example:
~~~java
try {
    InputStream inputStream = new ByteArrayInputStream(bytesData);
    TargetObject object = converter.convert(inputStream, TargetObject.class);
} catch (ValidationException e) {
    // Handle input validation error
    System.err.println("Invalid input: " + e.getMessage());
} catch (TypeConversionException e) {
    // Handle type conversion error
    System.err.println("Type conversion failed: " + e.getMessage());
} catch (ReflectionException e) {
    // Handle reflection error
    System.err.println("Reflection error: " + e.getMessage());
} catch (ConvertFailException e) {
    // Handle other conversion errors
    System.err.println("Conversion failed: " + e.getMessage());
}
~~~

## **Requirements**
- Java 8 or later
- Apache Commons Lang 3.14.0 (automatically included as a dependency)

## **Installation**
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

## **Changelog**

### **v2.0.0** (Latest)
- **New**: Added `deconvert()` method to convert Objects back to byte arrays
- **New**: Added `DataAlignment` enum to control string padding (LEFT/RIGHT) during deconversion
- **New**: Added `@Ignorable` annotation to skip null fields during deconversion
- **Breaking Change**: Removed Spring Framework dependency (was Spring 5.0+)
- **Enhancement**: Replaced Spring utilities with Apache Commons Lang 3.14.0
- **Improved**: Structured exception hierarchy with specific exception types for better error handling
