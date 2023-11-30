package io.github.libedi.converter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.time.Month;
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
import io.github.libedi.converter.exception.ConvertFailException;

/**
 * ConversionHelper
 *
 * @author "Sangjun,Park"
 *
 */
class ConversionHelper extends AbstractCommonHelper {

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
     * @throws ConvertFailException
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
            throw new ConvertFailException("Neither inputStream nor type must be null.");
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
            throw new ConvertFailException(e);
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
            throw new ConvertFailException(e);
        }
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
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws NumberFormatException
     */
    private <T> Object extractData(final Field field, final InputStream inputStream, final T targetObject,
            final Class<T> type) throws IllegalAccessException, IOException, NumberFormatException,
            NoSuchMethodException, InvocationTargetException {
        if (field.isAnnotationPresent(Iteration.class) && ClassUtils.isAssignable(List.class, field.getType())) {
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
        final Class<?> genericType = getGenericType(field);
        return IntStream.range(0, getCount(targetObject, type, field.getAnnotation(Iteration.class)))
                .mapToObj(i -> convert(inputStream, genericType))
                .collect(Collectors.toList());
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
            throw new ConvertFailException(e);
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
            throw new ConvertFailException("ConvertData#length() value must not be negative.");
        }
        try {
            final byte[] buf = new byte[length];
            inputStream.read(buf);
            return buf;
        } catch (final IOException e) {
            throw new ConvertFailException(e);
        }
    }

    /**
     * 필드 타입별 데이터 값 설정
     * 
     * @param field
     * @param bytes
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws NumberFormatException
     */
    private Object invokeSetValueByFieldType(final Field field, final byte[] bytes)
            throws NumberFormatException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
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
                throw new ConvertFailException("Date format must not be empty.");
            }
            return MethodUtils.invokeStaticMethod(fieldType, "parse", value, DateTimeFormatter.ofPattern(format));
        }
        return null;
    }

    /**
     * InputStream에서 특정 length만큼 String으로 변환하기
     *
     * @param inputStream
     * @param length
     * @return
     * @throws ConvertFailException
     */
    String convertInputStream(final InputStream inputStream, final int length) {
        try {
            return StringUtils.trim(new String(readInputStream(inputStream, length), dataCharset));
        } catch (final RuntimeException e) {
            throw new ConvertFailException(e);
        }
    }

}
