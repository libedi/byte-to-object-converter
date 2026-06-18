package io.github.libedi.converter.exception;

/**
 * <p>
 * 리플렉션을 통한 필드 접근, 필드 값 할당, 생성자 호출 등의 작업이 실패할 때 발생하는 예외입니다.
 * </p>
 * <p>
 * 리플렉션은 런타임에 클래스 정보에 접근하고 필드를 조작하는 데 사용됩니다.
 * 다양한 이유로 리플렉션 작업이 실패할 수 있습니다:
 * </p>
 * <ul>
 * <li>대상 클래스의 기본 생성자에 접근할 수 없음</li>
 * <li>필드에 접근할 수 없음 (private이고 setAccessible 실패 등)</li>
 * <li>필드에 값을 할당할 수 없음</li>
 * <li>생성자 호출 중 예외 발생</li>
 * <li>필드 타입 불일치</li>
 * </ul>
 * <p>
 * 이 예외의 하위 클래스:
 * </p>
 * <ul>
 * <li>{@link FieldAccessException} - 필드 접근/할당 실패</li>
 * <li>{@link ConstructorInvocationException} - 생성자 호출 실패</li>
 * </ul>
 * <p>
 * 주의: 대상 Object는 반드시 기본 생성자(매개변수 없는 생성자)를 가져야 합니다.
 * private 생성자도 가능합니다.
 * </p>
 *
 * @author "Sangjun,Park"
 * @see ConvertFailException
 * @see FieldAccessException
 * @see ConstructorInvocationException
 */
public class ReflectionException extends ConvertFailException {

    private static final long serialVersionUID = 1L;

    /**
     * ReflectionException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public ReflectionException(final String message) {
        super(message);
    }

    /**
     * ReflectionException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public ReflectionException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
