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

public class ByteToObjectConverter {

    private final Charset dataCharset;

    public ByteToObjectConverter(final Charset dataCharset) {
        this.dataCharset = dataCharset;
    }

    /**
     * 데이터를 DTO로 변환
     *
     * @param <T>
     * @param inputStream 데이터 입력 스트림
     * @param type        DTO 타입
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
            if (inputStream.available() == 0) {
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
     */
    private <T> Object extractData(final Field field, final InputStream inputStream, final T targetObject,
            final Class<T> type) throws NoSuchMethodException {
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
     */
    private <T> List<?> extractIteratedData(final Field field, final InputStream inputStream, final T targetObject,
            final Class<T> type) {
        final Iteration iteration = field.getAnnotation(Iteration.class);
        final Class<?> genericType = getGenericType(field);
        return IntStream.range(0, getCount(targetObject, type, iteration))
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
            length = (int) ReflectionUtils.getField(ReflectionUtils.findField(type, convertData.lenghField()),
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
     * @throws
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
        if (hasAdditionalType(fieldType)) {
            return invokeAdditionalField(fieldType);
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
     */
    protected boolean hasAdditionalType(final Class<?> fieldType) {
        return false;
    }

    /**
     * 사용자 정의 필드 타입 값 설정
     *
     * @param fieldType
     * @return
     */
    protected Object invokeAdditionalField(final Class<?> fieldType) {
        return null;
    }

}
