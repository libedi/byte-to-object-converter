package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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
