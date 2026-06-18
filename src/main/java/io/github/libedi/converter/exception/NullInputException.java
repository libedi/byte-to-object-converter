package io.github.libedi.converter.exception;

/**
 * <p>
 * 변환/역변환 시 필수 입력값이 null인 경우 발생하는 예외입니다.
 * </p>
 * <p>
 * 발생 상황:
 * </p>
 * <ul>
 * <li>{@link io.github.libedi.converter.ByteToObjectConverter#convert(java.io.InputStream, Class)}에서
 *     {@link java.io.InputStream}이 null인 경우</li>
 * <li>{@link io.github.libedi.converter.ByteToObjectConverter#convert(java.io.InputStream, Class)}에서
 *     target class가 null인 경우</li>
 * <li>{@link io.github.libedi.converter.ByteToObjectConverter#deconvert(Object, io.github.libedi.converter.DataAlignment)}에서
 *     targetObject가 null인 경우</li>
 * </ul>
 *
 * @author "Sangjun,Park"
 * @see ValidationException
 */
public class NullInputException extends ValidationException {

    private static final long serialVersionUID = 1L;

    /**
     * NullInputException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public NullInputException(final String message) {
        super(message);
    }

    /**
     * NullInputException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public NullInputException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
