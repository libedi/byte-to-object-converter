package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.libedi.converter.ByteToObjectConverter;

/**
 * <p>
 * 사용자 정의 Value Object 필드를 변환하기 위한 애노테이션. VO 내부 필드는 반드시
 * {@link ConvertData @ConvertData}, {@link Iteration @Iteration} 또는
 * {@link Embeddable @Embeddable} 애노테이션이 지정되어 있어야 합니다.
 * </p>
 * <p>
 * 사용법은 아래와 같습니다:
 * </p>
 *
 * <pre>
 * &#64;Embeddable
 * CustomVo customVo;
 * </pre>
 *
 * @author "Sangjun,Park"
 *
 * @see ByteToObjectConverter
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface Embeddable {

}
