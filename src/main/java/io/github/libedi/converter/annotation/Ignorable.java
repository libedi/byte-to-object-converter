package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>
 * {@link Ignorable @Ignorable} 은 Object를 <code>byte[]</code>로 변환시 사용되며, 해당
 * 애노테이션이 지정된 필드는 필드의 값이 null인 경우, 길이가 지정되어 있어도 해당 필드의 변환을 무시합니다.
 * </p>
 *
 * @author "Sangjun,Park"
 *
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface Ignorable {

}
