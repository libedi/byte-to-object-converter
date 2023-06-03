package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

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
    String lenghField() default "";

    /**
     * 날짜 포맷
     *
     * @return
     */
    String format() default "";

}
