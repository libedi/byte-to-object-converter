package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>
 * 
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
