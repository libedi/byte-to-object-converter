package io.github.libedi.converter.exception;

/**
 * <p>
 * 변환/역변환 과정에서 입력값 검증이 실패할 때 발생하는 예외입니다.
 * </p>
 * <p>
 * 이 예외는 다음과 같은 상황에서 발생합니다:
 * </p>
 * <ul>
 * <li>필수 입력값이 null인 경우 (예: {@link java.io.InputStream}, 대상 Object)</li>
 * <li>입력 데이터가 유효하지 않은 경우</li>
 * </ul>
 * <p>
 * 이 예외의 하위 클래스:
 * </p>
 * <ul>
 * <li>{@link NullInputException} - null 입력값 처리</li>
 * </ul>
 *
 * @author "Sangjun,Park"
 * @see ConvertFailException
 * @see NullInputException
 */
public class ValidationException extends ConvertFailException {

    private static final long serialVersionUID = 1L;

    /**
     * ValidationException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public ValidationException(final String message) {
        super(message);
    }

    /**
     * ValidationException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public ValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
