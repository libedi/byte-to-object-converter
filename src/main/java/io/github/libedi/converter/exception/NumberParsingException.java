package io.github.libedi.converter.exception;

/**
 * <p>
 * {@link Integer}, {@link Long}, {@link Double}, {@link Float} 등의 숫자 타입으로
 * 문자열을 파싱하는 과정에서 실패할 때 발생하는 예외입니다.
 * </p>
 * <p>
 * 발생 상황:
 * </p>
 * <ul>
 * <li>문자열에 숫자가 아닌 문자가 포함된 경우</li>
 * <li>숫자 범위를 초과하는 경우 (예: Long의 범위를 벗어난 값)</li>
 * <li>부동소수점 형식이 잘못된 경우</li>
 * <li>공백을 포함한 문자열인 경우</li>
 * </ul>
 * <p>
 * 예:
 * </p>
 * <pre>
 * // byte data = "abc" (숫자 아님)
 * &#64;ConvertData(3)
 * int count;  // NumberParsingException 발생
 *
 * // byte data = "999" (int 범위에는 있지만 타입 불일치 등)
 * &#64;ConvertData(3)
 * byte value;  // NumberParsingException 발생 가능
 *
 * // byte data = "12.34.56" (형식 오류)
 * &#64;ConvertData(8)
 * double amount;  // NumberParsingException 발생
 * </pre>
 * <p>
 * 참고: 문자열을 trim 처리한 후 파싱을 시도하므로, 앞뒤 공백은 제거됩니다.
 * </p>
 *
 * @author "Sangjun,Park"
 * @see TypeConversionException
 * @see io.github.libedi.converter.annotation.ConvertData
 */
public class NumberParsingException extends TypeConversionException {

    private static final long serialVersionUID = 1L;

    /**
     * NumberParsingException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public NumberParsingException(final String message) {
        super(message);
    }

    /**
     * NumberParsingException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public NumberParsingException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
