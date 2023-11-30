package io.github.libedi.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import io.github.libedi.converter.annotation.ConvertData;
import io.github.libedi.converter.annotation.Embeddable;
import io.github.libedi.converter.annotation.Ignorable;
import io.github.libedi.converter.annotation.Iteration;
import io.github.libedi.converter.exception.ConvertFailException;

/**
 * DeconversionHelper
 *
 * @author "Sangjun,Park"
 *
 */
class DeconversionHelper extends AbstractCommonHelper {

    private final Charset dataCharset;

    private final Function<Class<?>, Boolean> hasAdditionalTypeFunction;
    private final Function<Object, String>    changeAdditionalDataToStringFunction;

    DeconversionHelper(final Charset dataCharset, final Function<Class<?>, Boolean> hasAdditionalTypeFunction,
            final Function<Object, String> changeAdditionalDataToStringFunction) {
        this.dataCharset = dataCharset;
        this.hasAdditionalTypeFunction = hasAdditionalTypeFunction;
        this.changeAdditionalDataToStringFunction = changeAdditionalDataToStringFunction;
    }

    /**
     * Object 를 byte[] 데이터로 역변환
     *
     * @param targetObject
     * @param alignment
     * @return
     * @throws ConvertFailException
     */
    byte[] deconvert(final Object targetObject, final DataAlignment alignment) {
        validateArguments(targetObject);
        return deconvertDatas(targetObject, alignment);
    }

    /**
     * 인자값 유효성 검사
     *
     * @param targetObject
     */
    private void validateArguments(final Object targetObject) {
        if (targetObject == null) {
            throw new ConvertFailException("targetObject must be null.");
        }
    }

    /**
     * 대상 객체의 데이터 변환
     *
     * @param targetObject
     * @param alignment
     * @return
     */
    private byte[] deconvertDatas(final Object targetObject, final DataAlignment alignment) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FieldUtils.getAllFieldsList(targetObject.getClass()).stream()
                .filter(this::isTargetField)
                .map(field -> deconvertDataByField(field, targetObject, alignment))
                .forEach(bytes -> writeBytes(baos, bytes));
        return baos.toByteArray();
    }

    /**
     * 
     * @param baos
     * @param bytes
     */
    private void writeBytes(final OutputStream baos, final byte[] bytes) {
        try {
            baos.write(bytes);
        } catch (final IOException e) {
            throw new ConvertFailException(e);
        }
    }

    /**
     * 필드별 데이터 변환
     *
     * @param field
     * @param targetObject
     * @param alignment
     * @return
     */
    private byte[] deconvertDataByField(final Field field, final Object targetObject,
            final DataAlignment alignment) {
        try {
            final Object fieldValue = readField(field, targetObject);
            if (isIgnorable(field, fieldValue)) {
                return ArrayUtils.EMPTY_BYTE_ARRAY;
            }
            if (field.isAnnotationPresent(Iteration.class) && ClassUtils.isAssignable(List.class, field.getType())) {
                return extractIteratedData(field, targetObject, fieldValue, alignment);
            }
            if (field.isAnnotationPresent(Embeddable.class)) {
                return extractEmbeddedData(field, fieldValue, alignment);
            }
            return deconvertFieldDataToBytes(field, targetObject, fieldValue, alignment);
        } catch (final ReflectiveOperationException e) {
            throw new ConvertFailException(e);
        }
    }

    /**
     * <code>List</code> 형식 데이터 추출
     *
     * @param field
     * @param targetObject
     * @param listValue
     * @param alignment
     * @return
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private byte[] extractIteratedData(final Field field, final Object targetObject, final Object listValue,
            final DataAlignment alignment)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, InvocationTargetException {
        final int size = getListSize(listValue);
        final Constructor<?> elementConstructor = makeAccessible(getGenericType(field).getDeclaredConstructor());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IntStream.range(0, getCount(targetObject, targetObject.getClass(), field.getAnnotation(Iteration.class)))
                .mapToObj(i -> deconvertElement(listValue, alignment, size, elementConstructor, i))
                .forEach(bytes -> writeBytes(baos, bytes));
        return baos.toByteArray();
    }

    /**
     * 
     * @param field
     * @param targetObject
     * @return
     * @throws IllegalAccessException
     */
    private Object readField(final Field field, final Object targetObject) throws IllegalAccessException {
        return FieldUtils.readField(field, targetObject, true);
    }

    private boolean isIgnorable(final Field field, final Object fieldValue) {
        return fieldValue == null && field.isAnnotationPresent(Ignorable.class);
    }

    private int getListSize(final Object list)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return list == null ? 0 : (int) MethodUtils.invokeMethod(list, true, "size");
    }

    private byte[] deconvertElement(final Object targetObject, final DataAlignment alignment, final int size,
            final Constructor<?> elementConstructor, final int i) {
        try {
            final Object element = i < size ? MethodUtils.invokeMethod(targetObject, true, "get", i)
                    : elementConstructor.newInstance();
            return deconvert(element, alignment);
        } catch (final ReflectiveOperationException e) {
            throw new ConvertFailException(e);
        }
    }

    /**
     * 
     * @param field
     * @param fieldObject
     * @param alignment
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    private byte[] extractEmbeddedData(final Field field, final Object fieldObject, final DataAlignment alignment)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException {
        return deconvert(fieldObject == null
                ? makeAccessible(field.getType().getDeclaredConstructor()).newInstance()
                : fieldObject
                , alignment);
    }

    /**
     * 필드 데이터를 <code>byte[]</code>로 변환
     *
     * @param field
     * @param targetObject
     * @param fieldValue
     * @param alignment
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private byte[] deconvertFieldDataToBytes(final Field field, final Object targetObject, final Object fieldValue,
            final DataAlignment alignment)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (ClassUtils.isAssignable(field.getType(), byte[].class)) {
            return alignment.applyPad((byte[]) fieldValue, getPadSize(field, targetObject), dataCharset);
        }
        final String fieldData = changeFieldDataToString(field, fieldValue);
        return alignment.applyPad(StringUtils.defaultString(fieldData), getPadSize(field, targetObject))
                .getBytes(dataCharset);
    }

    /**
     * 
     * @param field
     * @param value
     * @return
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private String changeFieldDataToString(final Field field, final Object value)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Class<?> fieldType = field.getType();
        if (value == null || ClassUtils.isAssignable(fieldType, String.class)) {
            return (String) value;
        }
        if (hasAdditionalTypeFunction.apply(fieldType)) {
            return changeAdditionalDataToStringFunction.apply(value);
        }
        if (ClassUtils.isAssignable(fieldType, Month.class)) {
            return String.valueOf(MethodUtils.invokeMethod(value, true, "getValue"));
        }
        if (!ClassUtils.isAssignable(fieldType, Void.class) && ClassUtils.isPrimitiveOrWrapper(fieldType)
                || fieldType.isEnum()) {
            return (String) MethodUtils.invokeMethod(value, true, "toString");
        }
        if (isJavaTimePackageClass(fieldType)) {
            final String format = field.getAnnotation(ConvertData.class).format();
            if (StringUtils.isBlank(format)) {
                throw new ConvertFailException("Date format must not be empty.");
            }
            return (String) MethodUtils.invokeMethod(value, true, "format", DateTimeFormatter.ofPattern(format));
        }
        return null;
    }

    /**
     * 
     * @param field
     * @param targetObject
     * @return
     * @throws IllegalAccessException
     */
    private int getPadSize(final Field field, final Object targetObject) throws IllegalAccessException {
        final ConvertData convertData = field.getAnnotation(ConvertData.class);
        final int size = convertData.value();
        if (size == 0) {
            return (int) FieldUtils.readField(
                    FieldUtils.getField(targetObject.getClass(), convertData.lengthField(), true), targetObject);
        }
        return size;
    }

}
