package io.github.libedi.converter;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.github.libedi.converter.annotation.ConvertData;
import io.github.libedi.converter.annotation.Embeddable;
import io.github.libedi.converter.annotation.Ignorable;
import io.github.libedi.converter.annotation.Iteration;
import io.github.libedi.converter.exception.ConvertFailException;

/**
 * <p>
 * byte 데이터와 Object 간의 변환을 쉽게 해줍니다.
 * </p>
 * <p>
 * 기존 레거시 코드에서 byte 전문과 Object 간의 변환시 데이터 파싱에 많은 boilerplate 코드가 필요합니다.
 * {@link ByteToObjectConverter}는 이러한 번거로운 작업을 줄이고, 개발자가 비즈니스 도메인의 설게에 집중할 수 있게
 * 도와줍니다.
 * </p>
 * <p>
 * Object내 다양한 타입을 지원합니다. 지원되는 타입은 아래와 같습니다:
 * </p>
 * <ul>
 * <li><code>byte[]</code></li>
 * <li>{@link String}</li>
 * <li>Enum Type</li>
 * <li><code>int</code>/<code>long</code>/<code>double</code> 등
 * <code>void</code>를 제외한 Primitive Type / Wrapper class</li>
 * <li>java.time 패키지의 date-time class</li>
 * <li>사용자 정의 타입</li>
 * <li>{@link List}
 * <li>사용자 정의 Value Object</li>
 * </ul>
 * <p>
 * 대상 Object는 반드시 기본 생성자를 갖고 있어야 합니다. (private 접근자도 가능)
 * </p>
 * 
 * <p>
 * 기본적으로 {@link ConvertData @ConvertData} 애노테이션으로 변환 필드를 지정합니다. 필드 변환 순서는 클래스에
 * 정의한 필드 순서를 따릅니다. 데이터의 길이는 {@link ConvertData @ConvertData} 애노테이션의
 * {@link ConvertData#value() value} 속성으로 지정합니다. 만약, 데이터의 길이가 다른 필드값에 정의되어 있다면,
 * {@link ConvertData#lengthField() lengthField} 속성으로 해당 필드명을
 * <code>String</code>으로 지정하며, 해당하는 필드의 타입은 반드시 <code>int</code>여야 합니다. 변환 필드의
 * 타입이 date-time 타입이면, {@link ConvertData#format() format} 속성으로 데이터의 format을 지정할
 * 수 있습니다.
 * </p>
 * <p>
 * <code>List</code> 타입을 가진 반복되는 필드의 경우, {@link Iteration @Iteration} 애노테이션으로
 * 지정합니다. 반복 횟수는 고정 반복 횟수를 지정하는 {@link Iteration#value() value} 속성과 다른 필드값에 따라
 * 반복되는 {@link Iteration#countField() countField} 속성으로 지정합니다.
 * {@link Iteration#countField() countField} 는 필드명을 <code>String</code>으로 지정하며,
 * 해당 필드의 타입은 반드시 <code>int</code>여야 합니다.<br/>
 * {@link Iteration @Iteration} 애노테이션이 지정한 필드는 반드시 <code>List</code>의 제네릭 타입을
 * 지정해야 합니다. 해당 제네릭 타입의 클래스 필드는 반드시 {@link ConvertData @ConvertData},
 * {@link Iteration @Iteration} 또는 {@link Embeddable @Embeddable} 애노테이션이 지정되어
 * 있어야 합니다.
 * </p>
 * <p>
 * 대상 Object의 필드로 사용자 정의 Value Object를 사용할 수 있습니다. 해당 필드는
 * {@link Embeddable @Embeddable} 로 지정합니다. VO 내부 필드는 반드시
 * {@link ConvertData @ConvertData}, {@link Iteration @Iteration} 또는
 * {@link Embeddable @Embeddable} 애노테이션이 지정되어 있어야 합니다.
 * </p>
 * <p>
 * {@link Ignorable @Ignorable} 은 Object를 <code>byte[]</code>로 변환시 사용되며, 해당
 * 애노테이션이 지정된 필드는 필드의 값이 null인 경우, 길이가 지정되어 있어도 해당 필드의 변환을 무시합니다.
 * </p>
 * 
 * @author "Sangjun, Park"
 * 
 * @see ConvertData
 * @see Iteration
 * @see Embeddable
 * @see Ignorable
 */
public class ByteToObjectConverter {

    private final ConversionHelper conversionHelper;
    private final DeconversionHelper deconversionHelper;

    /**
     * <p>
     * 시스템 기본 Charset으로 converter를 생성합니다.
     * </p>
     *
     * @see #ByteToObjectConverter(String)
     * @see #ByteToObjectConverter(Charset)
     */
    public ByteToObjectConverter() {
        this(Charset.defaultCharset());
    }

    /**
     * <p>
     * 지정된 Charset 이름으로 converter를 생성합니다.
     * </p>
     *
     * @param dataCharset byte 데이터를 문자열로 변환할 때 사용할 Charset 이름
     *                    (예: "UTF-8", "EUC-KR")
     * @throws IllegalCharsetNameException Charset 이름이 유효하지 않은 경우
     * @see #ByteToObjectConverter(Charset)
     */
    public ByteToObjectConverter(final String dataCharset) {
        this(Charset.forName(dataCharset));
    }

    /**
     * <p>
     * 지정된 {@link Charset}으로 converter를 생성합니다.
     * </p>
     * <p>
     * 이 Charset은 byte 데이터를 문자열로 변환할 때 사용됩니다.
     * 변환하려는 데이터의 인코딩과 일치하는 Charset을 지정해야 합니다.
     * </p>
     *
     * @param dataCharset byte 데이터 인코딩에 사용된 Charset
     * @see #ByteToObjectConverter()
     * @see #ByteToObjectConverter(String)
     */
    public ByteToObjectConverter(final Charset dataCharset) {
        final Function<Class<?>, Boolean> hasAdditionalTypeFunction = hasAdditionalTypeFunction();

        conversionHelper = new ConversionHelper(dataCharset, hasAdditionalTypeFunction,
                invokeAdditionalFieldFunction());
        deconversionHelper = new DeconversionHelper(dataCharset, hasAdditionalTypeFunction,
                changeAdditionalDataToStringFunction());
    }

    /**
     * <p>
     * {@link java.io.InputStream}에서 byte 데이터를 읽어 지정된 타입의 Object로 변환합니다.
     * </p>
     * <p>
     * 변환 과정:
     * </p>
     * <ol>
     * <li>입력값 검증 (inputStream, type이 null이 아닌지 확인)</li>
     * <li>대상 타입의 기본 생성자로 인스턴스 생성</li>
     * <li>클래스 필드 순서대로 annotation 정보 확인</li>
     * <li>{@link ConvertData @ConvertData} 필드: 지정 길이만큼 읽고 필드 타입으로 변환</li>
     * <li>{@link Iteration @Iteration} 필드: 반복 횟수만큼 재귀 호출</li>
     * <li>{@link Embeddable @Embeddable} 필드: 중첩 Object로 재귀 호출</li>
     * </ol>
     * <p>
     * <strong>사용 예:</strong>
     * </p>
     * <pre>
     * ByteToObjectConverter converter = new ByteToObjectConverter("UTF-8");
     * InputStream input = new FileInputStream("data.bin");
     * MyObject obj = converter.convert(input, MyObject.class);
     * </pre>
     *
     * @param <T> 변환 대상 타입
     * @param inputStream byte 데이터 입력 스트림. null이 아니어야 함
     * @param type 변환 대상 클래스. null이 아니어야 함.
     *             기본 생성자(private 가능)를 반드시 가져야 함
     * @return 변환된 Object 인스턴스
     * @throws NullInputException inputStream 또는 type이 null인 경우
     * @throws ConstructorInvocationException 기본 생성자 호출 실패 시
     * @throws FieldAccessException 필드 접근/할당 실패 시
     * @throws TypeConversionException 타입 변환 실패 시
     * @throws InvalidAnnotationException annotation 설정 오류 시
     * @throws ConvertFailException 그 외 변환 실패 시
     * @see ConvertData
     * @see Iteration
     * @see Embeddable
     */
    public <T> T convert(final InputStream inputStream, final Class<T> type) {
        return conversionHelper.convert(inputStream, type);
    }

    /**
     * <p>
     * {@link java.io.InputStream}에서 지정된 길이만큼 byte를 읽어 문자열로 변환합니다.
     * </p>
     * <p>
     * 이 메서드는 특정 길이의 byte 데이터를 문자열로 변환할 필요가 있을 때
     * 편의상 제공됩니다. 읽은 문자열 앞뒤의 공백은 제거됩니다.
     * </p>
     * <p>
     * <strong>사용 예:</strong>
     * </p>
     * <pre>
     * ByteToObjectConverter converter = new ByteToObjectConverter("UTF-8");
     * InputStream input = new FileInputStream("data.bin");
     * String header = converter.convertInputStream(input, 20);  // 20 byte 읽어 문자열로 변환
     * </pre>
     *
     * @param inputStream byte 데이터 입력 스트림
     * @param length 읽을 byte 길이. 0 이상이어야 함
     * @return trim된 문자열. 읽은 byte가 없으면 빈 문자열("")
     * @throws ConvertFailException length가 음수이거나 읽기 실패 시
     * @see #convert(InputStream, Class)
     */
    public String convertInputStream(final InputStream inputStream, final int length) {
        return conversionHelper.convertInputStream(inputStream, length);
    }

    /**
     * <p>
     * Object를 byte[] 데이터로 역변환합니다.
     * </p>
     * <p>
     * 이 메서드는 Object의 필드 값을 byte[] 형식으로 직렬화합니다.
     * {@link ConvertData @ConvertData}의 길이와 {@link DataAlignment}에 따라
     * 패딩이 적용됩니다.
     * </p>
     * <p>
     * 역변환 과정:
     * </p>
     * <ol>
     * <li>입력값 검증 (targetObject가 null이 아닌지 확인)</li>
     * <li>클래스 필드 순서대로 처리</li>
     * <li>{@link Ignorable @Ignorable} 필드: null이면 스킵</li>
     * <li>{@link ConvertData @ConvertData} 필드: 지정 길이로 패딩 후 변환</li>
     * <li>{@link Iteration @Iteration} 필드: 각 요소를 재귀 호출하여 변환</li>
     * <li>{@link Embeddable @Embeddable} 필드: 중첩 Object를 재귀 호출하여 변환</li>
     * <li>모든 byte[]를 순서대로 연결</li>
     * </ol>
     * <p>
     * <strong>사용 예:</strong>
     * </p>
     * <pre>
     * ByteToObjectConverter converter = new ByteToObjectConverter("UTF-8");
     * MyObject obj = new MyObject();
     * obj.id = 12345;
     * obj.name = "John";
     *
     * // 왼쪽 정렬 (문자열 용)
     * byte[] resultLeft = converter.deconvert(obj, DataAlignment.LEFT);
     *
     * // 오른쪽 정렬 (숫자 용)
     * byte[] resultRight = converter.deconvert(obj, DataAlignment.RIGHT);
     * </pre>
     *
     * @param targetObject 역변환할 Object. null이 아니어야 함
     * @param alignment 데이터 정렬 방식
     *                  ({@link DataAlignment#LEFT} - 왼쪽 정렬,
     *                  {@link DataAlignment#RIGHT} - 오른쪽 정렬)
     * @return 역변환된 byte[] 배열
     * @throws NullInputException targetObject가 null인 경우
     * @throws FieldAccessException 필드 읽기 실패 시
     * @throws TypeConversionException 타입 변환 실패 시
     * @throws ConvertFailException 그 외 역변환 실패 시
     * @see DataAlignment
     * @see ConvertData
     * @see Iteration
     * @see Embeddable
     * @see Ignorable
     */
    public byte[] deconvert(final Object targetObject, final DataAlignment alignment) {
        return deconversionHelper.deconvert(targetObject, alignment);
    }

    /**
     * <p>
     * 주어진 클래스가 converter에서 지원하는 사용자 정의 타입인지 판별합니다.
     * </p>
     * <p>
     * 기본 구현은 {@code false}를 반환하므로, 사용자 정의 타입을 지원하려면
     * 이 메서드를 override하여 구현해야 합니다.
     * </p>
     * <p>
     * <strong>사용 예:</strong>
     * </p>
     * <pre>
     * public class CustomConverter extends ByteToObjectConverter {
     *     public CustomConverter() {
     *         super();
     *     }
     *
     *     &#64;Override
     *     protected boolean hasAdditionalType(Class&lt;?&gt; fieldType) throws Exception {
     *         return MyCustomType.class.isAssignableFrom(fieldType);
     *     }
     *
     *     &#64;Override
     *     protected Object invokeAdditionalField(Class&lt;?&gt; fieldType, String value) throws Exception {
     *         if (MyCustomType.class.isAssignableFrom(fieldType)) {
     *             return MyCustomType.parse(value);
     *         }
     *         return super.invokeAdditionalField(fieldType, value);
     *     }
     * }
     * </pre>
     *
     * @param fieldType 검사할 필드 타입
     * @return {@code true}이면 사용자 정의 타입, {@code false}이면 기본 타입
     * @throws Exception 검사 중 발생할 수 있는 예외
     * @see #invokeAdditionalField(Class, String)
     */
    protected boolean hasAdditionalType(final Class<?> fieldType) throws Exception {
        return false;
    }

    /**
     * <p>
     * byte[] 데이터를 Object로 변환할 때 사용자 정의 타입의 필드 값을 설정합니다.
     * </p>
     * <p>
     * 이 메서드는 {@link #hasAdditionalType(Class)}가 {@code true}를 반환한
     * 경우에만 호출됩니다. byte 데이터를 문자열로 변환한 후 이를
     * 사용자 정의 타입의 값으로 변환하는 로직을 구현해야 합니다.
     * </p>
     * <p>
     * <strong>사용 예:</strong>
     * </p>
     * <pre>
     * public class CustomConverter extends ByteToObjectConverter {
     *     &#64;Override
     *     protected Object invokeAdditionalField(Class&lt;?&gt; fieldType, String value)
     *             throws Exception {
     *         if (MyCustomType.class.isAssignableFrom(fieldType)) {
     *             return MyCustomType.parse(value);
     *         }
     *         return super.invokeAdditionalField(fieldType, value);
     *     }
     * }
     * </pre>
     *
     * @param fieldType 변환 대상 사용자 정의 타입
     * @param value 문자열로 변환된 byte 데이터 (trim됨)
     * @return 변환된 사용자 정의 타입 인스턴스
     * @throws Exception 변환 중 발생할 수 있는 예외
     * @see #hasAdditionalType(Class)
     */
    protected Object invokeAdditionalField(final Class<?> fieldType, final String value) throws Exception {
        return null;
    }

    /**
     * <p>
     * Object를 byte[] 데이터로 역변환할 때 사용자 정의 타입의 필드 값을 문자열로 변환합니다.
     * </p>
     * <p>
     * 이 메서드는 {@link #hasAdditionalType(Class)}가 {@code true}를 반환한
     * 경우에만 호출됩니다. 사용자 정의 타입의 값을 문자열로 변환하는 로직을 구현해야 합니다.
     * </p>
     * <p>
     * <strong>사용 예:</strong>
     * </p>
     * <pre>
     * public class CustomConverter extends ByteToObjectConverter {
     *     &#64;Override
     *     protected String changeAdditionalDataToString(Object fieldData) throws Exception {
     *         if (fieldData instanceof MyCustomType) {
     *             return ((MyCustomType) fieldData).toFormattedString();
     *         }
     *         return super.changeAdditionalDataToString(fieldData);
     *     }
     * }
     * </pre>
     *
     * @param fieldData 변환할 사용자 정의 타입 인스턴스
     * @return 변환된 문자열. 이 문자열은 {@link ConvertData#value()},
     *         {@link ConvertData#lengthField()}, {@link DataAlignment}에
     *         따라 패딩 처리됨
     * @throws Exception 변환 중 발생할 수 있는 예외
     * @see #hasAdditionalType(Class)
     * @see #invokeAdditionalField(Class, String)
     */
    protected String changeAdditionalDataToString(final Object fieldData) throws Exception {
        return null;
    }

    /**
     * 사용자 정의 필드 타입 여부 함수
     *
     * @return
     */
    private Function<Class<?>, Boolean> hasAdditionalTypeFunction() {
        return fieldType -> {
            try {
                return hasAdditionalType(fieldType);
            } catch (final Exception e) {
                throw new ConvertFailException(e);
            }
        };
    }

    /**
     * 사용자 정의 필드 타입 값 설정 함수
     *
     * @return
     */
    private BiFunction<Class<?>, String, Object> invokeAdditionalFieldFunction() {
        return (fieldType, value) -> {
            try {
                return invokeAdditionalField(fieldType, value);
            } catch (final Exception e) {
                throw new ConvertFailException(e);
            }
        };
    }

    /**
     * 사용자 정의 필드 타입 값 설정 함수
     * 
     * @return
     */
    private Function<Object, String> changeAdditionalDataToStringFunction() {
        return fieldData -> {
            try {
                return changeAdditionalDataToString(fieldData);
            } catch (final Exception e) {
                throw new ConvertFailException(e);
            }
        };
    }

}
