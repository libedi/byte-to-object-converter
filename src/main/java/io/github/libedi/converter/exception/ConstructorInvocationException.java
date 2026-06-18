package io.github.libedi.converter.exception;

/**
 * <p>
 * 대상 클래스의 기본 생성자(default constructor) 호출이 실패할 때 발생하는 예외입니다.
 * </p>
 * <p>
 * 변환 과정에서 대상 Object와 Value Object의 인스턴스를 생성할 때 기본 생성자를 호출합니다.
 * 이 과정에서 다양한 이유로 실패할 수 있습니다:
 * </p>
 * <ul>
 * <li>기본 생성자가 존재하지 않는 경우</li>
 * <li>생성자가 private이고 setAccessible 실패한 경우</li>
 * <li>생성자 내부에서 예외 발생 (초기화 로직 오류 등)</li>
 * <li>생성자 호출 권한이 없는 경우</li>
 * <li>Value Object 클래스에 기본 생성자가 없는 경우</li>
 * </ul>
 * <p>
 * 요구사항:
 * </p>
 * <p>
 * 변환 대상이 되는 모든 클래스는 반드시 기본 생성자(매개변수 없는 생성자)를 가져야 합니다.
 * private 생성자도 허용됩니다.
 * </p>
 * <pre>
 * // OK - public 기본 생성자
 * public class MyObject {
 *     public MyObject() { }
 * }
 *
 * // OK - private 기본 생성자
 * public class MyObject {
 *     private MyObject() { }
 * }
 *
 * // NOT OK - 기본 생성자 없음
 * public class MyObject {
 *     public MyObject(String param) { }  // 생성자는 있지만 기본이 아님
 * }
 * </pre>
 * <p>
 * Embeddable로 사용되는 Value Object도 동일한 요구사항을 만족해야 합니다.
 * </p>
 *
 * @author "Sangjun,Park"
 * @see ReflectionException
 * @see io.github.libedi.converter.annotation.Embeddable
 */
public class ConstructorInvocationException extends ReflectionException {

    private static final long serialVersionUID = 1L;

    /**
     * ConstructorInvocationException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public ConstructorInvocationException(final String message) {
        super(message);
    }

    /**
     * ConstructorInvocationException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public ConstructorInvocationException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
