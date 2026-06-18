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
 * <p>
 * {@link ConversionHelper}와 {@link DeconversionHelper}가 공유하는
 * 공통 기능을 제공하는 추상 클래스입니다.
 * </p>
 * <p>
 * 주요 기능:
 * </p>
 * <ul>
 * <li>리플렉션을 통한 생성자 접근성 관리</li>
 * <li>annotation이 있는 필드 필터링</li>
 * <li>제네릭 타입 정보 추출</li>
 * <li>반복 횟수 계산</li>
 * <li>Java 8+ date-time 클래스 판별</li>
 * </ul>
 * <p>
 * 이 클래스의 protected 메서드들은 변환/역변환 과정에서 자주 사용되는
 * 유틸리티 메서드를 제공하며, 하위 클래스에서 재사용할 수 있도록 설계되었습니다.
 * </p>
 *
 * @author "Sangjun,Park"
 * @see ConversionHelper
 * @see DeconversionHelper
 */
abstract class AbstractCommonHelper {

    /**
     * <p>
     * 주어진 생성자의 접근성(accessibility)을 확인하고 필요한 경우 변경합니다.
     * </p>
     * <p>
     * private 또는 package-private 생성자에 대해 리플렉션을 통해 접근하기 위해
     * setAccessible(true)을 호출합니다. 이미 접근 가능한 경우는 변경하지 않습니다.
     * </p>
     *
     * @param <T> 생성자가 속한 클래스의 타입
     * @param constructor 접근성을 확인/변경할 생성자
     * @return 접근 가능하게 설정된 생성자
     * @see java.lang.reflect.Constructor#setAccessible(boolean)
     */
    protected <T> Constructor<T> makeAccessible(final Constructor<T> constructor) {
        if ((!Modifier.isPublic(constructor.getModifiers())
                || !Modifier.isPublic(constructor.getDeclaringClass().getModifiers())) && !constructor.isAccessible()) {
            constructor.setAccessible(true);
        }
        return constructor;
    }

    /**
     * <p>
     * 주어진 필드가 변환 대상인지 판별합니다.
     * </p>
     * <p>
     * 다음 중 하나의 annotation이 있으면 변환 대상입니다:
     * </p>
     * <ul>
     * <li>{@link ConvertData @ConvertData} - 기본 필드 변환</li>
     * <li>{@link Iteration @Iteration} - List 타입 필드 변환</li>
     * <li>{@link Embeddable @Embeddable} - Value Object 필드 변환</li>
     * </ul>
     * <p>
     * {@link Ignorable @Ignorable} annotation이 있는 필드도 변환 대상이 될 수 있습니다.
     * (역변환 시에만 사용되며, 필드 값이 null인 경우 처리 방식이 다릅니다)
     * </p>
     *
     * @param field 검사할 필드
     * @return {@code true}이면 변환 대상, {@code false}이면 무시
     * @see ConvertData
     * @see Iteration
     * @see Embeddable
     */
    protected boolean isTargetField(final Field field) {
        return field.isAnnotationPresent(ConvertData.class) || field.isAnnotationPresent(Iteration.class)
                || field.isAnnotationPresent(Embeddable.class);
    }

    /**
     * <p>
     * List 필드의 제네릭 타입을 추출합니다.
     * </p>
     * <p>
     * {@link Iteration @Iteration} annotation이 있는 List 필드의 경우,
     * 리스트가 포함하는 요소의 타입을 얻기 위해 사용됩니다.
     * </p>
     * <p>
     * 예:
     * </p>
     * <pre>
     * &#64;Iteration(3)
     * List&lt;Item&gt; items;  // getGenericType() returns Item.class
     * </pre>
     * <p>
     * 주의: 이 메서드는 List의 첫 번째 제네릭 타입 인자만 반환합니다.
     * 제네릭 타입이 지정되지 않으면 ClassCastException이 발생할 수 있습니다.
     * </p>
     *
     * @param field List 타입 필드 (제네릭 타입이 지정되어야 함)
     * @return List의 요소 타입
     * @throws ClassCastException 필드가 제네릭 타입 정보를 가지지 않은 경우
     * @see Iteration
     */
    protected Class<?> getGenericType(final Field field) {
        return (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
    }

    /**
     * <p>
     * {@link Iteration @Iteration} annotation의 반복 횟수를 계산합니다.
     * </p>
     * <p>
     * 반복 횟수는 두 가지 방식으로 지정될 수 있습니다:
     * </p>
     * <ol>
     * <li>고정 반복 횟수: {@link Iteration#value()}가 0보다 큰 경우 → 그 값을 반환</li>
     * <li>동적 반복 횟수: {@link Iteration#countField()}로 지정된 필드의 값을 읽어 반환
     *     (해당 필드의 타입은 반드시 int여야 함)</li>
     * </ol>
     * <p>
     * 예:
     * </p>
     * <pre>
     * // 고정 반복 - countField = "", value = 3 → getCount() returns 3
     * &#64;Iteration(3)
     * List&lt;Item&gt; fixedItems;
     *
     * // 동적 반복 - countField = "itemCount", value = 0 → countField 필드값 반환
     * &#64;ConvertData(4)
     * int itemCount;
     *
     * &#64;Iteration(countField = "itemCount")
     * List&lt;Item&gt; dynamicItems;
     * </pre>
     *
     * @param <T> 대상 Object 타입
     * @param targetObject 반복 횟수를 계산할 대상 객체 (동적 반복 시 필요)
     * @param type 대상 클래스 타입
     * @param iteration Iteration annotation 정보
     * @return 반복 횟수 (0 이상)
     * @throws IllegalAccessException countField 필드 읽기 실패 시
     * @see Iteration
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
     * <p>
     * 주어진 클래스가 java.time 패키지의 date-time 클래스인지 판별합니다.
     * </p>
     * <p>
     * 지원되는 java.time 클래스:
     * </p>
     * <ul>
     * <li>{@link java.time.chrono.ChronoLocalDate} - LocalDate, ThaiBuddhistDate 등</li>
     * <li>{@link java.time.chrono.ChronoLocalDateTime} - LocalDateTime 등</li>
     * <li>{@link java.time.LocalTime}</li>
     * <li>{@link java.time.OffsetDateTime}</li>
     * <li>{@link java.time.OffsetTime}</li>
     * <li>{@link java.time.ZonedDateTime}</li>
     * <li>{@link java.time.Year}</li>
     * <li>{@link java.time.YearMonth}</li>
     * </ul>
     * <p>
     * 이러한 타입의 필드에는 {@link ConvertData @ConvertData}의 format 속성이
     * 반드시 지정되어야 합니다.
     * </p>
     *
     * @param fieldType 검사할 클래스 타입
     * @return {@code true}이면 java.time 패키지 클래스, {@code false}이면 아님
     * @see ConvertData#format()
     * @see io.github.libedi.converter.annotation.ConvertData
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
