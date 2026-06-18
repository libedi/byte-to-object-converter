package io.github.libedi.converter.exception;

/**
 * <p>
 * {@link io.github.libedi.converter.annotation.ConvertData @ConvertData} annotation의
 * format 속성이 누락된 경우 발생하는 예외입니다.
 * </p>
 * <p>
 * 날짜/시간 타입(예: {@link java.time.LocalDate}, {@link java.time.LocalDateTime} 등)의
 * 필드를 변환할 때 format 속성이 반드시 필요합니다. 이 속성이 없거나 빈 문자열인 경우 이 예외가 발생합니다.
 * </p>
 * <p>
 * 발생 상황:
 * </p>
 * <ul>
 * <li>LocalDate 필드에 {@code @ConvertData(8)} 만 지정하고 format을 누락한 경우</li>
 * <li>LocalDateTime 필드에 format을 빈 문자열로 지정한 경우</li>
 * </ul>
 * <p>
 * 올바른 사용 예:
 * </p>
 * <pre>
 * &#64;ConvertData(value = 8, format = "yyyyMMdd")
 * LocalDate date;
 * </pre>
 *
 * @author "Sangjun,Park"
 * @see InvalidAnnotationException
 * @see io.github.libedi.converter.annotation.ConvertData
 */
public class MissingFormatException extends InvalidAnnotationException {

    private static final long serialVersionUID = 1L;

    /**
     * MissingFormatException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public MissingFormatException(final String message) {
        super(message);
    }

    /**
     * MissingFormatException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public MissingFormatException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
