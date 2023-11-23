package io.github.libedi.converter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import io.github.libedi.converter.annotation.ConvertData;
import io.github.libedi.converter.annotation.Embeddable;
import io.github.libedi.converter.annotation.Iteration;

/**
 * ConversionHelper
 *
 * @author "Sangjun,Park"
 *
 */
class ConversionHelper {

    private final Charset dataCharset;

    private final Function<Class<?>, Boolean>          hasAdditionalTypeFunction;
    private final BiFunction<Class<?>, String, Object> invokeAdditionalFieldFunction;

    ConversionHelper(final Charset dataCharset, final Function<Class<?>, Boolean> hasAdditionalTypeFunction,
            final BiFunction<Class<?>, String, Object> invokeAdditionalFieldFunction) {
        this.dataCharset = dataCharset;
        this.hasAdditionalTypeFunction = hasAdditionalTypeFunction;
        this.invokeAdditionalFieldFunction = invokeAdditionalFieldFunction;
    }

    /**
     * byte[] 데이터를 Object로 변환
     *
     * @param <T>
     * @param inputStream 데이터 입력 스트림
     * @param type        대상 Object 타입
     * @return
     */
    <T> T convert(final InputStream inputStream, final Class<T> type) {
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
            return makeAccessible(type.getDeclaredConstructor()).newInstance();
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 주어진 생성자를 접근가능하게 변경
     * 
     * @param <T>
     * @param constructor
     * @return
     */
    private <T> Constructor<T> makeAccessible(final Constructor<T> constructor) {
        if ((!Modifier.isPublic(constructor.getModifiers())
                || !Modifier.isPublic(constructor.getDeclaringClass().getModifiers())) && !constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        return constructor;
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
        FieldUtils.getAllFieldsList(type).stream()
                .filter(this::isTargetField)
                .forEach(field -> convertDataByField(field, inputStream, targetObject, type));
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
            FieldUtils.writeField(field, targetObject, extractData(field, inputStream, targetObject, type), true);
        } catch (IOException | ReflectiveOperationException e) {
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
     * @throws IOException
     * @throws ReflectiveOperationException
     */
    private <T> Object extractData(final Field field, final InputStream inputStream, final T targetObject,
            final Class<T> type) throws IOException, ReflectiveOperationException {
        if (field.isAnnotationPresent(Iteration.class) || ClassUtils.isAssignable(field.getType(), List.class)) {
            return inputStream.available() == 0 ? Collections.emptyList()
                    : extractIteratedData(field, inputStream, targetObject, type);
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
     * @throws IllegalAccessException
     */
    private <T> List<?> extractIteratedData(final Field field, final InputStream inputStream, final T targetObject,
            final Class<T> type) throws IllegalAccessException {
        final Iteration iteration = field.getAnnotation(Iteration.class);
        final Class<?> genericType = getGenericType(field);
        return IntStream.range(0, getCount(targetObject, type, iteration))
                .mapToObj(i -> convert(inputStream, genericType)).collect(Collectors.toList());
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
     * @throws IllegalAccessException
     */
    private <T> int getCount(final T targetObject, final Class<T> type, final Iteration iteration)
            throws IllegalAccessException {
        final int count = iteration.value();
        if (count > 0) {
            return count;
        }
        return (int) FieldUtils.readField(FieldUtils.getField(type, iteration.countField(), true), targetObject);
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
     * @throws IllegalAccessException
     */
    private <T> byte[] extractFieldData(final Field field, final InputStream inputStream, final T targetObject,
            final Class<T> type) throws IllegalAccessException {
        final ConvertData convertData = field.getAnnotation(ConvertData.class);
        int length = convertData.value();
        if (length == -1) {
            return readAllInputStream(inputStream);
        }
        if (length == 0) {
            length = (int) FieldUtils.readField(FieldUtils.getField(type, convertData.lengthField(), true),
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
     * @throws ReflectiveOperationException
     */
    private Object invokeSetValueByFieldType(final Field field, final byte[] bytes)
            throws NoSuchMethodException, ReflectiveOperationException {
        final Class<?> fieldType = field.getType();
        if (ClassUtils.isAssignable(fieldType, byte[].class)) {
            return bytes;
        }
        final String value = StringUtils.trim(new String(bytes, dataCharset));
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (ClassUtils.isAssignable(fieldType, String.class)) {
            return value;
        }
        if (hasAdditionalTypeFunction.apply(fieldType)) {
            return invokeAdditionalFieldFunction.apply(fieldType, value);
        }
        if (ClassUtils.isAssignable(fieldType, Month.class)) {
            return MethodUtils.invokeStaticMethod(fieldType, "of", Integer.parseInt(value));
        }
        if (!ClassUtils.isAssignable(fieldType, Void.class) && ClassUtils.isPrimitiveOrWrapper(fieldType)
                || fieldType.isEnum()) {
            final Class<?> type = ClassUtils.primitiveToWrapper(fieldType);
            return MethodUtils.invokeStaticMethod(type, "valueOf", value);
        }
        if (isJavaTimePackageClass(fieldType)) {
            final String format = field.getAnnotation(ConvertData.class).format();
            if (StringUtils.isBlank(format)) {
                throw new IllegalArgumentException("Date format must not be empty.");
            }
            return MethodUtils.invokeStaticMethod(fieldType, "parse", value, DateTimeFormatter.ofPattern(format));
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
        return ClassUtils.isAssignable(fieldType, ChronoLocalDate.class)
                || ClassUtils.isAssignable(fieldType, ChronoLocalDateTime.class)
                || ClassUtils.isAssignable(fieldType, LocalTime.class)
                || ClassUtils.isAssignable(fieldType, OffsetDateTime.class)
                || ClassUtils.isAssignable(fieldType, OffsetTime.class)
                || ClassUtils.isAssignable(fieldType, ZonedDateTime.class)
                || ClassUtils.isAssignable(fieldType, Year.class)
                || ClassUtils.isAssignable(fieldType, YearMonth.class);
    }

    /**
     * InputStream에서 특정 length만큼 String으로 변환하기
     *
     * @param inputStream
     * @param length
     * @return
     */
    String convertInputStream(final InputStream inputStream, final int length) {
        return StringUtils.trim(new String(readInputStream(inputStream, length), dataCharset));
    }

}
