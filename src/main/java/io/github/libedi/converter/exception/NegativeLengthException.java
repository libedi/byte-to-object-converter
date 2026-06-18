package io.github.libedi.converter.exception;

/**
 * <p>
 * {@link io.github.libedi.converter.annotation.ConvertData @ConvertData} annotation의
 * length(value) 속성이 유효하지 않은 음수인 경우 발생하는 예외입니다.
 * </p>
 * <p>
 * {@link io.github.libedi.converter.annotation.ConvertData#value()}의 유효한 값:
 * </p>
 * <ul>
 * <li>{@code > 0} - 고정 길이 (예: 10은 10 byte 읽음)</li>
 * <li>{@code = 0} - lengthField를 통한 동적 길이 지정</li>
 * <li>{@code = -1} - InputStream의 남은 모든 byte 읽음</li>
 * <li>{@code < -1} - 무효 (이 예외 발생)</li>
 * </ul>
 * <p>
 * 발생 상황:
 * </p>
 * <ul>
 * <li>{@code @ConvertData(-2)} 또는 더 작은 음수로 지정한 경우</li>
 * <li>lengthField도 지정하지 않으면서 음수 length를 사용한 경우</li>
 * </ul>
 * <p>
 * 올바른 사용:
 * </p>
 * <pre>
 * &#64;ConvertData(10)           // 10 byte 고정 길이
 * String fixed;
 *
 * &#64;ConvertData(-1)            // 남은 모든 byte 읽음
 * String remaining;
 *
 * &#64;ConvertData(lengthField = "length")  // 동적 길이
 * byte[] dynamic;
 * </pre>
 *
 * @author "Sangjun,Park"
 * @see InvalidAnnotationException
 * @see io.github.libedi.converter.annotation.ConvertData
 */
public class NegativeLengthException extends InvalidAnnotationException {

    private static final long serialVersionUID = 1L;

    /**
     * NegativeLengthException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public NegativeLengthException(final String message) {
        super(message);
    }

    /**
     * NegativeLengthException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public NegativeLengthException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
