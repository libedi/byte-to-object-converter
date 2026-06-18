package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>
 * Object를 byte[]로 역변환할 때 조건부로 필드를 무시하기 위한 annotation입니다.
 * </p>
 * <p>
 * {@link ConvertData @ConvertData}와 함께 사용하여, 필드 값이 null인 경우
 * 해당 필드를 바이트 변환에서 제외합니다. 이는 선택적 필드나 가변적인 구조를
 * 처리할 때 유용합니다.
 * </p>
 * <p>
 * <strong>동작:</strong>
 * </p>
 * <ul>
 * <li>필드 값이 null이면 - 필드를 무시하고 byte[]에 추가하지 않음</li>
 * <li>필드 값이 null이 아니면 - 일반적으로 {@link ConvertData @ConvertData}에 따라 변환</li>
 * </ul>
 * <p>
 * <strong>주의사항:</strong>
 * </p>
 * <ul>
 * <li>변환(byte[] → Object)시에는 효과가 없습니다</li>
 * <li>역변환(Object → byte[])시에만 적용됩니다</li>
 * <li>{@link ConvertData @ConvertData} annotation과 함께 사용해야 합니다</li>
 * <li>Iteration이나 Embeddable 필드와는 함께 사용할 수 없습니다</li>
 * </ul>
 * <p>
 * <strong>사용 예:</strong>
 * </p>
 *
 * <pre>
 * public class Message {
 *     public Message() { }
 *
 *     &#64;ConvertData(4)
 *     int messageId;
 *
 *     // 필수 필드 - 항상 포함
 *     &#64;ConvertData(20)
 *     String sender;
 *
 *     // 선택적 필드 - null이면 무시, null이 아니면 10 byte 포함
 *     &#64;ConvertData(10)
 *     &#64;Ignorable
 *     String recipient;
 * }
 *
 * // 사용
 * ByteToObjectConverter converter = new ByteToObjectConverter();
 *
 * Message msg1 = new Message();
 * msg1.messageId = 1;
 * msg1.sender = "Alice";
 * msg1.recipient = null;  // 무시됨
 * byte[] result1 = converter.deconvert(msg1, DataAlignment.LEFT);
 * // result1 = messageId(4) + sender(20) = 24 byte
 *
 * Message msg2 = new Message();
 * msg2.messageId = 1;
 * msg2.sender = "Alice";
 * msg2.recipient = "Bob";  // 포함됨
 * byte[] result2 = converter.deconvert(msg2, DataAlignment.LEFT);
 * // result2 = messageId(4) + sender(20) + recipient(10) = 34 byte
 * </pre>
 *
 * @author "Sangjun,Park"
 * @see ConvertData
 * @see ByteToObjectConverter#deconvert(Object, io.github.libedi.converter.DataAlignment)
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface Ignorable {

}
