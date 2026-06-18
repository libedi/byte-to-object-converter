package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.libedi.converter.ByteToObjectConverter;

/**
 * <p>
 * 변환/역변환할 필드를 지정하기 위한 annotation입니다.
 * </p>
 * <p>
 * 이 annotation은 {@link ByteToObjectConverter}가 byte 데이터로부터 어떤 필드를
 * 어떻게 변환할지 지정합니다. 필드 변환 순서는 클래스에 선언한 필드 순서를 따릅니다.
 * </p>
 * <p>
 * <strong>지원되는 필드 타입:</strong>
 * </p>
 * <ul>
 * <li><code>byte[]</code> - 원본 byte 데이터 유지</li>
 * <li>{@link String} - 문자열</li>
 * <li>Enum Type - enum 타입 (상수명으로 변환)</li>
 * <li>Primitive 타입 및 Wrapper 클래스
 *   ({@code int}, {@code long}, {@code double}, {@code boolean} 등,
 *   {@code void} 제외)</li>
 * <li>java.time 패키지 date-time 클래스
 *   ({@code LocalDate}, {@code LocalDateTime}, {@code ZonedDateTime}, {@code Year}, {@code YearMonth} 등)</li>
 * <li>사용자 정의 타입 (ByteToObjectConverter 확장 시)</li>
 * <li>{@link java.time.Month} - 월 enum</li>
 * </ul>
 * <p>
 * <strong>길이 지정 방식:</strong>
 * </p>
 * <ul>
 * <li>{@code value > 0} - 고정 길이 (기본값). 예: {@code @ConvertData(10)}는 10 byte 읽음</li>
 * <li>{@code value == 0} - {@link #lengthField()} 속성으로 길이 지정.
 *     해당 필드는 반드시 {@code int} 타입이어야 함</li>
 * <li>{@code value == -1} - InputStream의 남은 모든 byte 읽음 (변환 시에만)</li>
 * </ul>
 * <p>
 * <strong>날짜-시간 타입 format 속성:</strong>
 * </p>
 * <p>
 * java.time 패키지의 date-time 타입을 사용하려면 {@link #format()}에
 * {@link java.time.format.DateTimeFormatter}의 패턴을 지정해야 합니다.
 * </p>
 * <p>
 * <strong>사용 예:</strong>
 * </p>
 *
 * <pre>
 * // 고정 길이 - 14 byte 문자열
 * &#64;ConvertData(14)
 * String name;
 *
 * // 고정 길이 - 4 byte int
 * &#64;ConvertData(4)
 * int quantity;
 *
 * // 동적 길이 - 다른 필드(length)의 값만큼 읽음
 * &#64;ConvertData(4)
 * int length;
 *
 * &#64;ConvertData(lengthField = "length")
 * byte[] payload;
 *
 * // 날짜 - 8 byte, yyyyMMdd 형식
 * &#64;ConvertData(value = 8, format = "yyyyMMdd")
 * LocalDate dateOfBirth;
 *
 * // 날짜-시간 - 19 byte, yyyy-MM-dd HH:mm:ss 형식
 * &#64;ConvertData(value = 19, format = "yyyy-MM-dd HH:mm:ss")
 * LocalDateTime timestamp;
 *
 * // 남은 모든 데이터 읽기 (변환 시에만)
 * &#64;ConvertData(-1)
 * String remaining;
 * </pre>
 * <p>
 * <strong>역변환 시 주의사항:</strong>
 * </p>
 * <p>
 * Object를 byte[]로 역변환할 때:
 * </p>
 * <ul>
 * <li>value가 0이면 lengthField로 지정된 필드의 현재 값을 사용합니다</li>
 * <li>value가 -1이면 패딩 없이 원본 데이터 길이만큼 사용합니다</li>
 * <li>부족한 부분은 {@link io.github.libedi.converter.DataAlignment}에 따라 패딩됩니다</li>
 * </ul>
 *
 * @author "Sangjun,Park"
 * @see ByteToObjectConverter
 * @see Iteration
 * @see Embeddable
 * @see io.github.libedi.converter.DataAlignment
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface ConvertData {

    /**
     * <p>
     * 읽어올(또는 역변환할) byte 길이를 지정합니다.
     * </p>
     * <p>
     * <ul>
     * <li>{@code > 0} - 고정 길이. 예: 10이면 10 byte 읽음</li>
     * <li>{@code == 0} (기본값) - {@link #lengthField()}로 길이 지정</li>
     * <li>{@code == -1} - InputStream의 남은 모든 byte 읽음 (변환 시에만)</li>
     * </ul>
     * </p>
     *
     * @return byte 길이 (기본값: 0)
     */
    int value() default 0;

    /**
     * <p>
     * 읽어올 byte 길이가 정의된 다른 필드의 이름을 지정합니다.
     * </p>
     * <p>
     * {@link #value()}가 0일 때 사용되며, 지정된 필드는 반드시 {@code int} 타입이어야 합니다.
     * </p>
     * <p>
     * 예: {@code lengthField = "dataLength"}는 클래스의 {@code int dataLength} 필드의
     * 값만큼 현재 필드에서 byte를 읽습니다.
     * </p>
     * <p>
     * 동적 길이가 필요하지 않으면 빈 문자열("")을 유지하고, {@link #value()}에 고정 길이를 지정하세요.
     * </p>
     *
     * @return 길이 필드명 (기본값: "")
     */
    String lengthField() default "";

    /**
     * <p>
     * java.time 패키지 date-time 타입의 파싱/포맷 형식을 지정합니다.
     * </p>
     * <p>
     * 필드 타입이 {@code LocalDate}, {@code LocalDateTime}, {@code ZonedDateTime},
     * {@code Year}, {@code YearMonth}, {@code LocalTime}, {@code OffsetDateTime},
     * {@code OffsetTime} 등일 때 반드시 지정해야 합니다.
     * </p>
     * <p>
     * 형식은 {@link java.time.format.DateTimeFormatter}의 패턴을 따릅니다:
     * </p>
     * <ul>
     * <li>{@code "yyyyMMdd"} - LocalDate (예: 20240125)</li>
     * <li>{@code "yyyy-MM-dd"} - LocalDate (예: 2024-01-25)</li>
     * <li>{@code "yyyy-MM-dd HH:mm:ss"} - LocalDateTime (예: 2024-01-25 14:30:00)</li>
     * <li>{@code "HH:mm:ss"} - LocalTime</li>
     * <li>{@code "yyyy"} - Year</li>
     * <li>{@code "yyyy-MM"} - YearMonth</li>
     * </ul>
     * <p>
     * 형식이 지정되지 않으면 {@link io.github.libedi.converter.exception.MissingFormatException}이 발생합니다.
     * </p>
     *
     * @return date-time 포맷 패턴 (기본값: "")
     * @see java.time.format.DateTimeFormatter
     */
    String format() default "";

}
