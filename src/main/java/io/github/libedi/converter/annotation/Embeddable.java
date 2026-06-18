package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.libedi.converter.ByteToObjectConverter;

/**
 * <p>
 * 사용자 정의 Value Object(VO) 필드를 변환하기 위한 annotation입니다.
 * </p>
 * <p>
 * 중첩된 Object 구조를 표현할 때 사용하며, VO 내부의 모든 필드도
 * 변환 대상이 되어야 합니다.
 * </p>
 * <p>
 * <strong>필수 조건:</strong>
 * </p>
 * <ul>
 * <li>VO 클래스는 기본 생성자(매개변수 없는 생성자)를 가져야 합니다
 *     (private 생성자도 허용)</li>
 * <li>VO 내부의 모든 필드는 반드시 다음 중 하나의 annotation을 가져야 합니다:
 *   <ul>
 *     <li>{@link ConvertData @ConvertData} - 기본 필드</li>
 *     <li>{@link Iteration @Iteration} - List 타입 필드</li>
 *     <li>{@link Embeddable @Embeddable} - 또 다른 중첩 VO</li>
 *   </ul>
 * </li>
 * <li>annotation이 없는 필드는 무시됩니다</li>
 * </ul>
 * <p>
 * <strong>동작:</strong>
 * </p>
 * <p>
 * 변환 시, 이 필드에 대해 VO 타입으로 재귀적으로 {@code convert()}를 호출합니다.
 * 역변환 시, VO의 모든 annotation이 있는 필드를 순서대로 처리합니다.
 * </p>
 * <p>
 * <strong>사용 예:</strong>
 * </p>
 *
 * <pre>
 * // 메인 객체
 * public class Order {
 *     public Order() { }
 *
 *     &#64;ConvertData(4)
 *     int orderId;
 *
 *     // 중첩된 VO
 *     &#64;Embeddable
 *     Address deliveryAddress;
 * }
 *
 * // Value Object
 * public class Address {
 *     public Address() { }
 *
 *     &#64;ConvertData(20)
 *     String street;
 *
 *     &#64;ConvertData(10)
 *     String city;
 *
 *     &#64;ConvertData(5)
 *     String zipCode;
 * }
 *
 * // 사용
 * ByteToObjectConverter converter = new ByteToObjectConverter();
 * Order order = converter.convert(inputStream, Order.class);
 * // order.deliveryAddress.street, .city, .zipCode가 자동으로 채워짐
 * </pre>
 * <p>
 * <strong>역변환 시 주의사항:</strong>
 * </p>
 * <p>
 * Object를 byte[]로 역변환할 때:
 * </p>
 * <ul>
 * <li>VO 필드가 null이면 새 인스턴스를 생성하여 사용합니다</li>
 * <li>VO 내부의 모든 필드는 동일한 방식으로 처리됩니다</li>
 * </ul>
 *
 * @author "Sangjun,Park"
 * @see ByteToObjectConverter
 * @see ConvertData
 * @see Iteration
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface Embeddable {

}
