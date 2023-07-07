package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.libedi.converter.ByteToObjectConverter;

/**
 * <p>
 * <code>List</code> 타입을 가진 반복되는 필드 변환을 위한 애노테이션. 반복 횟수는 고정 반복 횟수를 지정하는
 * {@link #value} 속성과 다른 필드값에 따라 반복되는 {@link #countField} 속성으로 지정합니다.
 * {@link #countField} 는 필드명을 <code>String</code>으로 지정하며, 해당 필드의 타입은 반드시
 * <code>int</code>여야 합니다.<br/>
 * {@link Iteration @Iteration} 애노테이션이 지정한 필드는 반드시 <code>List</code>의 제네릭 타입을
 * 지정해야 합니다. 해당 제네릭 타입의 클래스 필드는 반드시 {@link ConvertData @ConvertData},
 * {@link Iteration @Iteration} 또는 {@link Embeddable @Embeddable} 애노테이션이 지정되어
 * 있어야 합니다.
 * </p>
 * <p>
 * 사용법은 아래와 같습니다:
 * </p>
 *
 * <pre>
 * // 반복 횟수가 3으로 고정된 데이터
 * &#64;Iteration(3)
 * List&lt;VO&gt; fixedIterationList;
 * 
 * &#64;ConvertData(4)
 * int count;
 * 
 * // count 필드의 값만큼 반복하는 데이터
 * &#64;Iteration(countField = "count")
 * List&lt;VO&gt; fieldIterationList;
 * </pre>
 *
 * @author "Sangjun,Park"
 * @see ByteToObjectConverter
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface Iteration {

    /**
     * 고정 횟수 반복값 설정
     *
     * @return
     */
    int value() default 0;

    /**
     * 반복값 필드 지정
     *
     * @return
     */
    String countField() default "";
}
