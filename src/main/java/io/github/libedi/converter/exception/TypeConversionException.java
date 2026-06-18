package io.github.libedi.converter.exception;

/**
 * <p>
 * byte 데이터를 특정 타입으로 변환(파싱)하는 과정에서 실패할 때 발생하는 예외입니다.
 * </p>
 * <p>
 * 지원되는 타입으로의 변환 과정에서 다음과 같은 상황에서 발생합니다:
 * </p>
 * <ul>
 * <li>문자열이 숫자 형식이 아니어서 Integer, Long 등으로 파싱 실패 ({@link NumberParsingException})</li>
 * <li>문자열을 지정된 날짜 포맷으로 파싱 실패 ({@link DateParsingException})</li>
 * <li>문자열을 Enum 값으로 변환 실패</li>
 * <li>사용자 정의 타입 변환 실패</li>
 * </ul>
 * <p>
 * 이 예외의 하위 클래스:
 * </p>
 * <ul>
 * <li>{@link DateParsingException} - 날짜 파싱 실패</li>
 * <li>{@link NumberParsingException} - 숫자 파싱 실패</li>
 * </ul>
 * <p>
 * 예:
 * </p>
 * <pre>
 * &#64;ConvertData(3)
 * int count;  // byte가 "abc"인 경우 NumberParsingException 발생
 *
 * &#64;ConvertData(value = 8, format = "yyyyMMdd")
 * LocalDate date;  // byte가 "20241399"인 경우 DateParsingException 발생
 * </pre>
 *
 * @author "Sangjun,Park"
 * @see ConvertFailException
 * @see DateParsingException
 * @see NumberParsingException
 */
public class TypeConversionException extends ConvertFailException {

    private static final long serialVersionUID = 1L;

    /**
     * TypeConversionException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public TypeConversionException(final String message) {
        super(message);
    }

    /**
     * TypeConversionException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public TypeConversionException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
