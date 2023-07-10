package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.libedi.converter.ByteToObjectConverter;

/**
 * <p>
 * 변환할 필드를 지정하기 위한 애노테이션. 필드 변환 순서는 클래스에 선언한 필드 순서를 따릅니다. 변환할 byte 데이터의 길이는
 * {@link ConvertData @ConvertData} 애노테이션의 {@link #value} 속성으로 지정합니다. 만약, 필드의
 * 데이터 길이가 다른 필드의 값으로 지정되어 있는 경우, {@link #lengthField} 속성으로 데이터 길이가 지정된 필드명을
 * <code>String</code>으로 지정하며, 지정된 필드의 타입은 반드시 <code>int</code> 여야 합니다. 변환 필드의
 * 타입이 date-time 타입이면, {@link #format} 속성으로 데이터의 format을 지정할 수 있습니다.
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
 * </ul>
 * 
 * <p>
 * 사용법은 아래와 같습니다:
 * </p>
 *
 * <pre>
 * // 데이터의 길이가 14 byte인 문자열 데이터
 * &#64;ConvertData(14)
 * String string;
 * 
 * // 데이터의 길이가 4 byte인 int형 데이터
 * &#64;ConvertData(4)
 * int length;
 * 
 * // 데이터의 길이가 length 필드의 값만큼 가진 byte[] 데이터
 * &#64;ConvertData(lengthField = "length")
 * byte[] bytes;
 * 
 * // 데이터의 길이가 8 byte인 날짜형 데이터
 * &#64;ConvertData(value = 8, format = "yyyyMMdd")
 * LocalDate date;
 * </pre>
 *
 * @author "Sangjun,Park"
 *
 * @see ByteToObjectConverter
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface ConvertData {

    /**
     * 읽어올 byte 길이
     *
     * @return
     */
    int value() default 0;

    /**
     * 읽어올 byte 길이를 지정한 필드
     *
     * @return
     */
    String lengthField() default "";

    /**
     * 날짜 포맷
     *
     * @return
     */
    String format() default "";

}
