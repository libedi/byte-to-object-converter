package io.github.libedi.converter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import io.github.libedi.converter.annotation.ConvertData;
import io.github.libedi.converter.annotation.Embeddable;
import io.github.libedi.converter.annotation.Iteration;

/**
 * <p>
 * byte의 데이터를 Object로 쉽게 변환해줍니다.
 * </p>
 * <p>
 * 기존 레거시 코드에서 byte 전문을 Object로 변환시 데이터 파싱에 많은 boilerplate 코드가 필요합니다.
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
 * 
 * @author "Sangjun, Park"
 * 
 * @see ConvertData
 * @see Iteration
 * @see Embeddable
 */
public class ByteToObjectConverter {

    private final Charset dataCharset;

    public ByteToObjectConverter() {
        dataCharset = Charset.defaultCharset();
    }

    public ByteToObjectConverter(final Charset dataCharset) {
        this.dataCharset = dataCharset;
    }

    public ByteToObjectConverter(final String dataCharset) {
        this.dataCharset = Charset.forName(dataCharset);
    }

    /**
     * byte[] 데이터를 Object로 변환
     *
     * @param <T>
     * @param inputStream 데이터 입력 스트림
     * @param type        대상 Object 타입
     * @return
     */
    public <T> T convert(final InputStream inputStream, final Class<T> type) {
        validateArguments(inputStream, type);
        final T targetObject = createTargetObject(type);
        convertDatas(inputStream, type, targetObject);
        return targetObject;
    }

    /**
     * 인자값 유효성 검사
     *
     * @param <T>
     * @param inputStream
     * @param type
     */
    private <T> void validateArguments(final InputStream inputStream, final Class<T> type) {
        if (inputStream == null || type == null) {
            throw new IllegalArgumentException("Neither inputStream nor type must be null.");
        }
    }

    /**
     * 대상 객체 생성
     *
     * @param <T>
     * @param type
     * @return
     */
    private <T> T createTargetObject(final Class<T> type) {
        try {
            return ReflectionUtils.accessibleConstructor(type).newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 대상 객체의 데이터 변환
     *
     * @param <T>
     * @param inputStream
     * @param type
     * @param targetObject
     */
    private <T> void convertDatas(final InputStream inputStream, final Class<T> type, final T targetObject) {
        ReflectionUtils.doWithFields(type,
                field -> convertDataByField(field, inputStream, targetObject, type),
                this::isTargetField);
    }

    /**
     * 필드별 데이터 변환
     *
     * @param <T>
     * @param field
     * @param inputStream
     * @param targetObject
     * @param type
     */
    private <T> void convertDataByField(final Field field, final InputStream inputStream, final T targetObject,
            final Class<T> type) {
        try {
            if (inputStream.available() == 0 && !field.isAnnotationPresent(Iteration.class)) {
                return;
            }
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, targetObject, extractData(field, inputStream, targetObject, type));
        } catch (NoSuchMethodException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 변환 대상 필드 여부
     *
     * @param field
     * @return
     */
    private boolean isTargetField(final Field field) {
        return field.isAnnotationPresent(ConvertData.class) || field.isAnnotationPresent(Iteration.class)
                || field.isAnnotationPresent(Embeddable.class);
    }

    /**
     * 필드 데이터 추출
     *
     * @param <T>
     * @param field
     * @param inputStream
     * @param targetObject
     * @param type
     * @return
     * @throws NoSuchMethodException
     * @throws IOException
     */
    private <T> Object extractData(final Field field, final InputStream inputStream, final T targetObject,
            final Class<T> type) throws NoSuchMethodException, IOException {
        if (field.isAnnotationPresent(Iteration.class) || ClassUtils.isAssignable(List.class, field.getType())) {
            return extractIteratedData(field, inputStream, targetObject, type);
        }
        if (field.isAnnotationPresent(Embeddable.class)) {
            return extractEmbeddedData(field, inputStream);
        }
        return invokeSetValueByFieldType(field, extractFieldData(field, inputStream, targetObject, type));
    }

    /**
     * 반복되는 데이터 형식 추출
     *
     * @param <T>
     * @param field
     * @param inputStream
     * @param targetObject
     * @param type
     * @return
     * @throws IOException
     */
    private <T> List<?> extractIteratedData(final Field field, final InputStream inputStream, final T targetObject,
            final Class<T> type) throws IOException {
        final Iteration iteration = field.getAnnotation(Iteration.class);
        final Class<?> genericType = getGenericType(field);
        return IntStream.range(0, inputStream.available() == 0 ? 0 : getCount(targetObject, type, iteration))
                .mapToObj(i -> convert(inputStream, genericType))
                .collect(Collectors.toList());
    }

    /**
     * 제네릭 타입 조회
     *
     * @param field
     * @return
     */
    private Class<?> getGenericType(final Field field) {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }

    /**
     * 반복 횟수
     *
     * @param <T>
     * @param targetObject
     * @param type
     * @param iteration
     * @return
     */
    private <T> int getCount(final T targetObject, final Class<T> type, final Iteration iteration) {
        final int count = iteration.value();
        if (count > 0) {
            return count;
        }
        return (int) ReflectionUtils.getField(ReflectionUtils.findField(type, iteration.countField()), targetObject);
    }

    /**
     * Value Object 데이터 추출
     *
     * @param field
     * @param inputStream
     * @return
     */
    private Object extractEmbeddedData(final Field field, final InputStream inputStream) {
        return convert(inputStream, field.getType());
    }

    /**
     * 데이터 값 추출
     *
     * @param <T>
     * @param field
     * @param inputStream
     * @param targetObject
     * @param type
     * @return
     */
    private <T> byte[] extractFieldData(final Field field, final InputStream inputStream, final T targetObject,
            final Class<T> type) {
        final ConvertData convertData = field.getAnnotation(ConvertData.class);
        int length = convertData.value();
        if (length == -1) {
            return readAllInputStream(inputStream);
        }
        if (length == 0) {
            length = (int) ReflectionUtils.getField(ReflectionUtils.findField(type, convertData.lengthField()),
                    targetObject);
        }
        return readInputStream(inputStream, length);
    }

    /**
     * InputStream에서 남은 byte[] 읽어오기
     *
     * @param inputStream
     * @return
     */
    private byte[] readAllInputStream(final InputStream inputStream) {
        try {
            return readInputStream(inputStream, inputStream.available());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * InputStream에서 특정 length만큼 byte[] 읽어오기
     *
     * @param inputStream
     * @param length
     * @return
     */
    private byte[] readInputStream(final InputStream inputStream, final int length) {
        if (length < 0) {
            throw new IllegalArgumentException("length must not be negative.");
        }
        try {
            final byte[] buf = new byte[length];
            inputStream.read(buf);
            return buf;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 필드 타입별 데이터 값 설정
     * 
     * @param field
     * @param bytes
     * @return
     * @throws NoSuchMethodException
     */
    private Object invokeSetValueByFieldType(final Field field, final byte[] bytes) throws NoSuchMethodException {
        final Class<?> fieldType = field.getType();
        if (ClassUtils.isAssignable(byte[].class, fieldType)) {
            return bytes;
        }
        final String value = StringUtils.trimWhitespace(new String(bytes, dataCharset));
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (ClassUtils.isAssignable(String.class, fieldType)) {
            return value;
        }
        try {
            if (hasAdditionalType(fieldType)) {
                return invokeAdditionalField(fieldType, value);
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        if (ClassUtils.isAssignable(Month.class, fieldType)) {
            return ReflectionUtils.invokeMethod(fieldType.getMethod("of", int.class), null, Integer.valueOf(value));
        }
        if (!ClassUtils.isAssignable(Void.class, fieldType) && ClassUtils.isPrimitiveOrWrapper(fieldType)
                || fieldType.isEnum()) {
            final Class<?> type = fieldType.isPrimitive() ? ClassUtils.resolvePrimitiveIfNecessary(fieldType)
                    : fieldType;
            return ReflectionUtils.invokeMethod(type.getMethod("valueOf", String.class), null, value);
        }
        if (isJavaTimePackageClass(fieldType)) {
            final String format = field.getAnnotation(ConvertData.class).format();
            if (!StringUtils.hasText(format)) {
                throw new IllegalArgumentException("Date format must not be empty.");
            }
            return ReflectionUtils.invokeMethod(
                    fieldType.getMethod("parse", CharSequence.class, DateTimeFormatter.class), null, value,
                    DateTimeFormatter.ofPattern(format));
        }
        return null;
    }

    /**
     * java.time 패키지 클래스 여부
     *
     * @param fieldType
     * @return
     */
    private boolean isJavaTimePackageClass(final Class<?> fieldType) {
        return ClassUtils.isAssignable(ChronoLocalDate.class, fieldType)
                || ClassUtils.isAssignable(ChronoLocalDateTime.class, fieldType)
                || ClassUtils.isAssignable(LocalTime.class, fieldType)
                || ClassUtils.isAssignable(OffsetDateTime.class, fieldType)
                || ClassUtils.isAssignable(OffsetTime.class, fieldType)
                || ClassUtils.isAssignable(ZonedDateTime.class, fieldType)
                || ClassUtils.isAssignable(Year.class, fieldType)
                || ClassUtils.isAssignable(YearMonth.class, fieldType);
    }

    /**
     * InputStream에서 특정 length만큼 String으로 변환하기
     *
     * @param inputStream
     * @param length
     * @return
     */
    public String convertInputStream(final InputStream inputStream, final int length) {
        return StringUtils.trimWhitespace(new String(readInputStream(inputStream, length), dataCharset));
    }

    /**
     * 사용자 정의 필드 타입 여부
     *
     * @param fieldType
     * @return
     * @throws Exception
     */
    protected boolean hasAdditionalType(final Class<?> fieldType) throws Exception {
        return false;
    }

    /**
     * 사용자 정의 필드 타입 값 설정
     *
     * @param fieldType
     * @param value
     * @return
     * @throws Exception
     */
    protected Object invokeAdditionalField(final Class<?> fieldType, final String value) throws Exception {
        return null;
    }

}
