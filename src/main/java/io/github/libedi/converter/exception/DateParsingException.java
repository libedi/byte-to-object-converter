package io.github.libedi.converter.exception;

/**
 * <p>
 * {@link java.time.LocalDate}, {@link java.time.LocalDateTime}, {@link java.time.ZonedDateTime}
 * 등의 date-time 타입(달력 개념인 {@link java.time.Month} 포함)으로 문자열을 파싱하는 과정에서 실패할 때
 * 발생하는 예외입니다.
 * </p>
 * <p>
 * 발생 상황:
 * </p>
 * <ul>
 * <li>문자열 형식이 지정된 날짜 포맷과 맞지 않는 경우</li>
 * <li>존재하지 않는 날짜 값인 경우 (예: 2024-02-30)</li>
 * <li>시간 형식이 잘못된 경우</li>
 * <li>{@link java.time.Month} 값이 정수로 변환되지 않는 경우</li>
 * <li>{@link java.time.Month} 값이 정수로 변환되더라도 유효한 월 범위(1~12)를 벗어나는 경우</li>
 * </ul>
 * <p>
 * 예:
 * </p>
 * <pre>
 * // format = "yyyyMMdd", byte data = "20241399" (잘못된 월)
 * &#64;ConvertData(value = 8, format = "yyyyMMdd")
 * LocalDate date;  // DateParsingException 발생
 *
 * // format = "yyyyMMdd", byte data = "2024-01-01" (포맷 불일치)
 * &#64;ConvertData(value = 10, format = "yyyyMMdd")
 * LocalDate date;  // DateParsingException 발생
 *
 * // byte data = "ab" (정수로 변환 불가)
 * &#64;ConvertData(2)
 * Month month;  // DateParsingException 발생
 *
 * // byte data = "13" (1~12 범위 초과)
 * &#64;ConvertData(2)
 * Month month;  // DateParsingException 발생
 * </pre>
 *
 * @author "Sangjun,Park"
 * @see TypeConversionException
 * @see io.github.libedi.converter.annotation.ConvertData
 */
public class DateParsingException extends TypeConversionException {

    private static final long serialVersionUID = 1L;

    /**
     * DateParsingException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public DateParsingException(final String message) {
        super(message);
    }

    /**
     * DateParsingException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public DateParsingException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
