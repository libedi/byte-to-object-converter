package io.github.libedi.converter.exception;

/**
 * <p>
 * 객체의 필드에 접근하거나 값을 할당하는 과정에서 실패할 때 발생하는 예외입니다.
 * </p>
 * <p>
 * 발생 상황:
 * </p>
 * <ul>
 * <li>존재하지 않는 필드에 접근하려는 경우</li>
 * <li>private 필드에 접근하려고 했지만 setAccessible이 실패한 경우</li>
 * <li>필드 값을 할당할 수 없는 경우 (타입 불일치 등)</li>
 * <li>필드가 final이고 변경 시도한 경우</li>
 * <li>Security Manager가 필드 접근을 차단한 경우</li>
 * <li>lengthField로 지정된 필드가 존재하지 않는 경우</li>
 * <li>countField로 지정된 필드가 존재하지 않는 경우</li>
 * </ul>
 * <p>
 * 예:
 * </p>
 * <pre>
 * // lengthField가 존재하지 않는 경우
 * &#64;ConvertData(lengthField = "nonExistentField")
 * byte[] data;  // FieldAccessException 발생
 *
 * // countField가 존재하지 않는 경우
 * &#64;Iteration(countField = "noSuchCount")
 * List&lt;Item&gt; items;  // FieldAccessException 발생
 * </pre>
 *
 * @author "Sangjun,Park"
 * @see ReflectionException
 */
public class FieldAccessException extends ReflectionException {

    private static final long serialVersionUID = 1L;

    /**
     * FieldAccessException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public FieldAccessException(final String message) {
        super(message);
    }

    /**
     * FieldAccessException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public FieldAccessException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
