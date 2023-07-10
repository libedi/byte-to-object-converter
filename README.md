# **byte-to-object-converter**
***byte-to-object-converter*** makes it easy to convert data from a byte array to an Object. 

Still existing legacy code specializes in byte array data. When converting such byte array data to Object, a lot of boilerplate code is required for data parsing and field type conversion.  
***byte-to-object-converter*** reduces these tedious tasks and helps developers focus on the design of their business domain.

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
**`ByteToObjectConverter`** can be used to convert data from a byte array to an Object.  
**`ByteToObjectConverter`** takes as a constructor parameter the charset of the data you want to convert.  
After that, convert the data in the byte array to an InputStream, and **`ByteToObjectConverter.convert()`** convert it to the desired Object using as follows.
~~~java
Charset dataCharset = Charset.forName("UTF-8");
ByteToObjectConverter converter = new ByteObjectConverter(dataCharset);

InputStream inputStream = new ByteArrayInputStream(bytesData);
CustomType object = converter.convert(inputStream, CustomType.class);
~~~

Use the following annotations to specify the data in the byte array as the field you want to convert in Object:
- **`@ConvertData`**
- **`@Iteration`**
- **`@Embeddable`**

### **1. `@ConvertData`**
#### ***Annotation to specify which fields to transform***
By default, you specify transform fields with the **`@CovertData`** annotation.  
The field conversion order follows the order of the fields declared in the class.  
The length of the byte data to be converted is specified with the **`value`** attribute of the **`@ConvertData`** annotation. If the data length of the field is specified as the value of another field, the data length is specified with the **`lengthField`** attribute, the field name is specified as `String`, and the type of the specified field must be `int`.  
If the conversion field type is a date-time type, the format of the data can be specified with the **`format`** attribute.

Usage is as follows:
~~~java
// String data whose length is 14 bytes
@ConvertData(14)
String string;

// int type data whose data length is 4 bytes
@ConvertData(4)
int length;

// byte[] data whose length is equal to the value of the length field
@ConvertData(lengthField = "length")
byte[] bytes;

// Date-type data whose length is 8 bytes
@ConvertData(value = 8, format = "yyyyMMdd")
LocalDate date;
~~~

### **2. `@Iteration`**
#### ***Annotation for converting repeated fields with `List` type***
For repeated fields of `List` type, use the **`@Iteration`** annotation.  
The number of repetitions is specified by the **`value`** attribute, which specifies a fixed number of repetitions, and the **`countField`** attribute, which repeats according to the value of another field. **`countField`** specifies the field name as `String`, and the field type must be `int`.  
Fields specified with the **`@Iteration`** annotation must specify the generic type of `List`. Fields in the class of that generic type must be annotated with **`@ConvertData`**, **`@Iteration`** or **`@Embeddable`**.

Usage is as follows:
~~~java
// Data with a fixed number of iterations of 3
@Iteration(3)
List<VO> fixedIterationList;

@ConvertData(4)
int count;

// Data that repeats as many times as the value of the count field
@Iteration(countField = "count")
List<VO> fieldIterationList;
~~~

### **3. `@Embeddable`**
#### ***Annotations to transform custom Value Object fields***
To increase data cohesion, you can use a user-defined value object as a field of the target object.  
Fields inside VO must be annotated with **`@ConvertData`**, **`@Iteration`** or **`@Embeddable`**.

Usage is as follows:
~~~java
@Embeddable
CustomVo customVo;
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
    <version>1.0.1</version>
</dependency>
~~~
- ### **Gradle**
~~~groovy
implementation 'io.github.libedi:byte-to-object-converter:1.0.1'
~~~
