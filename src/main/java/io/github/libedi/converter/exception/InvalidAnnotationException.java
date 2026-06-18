package io.github.libedi.converter.exception;

/**
 * <p>
 * {@link io.github.libedi.converter.annotation.ConvertData @ConvertData},
 * {@link io.github.libedi.converter.annotation.Iteration @Iteration},
 * {@link io.github.libedi.converter.annotation.Embeddable @Embeddable}
 * 등의 annotation 설정이 유효하지 않거나 불완전한 경우 발생하는 예외입니다.
 * </p>
 * <p>
 * 발생 상황:
 * </p>
 * <ul>
 * <li>날짜 타입 필드에 format 속성이 누락된 경우 ({@link MissingFormatException})</li>
 * <li>ConvertData 속성의 length 값이 -1 미만인 음수인 경우 ({@link NegativeLengthException})</li>
 * <li>Iteration annotation의 value와 countField가 모두 설정되지 않은 경우</li>
 * <li>List 필드에 제네릭 타입이 지정되지 않은 경우</li>
 * </ul>
 * <p>
 * 이 예외의 하위 클래스:
 * </p>
 * <ul>
 * <li>{@link MissingFormatException} - 날짜 포맷 누락</li>
 * <li>{@link NegativeLengthException} - 음수 길이 설정</li>
 * </ul>
 *
 * @author "Sangjun,Park"
 * @see ConvertFailException
 * @see MissingFormatException
 * @see NegativeLengthException
 * @see io.github.libedi.converter.annotation.ConvertData
 * @see io.github.libedi.converter.annotation.Iteration
 * @see io.github.libedi.converter.annotation.Embeddable
 */
public class InvalidAnnotationException extends ConvertFailException {

    private static final long serialVersionUID = 1L;

    /**
     * InvalidAnnotationException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public InvalidAnnotationException(final String message) {
        super(message);
    }

    /**
     * InvalidAnnotationException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public InvalidAnnotationException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
