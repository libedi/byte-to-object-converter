package io.github.libedi.converter.exception;

/**
 * <p>
 * byte 데이터와 Object 간의 변환/역변환 과정에서 발생하는 모든 예외의 기본 추상 클래스입니다.
 * </p>
 * <p>
 * 이 클래스는 런타임 예외({@link RuntimeException})를 상속하며, 변환 과정에서 발생하는 다양한 문제
 * (입력 검증 실패, 타입 변환 실패, annotation 설정 오류, 리플렉션 오류 등)를 나타냅니다.
 * </p>
 * <p>
 * 예외 계층 구조:
 * </p>
 * <ul>
 * <li>{@link ConvertFailException} - 모든 변환 관련 예외의 기본 클래스
 *   <ul>
 *     <li>{@link ValidationException} - 입력값 검증 실패
 *       <ul>
 *         <li>{@link NullInputException} - null 입력</li>
 *       </ul>
 *     </li>
 *     <li>{@link InvalidAnnotationException} - annotation 설정 오류
 *       <ul>
 *         <li>{@link MissingFormatException} - 날짜 포맷 누락</li>
 *         <li>{@link NegativeLengthException} - 음수 길이 설정</li>
 *       </ul>
 *     </li>
 *     <li>{@link TypeConversionException} - 타입 변환/파싱 실패
 *       <ul>
 *         <li>{@link DateParsingException} - 날짜 파싱 실패</li>
 *         <li>{@link NumberParsingException} - 숫자 파싱 실패</li>
 *       </ul>
 *     </li>
 *     <li>{@link ReflectionException} - 리플렉션 작업 실패
 *       <ul>
 *         <li>{@link FieldAccessException} - 필드 접근/할당 실패</li>
 *         <li>{@link ConstructorInvocationException} - 생성자 호출 실패</li>
 *       </ul>
 *     </li>
 *   </ul>
 * </li>
 * </ul>
 * <p>
 * 사용 예:
 * </p>
 * <pre>
 * try {
 *     ByteToObjectConverter converter = new ByteToObjectConverter("UTF-8");
 *     MyObject obj = converter.convert(inputStream, MyObject.class);
 * } catch (ConvertFailException e) {
 *     // 변환 실패 처리
 *     System.err.println("변환 실패: " + e.getMessage());
 * }
 * </pre>
 *
 * @author "Sangjun,Park"
 * @see ValidationException
 * @see InvalidAnnotationException
 * @see TypeConversionException
 * @see ReflectionException
 */
public abstract class ConvertFailException extends RuntimeException {

    private static final long serialVersionUID = -6517334134314384506L;

    /**
     * ConvertFailException 생성자 - 메시지와 함께 예외 생성
     *
     * @param message 예외 메시지
     */
    public ConvertFailException(final String message) {
        super(message);
    }

    /**
     * ConvertFailException 생성자 - 메시지와 원인 예외와 함께 생성
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public ConvertFailException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
