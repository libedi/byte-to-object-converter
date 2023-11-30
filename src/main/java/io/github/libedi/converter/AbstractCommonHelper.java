package io.github.libedi.converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import io.github.libedi.converter.annotation.ConvertData;
import io.github.libedi.converter.annotation.Embeddable;
import io.github.libedi.converter.annotation.Iteration;

/**
 * AbstractCommonHelper
 *
 * @author "Sangjun,Park"
 *
 */
abstract class AbstractCommonHelper {

    /**
     * 주어진 생성자를 접근가능하게 변경
     * 
     * @param <T>
     * @param constructor
     * @return
     */
    protected <T> Constructor<T> makeAccessible(final Constructor<T> constructor) {
        if ((!Modifier.isPublic(constructor.getModifiers())
                || !Modifier.isPublic(constructor.getDeclaringClass().getModifiers())) && !constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        return constructor;
    }

    /**
     * 변환 대상 필드 여부
     *
     * @param field
     * @return
     */
    protected boolean isTargetField(final Field field) {
        return field.isAnnotationPresent(ConvertData.class) || field.isAnnotationPresent(Iteration.class)
                || field.isAnnotationPresent(Embeddable.class);
    }

    /**
     * 제네릭 타입 조회
     *
     * @param field
     * @return
     */
    protected Class<?> getGenericType(final Field field) {
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
    protected <T> int getCount(final T targetObject, final Class<? extends T> type, final Iteration iteration)
            throws IllegalAccessException {
        final int count = iteration.value();
        if (count > 0) {
            return count;
        }
        return (int) FieldUtils.readField(FieldUtils.getField(type, iteration.countField(), true), targetObject);
    }

    /**
     * java.time 패키지 클래스 여부
     *
     * @param fieldType
     * @return
     */
    protected boolean isJavaTimePackageClass(final Class<?> fieldType) {
        return ClassUtils.isAssignable(fieldType, ChronoLocalDate.class)
                || ClassUtils.isAssignable(fieldType, ChronoLocalDateTime.class)
                || ClassUtils.isAssignable(fieldType, LocalTime.class)
                || ClassUtils.isAssignable(fieldType, OffsetDateTime.class)
                || ClassUtils.isAssignable(fieldType, OffsetTime.class)
                || ClassUtils.isAssignable(fieldType, ZonedDateTime.class)
                || ClassUtils.isAssignable(fieldType, Year.class)
                || ClassUtils.isAssignable(fieldType, YearMonth.class);
    }

}
