# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**byte-to-object-converter** (v2.0.0) is a Java library that provides bidirectional conversion between byte arrays and Objects. It simplifies handling data from legacy systems that use byte array data telegrams, supporting both serialization (byte[] → Object) and deserialization (Object → byte[]).

This is a published Maven library distributed via Maven Central (groupId: `io.github.libedi`).

## Architecture

### Core Components

**Main facade class**: [ByteToObjectConverter.java](src/main/java/io/github/libedi/converter/ByteToObjectConverter.java)
- Provides `convert(InputStream, Class<T>)` for serialization (byte[] → Object)
- Provides `deconvert(Object, DataAlignment)` for deserialization (Object → byte[]) **[NEW in v2.0]**
- Accepts a `Charset` (or defaults to system charset) to handle character encoding
- Extensible: subclass and override `hasAdditionalType()` and `invokeAdditionalField()` to support custom types

**Helper classes**:
- `ConversionHelper` — handles byte[] → Object conversion; core reflection-based field population
- `DeconversionHelper` — handles Object → byte[] conversion; applies padding via `DataAlignment` **[NEW in v2.0]**
- `AbstractCommonHelper` — shared reflection utilities for both conversion directions

**Padding & Alignment**: [DataAlignment.java](src/main/java/io/github/libedi/converter/DataAlignment.java) **[NEW in v2.0]**
- `RIGHT` — right-aligns data with left padding (e.g., "____data")
- `LEFT` — left-aligns data with right padding (e.g., "data____")
- Applied during deconversion to match fixed-length field requirements

**Annotations**: Four annotations define how fields are converted:
- `@ConvertData` — marks fields to convert; specifies byte length (fixed value or via `lengthField`), optional date `format`, and alignment strategy
- `@Iteration` — marks `List<T>` fields; specifies repetition count (fixed via `value` or dynamic via `countField`)
- `@Embeddable` — marks user-defined Value Object fields; VO internal fields must also be annotated
- `@Ignorable` — marks fields to skip during deconversion if value is null; useful for optional fields **[NEW in v2.0]**

### Type Support

Supported field types:
- `byte[]`
- `String` 
- Enum types
- Primitive types and wrapper classes (all except `void`)
- Java 8+ date-time classes (`LocalDate`, `LocalDateTime`, `ZonedDateTime`, etc.)
- User-defined custom types (via converter extension)
- `java.util.List<T>` with generic type specified
- Custom Value Objects

**Constraints**:
- Target object and all Value Objects must have a default constructor (can be private)
- Fields are converted in declaration order
- Field classes referenced must be on classpath

### Conversion Flow

#### Serialization (byte[] → Object)

1. User creates `ByteToObjectConverter` with charset
2. Wraps byte array in `ByteArrayInputStream`
3. Calls `converter.convert(inputStream, TargetClass.class)`
4. `ConversionHelper` inspects target class fields via reflection
5. For each `@ConvertData` field: reads bytes according to length, converts to field type (handles String, primitives, dates, enums, etc.)
6. For each `@Iteration` field: loops N times, recursively converts list element type
7. For each `@Embeddable` field: recursively converts the Value Object
8. Returns fully populated target object

#### Deserialization (Object → byte[]) **[NEW in v2.0]**

1. User creates `ByteToObjectConverter` with charset
2. Calls `converter.deconvert(sourceObject, DataAlignment.LEFT)` or `DataAlignment.RIGHT`
3. `DeconversionHelper` inspects source object fields via reflection
4. For each `@ConvertData` field: converts field value to bytes, applies padding based on `DataAlignment`
5. For each field marked with `@Ignorable`: if value is null, skips conversion (no bytes written)
6. For each `@Iteration` field: loops over list items, recursively converts each element
7. For each `@Embeddable` field: recursively deconverts the Value Object
8. Concatenates all byte arrays and returns result

## Development

### Build & Test

```bash
# Build the library (compile, run tests, package)
mvn clean install

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ByteToObjectConverterTest

# Run a single test method
mvn test -Dtest=ByteToObjectConverterTest#testMethodName

# Build without running tests
mvn clean install -DskipTests
```

### Project Structure

```
src/
├── main/java/io/github/libedi/converter/
│   ├── ByteToObjectConverter.java       (main facade for bidirectional conversion)
│   ├── ConversionHelper.java            (byte[] → Object)
│   ├── DeconversionHelper.java          (Object → byte[]) [NEW in v2.0]
│   ├── AbstractCommonHelper.java        (shared reflection utilities)
│   ├── DataAlignment.java               (padding strategy enum) [NEW in v2.0]
│   ├── annotation/
│   │   ├── ConvertData.java
│   │   ├── Iteration.java
│   │   ├── Embeddable.java
│   │   └── Ignorable.java               [NEW in v2.0]
│   └── exception/
│       ├── ConvertFailException.java    (abstract base exception)
│       ├── ValidationException.java     └─ NullInputException
│       ├── InvalidAnnotationException.java  ├─ MissingFormatException
│       │                                     └─ NegativeLengthException
│       ├── TypeConversionException.java ├─ DateParsingException
│       │                                     └─ NumberParsingException
│       └── ReflectionException.java     ├─ FieldAccessException
│                                             └─ ConstructorInvocationException
└── test/java/io/github/libedi/converter/
    └── ByteToObjectConverterTest.java
```

### Dependencies

**Runtime** (bundled with the library):
- Apache Commons Lang 3.14.0 **[NEW in v2.0]**
  - `StringUtils` — string padding and manipulation
  - `ArrayUtils` — byte array operations
  - `FieldUtils`, `MethodUtils` — safe reflection utilities
  - `ClassUtils` — class introspection

**Test-only**:
- JUnit 5.9.3 (Jupiter)
- Mockito 5.3.1
- AssertJ 3.24.2
- AutoParams 1.1.1 (parameterized test generation)
- Lombok 1.18.28 (test annotations)
- Spring Test 5.0.0.RELEASE
- Commons Lang 3.12.0 (test utilities)

**Note**: Spring Framework is NO LONGER a runtime dependency as of v2.0. Commons Lang3 replaces Spring's utility classes.

### Testing Notes

- Uses AutoParams to auto-generate test parameters
- Tests are in `ByteToObjectConverterTest.java`
- To add new tests: follow existing patterns using `@ExtendWith(AutoArgumentsSource.class)` with Lombok-annotated test data classes

### Key Configuration

**pom.xml properties**:
- Java version: 1.8 (source and target)
- Project version: 2.0.0
- Character encoding: UTF-8

**Build plugins**:
- Maven Enforcer: enforces Java 1.8+
- Maven Compiler: targets Java 1.8
- Maven GPG, Javadoc, Source, Nexus Staging: for publishing to Maven Central

## Exception Handling **[NEW in v2.0]**

The library uses a structured exception hierarchy for detailed error reporting:

```
ConvertFailException (abstract base)
├── ValidationException
│   └── NullInputException — null input stream or object
├── InvalidAnnotationException
│   ├── MissingFormatException — @ConvertData field missing format for date-time type
│   └── NegativeLengthException — invalid negative length in @ConvertData
├── TypeConversionException
│   ├── DateParsingException — failed to parse date-time value
│   └── NumberParsingException — failed to parse numeric value
└── ReflectionException
    ├── FieldAccessException — failed to access field via reflection
    └── ConstructorInvocationException — failed to invoke default constructor
```

All exceptions are unchecked (`extends RuntimeException`). Catch specific exception types for precise error handling:

```java
try {
    converter.convert(input, MyClass.class);
} catch (DateParsingException e) {
    // handle date parsing error
} catch (NullInputException e) {
    // handle null input
} catch (ConvertFailException e) {
    // handle other conversion failures
}
```

## Common Tasks

### Serialization (Object → byte[]) **[NEW in v2.0]**

Convert a populated object back to byte array with padding:

```java
ByteToObjectConverter converter = new ByteToObjectConverter(StandardCharsets.UTF_8);
MyObject obj = new MyObject();
// ... populate fields ...

// LEFT alignment: "data____"
byte[] result = converter.deconvert(obj, DataAlignment.LEFT);

// RIGHT alignment: "____data"
byte[] result = converter.deconvert(obj, DataAlignment.RIGHT);
```

Fields marked with `@Ignorable` will be skipped if their value is `null`:

```java
@Embeddable
public class MyObject {
    @ConvertData(10)
    private String required;
    
    @Ignorable
    @ConvertData(20)
    private String optional;  // skipped if null during deconversion
}
```

### Deserialization (byte[] → Object)

Convert byte array to object with automatic field population:

```java
ByteToObjectConverter converter = new ByteToObjectConverter(StandardCharsets.UTF_8);
byte[] data = ...;
InputStream input = new ByteArrayInputStream(data);
MyObject result = converter.convert(input, MyObject.class);
```

### Adding Support for a New Type

Extend `ByteToObjectConverter` and override two methods:

```java
public class CustomTypeConverter extends ByteToObjectConverter {
    public CustomTypeConverter(Charset charset) {
        super(charset);
    }

    @Override
    protected boolean hasAdditionalType(Class<?> fieldType) throws Exception {
        return MyCustomType.class.isAssignableFrom(fieldType);
    }

    @Override
    protected Object invokeAdditionalField(Class<?> fieldType, String value) throws Exception {
        return fieldType.getMethod("parse", String.class).invoke(null, value);
    }
}
```

### Testing Conversion Logic

Create a target class with annotations, wrap test byte data in `ByteArrayInputStream`, and call `converter.convert()`. The converter will populate all annotated fields. Verify via assertions on the populated object.

### Code Style & Conventions

- Package: `io.github.libedi.converter`
- Class naming: `*Converter` for converter classes, `*Helper` for internal helpers, annotations use `@*` (PascalCase), exceptions use `*Exception` (PascalCase)
- Comments: Javadoc on public classes and public methods (Korean comments in source are acceptable for internal logic)
- Null safety: Use Apache Commons Lang3's `StringUtils`, `ArrayUtils`, `FieldUtils`, and `MethodUtils` for safe operations

## Publishing

This library is published to Maven Central. The pom.xml is configured for:
- Automatic GPG signing (signs artifacts on build)
- Javadoc and source JAR generation
- Nexus Staging (auto-release after close)

To publish a new version, update the version in pom.xml and push—the CI/CD pipeline handles Maven Central publication via Sonatype OSSRH.

## References

- README: Comprehensive user guide with usage examples and annotation details
- GitHub: https://github.com/libedi/byte-to-object-converter
- Maven Central: https://mvnrepository.com/artifact/io.github.libedi/byte-to-object-converter
