package io.github.libedi.converter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.commons.lang3.StringUtils;

import io.github.libedi.converter.annotation.ConvertData;
import io.github.libedi.converter.exception.DateParsingException;
import io.github.libedi.converter.exception.NumberParsingException;
import io.github.libedi.converter.exception.TypeConversionException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * <p>
 * {@code specs/02-exception-hierarchy-consistency/design.md}의 Correctness Properties를 jqwik으로 검증하는
 * 테스트 클래스입니다.
 * </p>
 * <p>
 * {@link ByteToObjectConverterTest}(example-based test)와 분리해 임의 입력에 대한 보편 속성(Correctness
 * Property)만 다룹니다.
 * </p>
 */
class ByteToObjectConverterPropertyTest {

    private static final int FIELD_LENGTH = 10;
    private static final int LOCAL_DATE_FIELD_LENGTH = 8;
    private static final String LOCAL_DATE_FORMAT_PATTERN = "yyyyMMdd";
    private static final int MONTH_FIELD_LENGTH = 2;
    private static final int ENUM_FIELD_LENGTH = 10;

    private final ByteToObjectConverter converter = new ByteToObjectConverter(StandardCharsets.UTF_8);

    /**
     * <p>
     * Property 1: 숫자 Wrapper 타입 파싱 실패는 항상 {@link NumberParsingException}이다.
     * </p>
     * <p>
     * 숫자 Wrapper 타입 집합({@link Byte}, {@link Short}, {@link Integer}, {@link Long}, {@link Float},
     * {@link Double}) 중 임의의 하나를 필드 타입으로 갖는 필드에, 그 타입으로 파싱 불가능한(trim 후 비어있지 않은) 임의의
     * 문자열이 입력되면, 변환 시 항상 {@link NumberParsingException}이 던져지고 그 {@code getCause()}는
     * {@link InvocationTargetException}이 아닌 원본 파싱 예외({@link NumberFormatException})다.
     * </p>
     * <p>
     * Validates: Requirements 1.1, 1.2
     * </p>
     */
    @Property
    void numericWrapperParsingFailureAlwaysThrowsNumberParsingException(
            @ForAll("invalidNumericFailureCase") final NumericFailureCase failureCase) {
        final String data = StringUtils.rightPad(failureCase.rawValue(), FIELD_LENGTH);
        final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> converter.convert(inputStream, failureCase.fixtureType()))
                .isInstanceOf(NumberParsingException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(NumberFormatException.class);
    }

    /**
     * 숫자 Wrapper 6종의 픽스처 클래스 중 하나를 고르는 provider와, 해당 타입으로 파싱 불가능한 문자열을 생성하는
     * provider를 조합한다. {@code convert()}는 필드를 선언 순서대로 순차 처리하므로, 픽스처 클래스마다 필드를 정확히
     * 하나만 두어 항상 그 필드 하나에서만 예외가 발생하도록 한다.
     */
    @Provide
    Arbitrary<NumericFailureCase> invalidNumericFailureCase() {
        return Arbitraries.oneOf(
                invalidValueOf(ByteFieldFixture.class, Byte.class),
                invalidValueOf(ShortFieldFixture.class, Short.class),
                invalidValueOf(IntegerFieldFixture.class, Integer.class),
                invalidValueOf(LongFieldFixture.class, Long.class),
                invalidValueOf(FloatFieldFixture.class, Float.class),
                invalidValueOf(DoubleFieldFixture.class, Double.class));
    }

    /**
     * 지정된 Wrapper 타입으로 파싱 불가능한(trim 후 비어있지 않은) ASCII 문자열을 생성하는 Arbitrary를 만든다. 절단
     * (substring 등)은 우연히 파싱 가능한 접두어를 남기거나 공백만 남겨 blank→null 경로로 샐 수 있으므로 쓰지 않고,
     * {@code ofMaxLength}로 제한한 문자열을 그대로 사용한다. 파싱 가능 여부는 대상 Wrapper 타입의 실제
     * {@code valueOf(String)} 호출 성공/실패로 판정한다.
     */
    private static Arbitrary<NumericFailureCase> invalidValueOf(final Class<?> fixtureType,
            final Class<?> wrapperType) {
        return Arbitraries.strings().ascii().ofMaxLength(FIELD_LENGTH)
                // ConversionHelper#invokeSetValueByFieldType이 trim 후 blank 여부로 판단하므로, 이 필터도 반드시
                // 같은 기준(StringUtils.trim 이후 isNotBlank)으로 판단해야 한다. Character.isWhitespace 기준의
                // isNotBlank(value)만으로는 String.trim()이 제거하는 일부 제어 문자(예: 0x01)를 걸러내지 못한다.
                .filter(value -> StringUtils.isNotBlank(StringUtils.trim(value)))
                .filter(value -> !isParsable(wrapperType, StringUtils.trim(value)))
                .map(value -> new NumericFailureCase(fixtureType, value));
    }

    /**
     * {@code wrapperType.valueOf(String)}가 예외 없이 성공하는지 여부를 반환한다.
     */
    private static boolean isParsable(final Class<?> wrapperType, final String value) {
        try {
            final Method valueOf = wrapperType.getMethod("valueOf", String.class);
            valueOf.invoke(null, value);
            return true;
        } catch (final ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /**
     * <p>
     * Property 2 (1/3): {@code format} 패턴으로 파싱 불가능한 {@code java.time} date-time 타입 필드는 항상
     * {@link DateParsingException}이다.
     * </p>
     * <p>
     * {@code format = "yyyyMMdd"}인 {@link LocalDate} 필드에 그 패턴으로 파싱 불가능한(trim 후 비어있지 않은)
     * 임의의 문자열이 입력되면, 변환 시 항상 {@link DateParsingException}이 던져지고 그 {@code getCause()}는
     * {@link InvocationTargetException}이 아닌 원본 {@link DateTimeParseException}이다.
     * </p>
     * <p>
     * Validates: Requirements 2.1, 2.3
     * </p>
     */
    @Property
    void javaTimeDateParsingFailureAlwaysThrowsDateParsingException(
            @ForAll("invalidLocalDateValue") final String rawValue) {
        final String data = StringUtils.rightPad(rawValue, LOCAL_DATE_FIELD_LENGTH);
        final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> converter.convert(inputStream, LocalDateFieldFixture.class))
                .isInstanceOf(DateParsingException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(DateTimeParseException.class);
    }

    /**
     * {@code "yyyyMMdd"} 패턴으로 파싱 불가능한(trim 후 비어있지 않은) ASCII 문자열을 생성한다. 절단 없이
     * {@code ofMaxLength}로 제한한 문자열을 그대로 사용하고, 파싱 가능 여부는 실제 {@link LocalDate#parse}
     * 호출 성공/실패로 판정한다.
     */
    @Provide
    Arbitrary<String> invalidLocalDateValue() {
        return Arbitraries.strings().ascii().ofMaxLength(LOCAL_DATE_FIELD_LENGTH)
                // ConversionHelper#invokeSetValueByFieldType과 동일하게 trim 이후 blank 여부로 판정한다(9.1에서
                // 확인한 Character.isWhitespace vs String.trim 기준 불일치 문제를 반복하지 않기 위함).
                .filter(value -> StringUtils.isNotBlank(StringUtils.trim(value)))
                .filter(value -> !isParsableAsLocalDate(StringUtils.trim(value)));
    }

    private static boolean isParsableAsLocalDate(final String value) {
        try {
            LocalDate.parse(value, DateTimeFormatter.ofPattern(LOCAL_DATE_FORMAT_PATTERN));
            return true;
        } catch (final DateTimeParseException e) {
            return false;
        }
    }

    /**
     * <p>
     * Property 2 (2/3): 정수로 파싱되지 않는 {@link Month} 필드 값은 항상 {@link DateParsingException}이다.
     * </p>
     * <p>
     * {@link Month} 필드에 정수로 파싱 불가능한(trim 후 비어있지 않은) 임의의 문자열이 입력되면, 변환 시 항상
     * {@link DateParsingException}이 던져지고 그 {@code getCause()}는 {@link InvocationTargetException}이
     * 아닌 원본 {@link NumberFormatException}이다.
     * </p>
     * <p>
     * Validates: Requirements 2.2, 2.3
     * </p>
     */
    @Property
    void monthValueNotParsableAsIntegerAlwaysThrowsDateParsingException(
            @ForAll("invalidMonthIntegerValue") final String rawValue) {
        final String data = StringUtils.rightPad(rawValue, MONTH_FIELD_LENGTH);
        final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> converter.convert(inputStream, MonthFieldFixture.class))
                .isInstanceOf(DateParsingException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(NumberFormatException.class);
    }

    /**
     * 정수로 파싱 불가능한(trim 후 비어있지 않은) ASCII 문자열을 생성한다({@code Month} 필드 길이 2자 이하). 파싱
     * 메서드({@code parseMonth})가 내부적으로 {@link Integer#parseInt}를 쓰므로, 동등한 파싱 규칙을 갖는
     * {@link Integer#valueOf(String)} 성공/실패로 판정한다({@link #isParsable}을 재사용).
     */
    @Provide
    Arbitrary<String> invalidMonthIntegerValue() {
        return Arbitraries.strings().ascii().ofMaxLength(MONTH_FIELD_LENGTH)
                .filter(value -> StringUtils.isNotBlank(StringUtils.trim(value)))
                .filter(value -> !isParsable(Integer.class, StringUtils.trim(value)));
    }

    /**
     * <p>
     * Property 2 (3/3): 1~12 범위를 벗어난 {@link Month} 필드 값은 항상 {@link DateParsingException}이다.
     * </p>
     * <p>
     * {@link Month} 필드에 정수로는 파싱되지만 유효한 월 범위(1~12)를 벗어난 임의의 값이 입력되면, 변환 시 항상
     * {@link DateParsingException}이 던져지고 그 {@code getCause()}는 {@link InvocationTargetException}이
     * 아닌 원본 {@link DateTimeException}이다.
     * </p>
     * <p>
     * Validates: Requirements 2.2, 2.3
     * </p>
     */
    @Property
    void monthValueOutOfRangeAlwaysThrowsDateParsingException(
            @ForAll("outOfRangeMonthValue") final int monthValue) {
        // MONTH_FIELD_LENGTH(2바이트)가 표현 가능한 범위 안에서 1~12를 벗어난 값(13~99)은 이미 정확히 2자리이므로
        // 패딩이 필요 없다(design.md Property 2 구현 방법 참고).
        final String data = String.valueOf(monthValue);
        final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> converter.convert(inputStream, MonthFieldFixture.class))
                .isInstanceOf(DateParsingException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(DateTimeException.class);
    }

    /**
     * 1~12 범위를 벗어나면서 {@code MONTH_FIELD_LENGTH}(2바이트)에 담을 수 있는 정수(13~99)를 생성한다. 3자리
     * 이상 값은 2바이트 필드에 담을 수 없으므로 생성 범위에서 제외한다.
     */
    @Provide
    Arbitrary<Integer> outOfRangeMonthValue() {
        return Arbitraries.integers().between(13, 99);
    }

    /**
     * <p>
     * Property 3: Enum 타입 파싱 실패는 항상 하위 타입이 아닌 {@link TypeConversionException}이다.
     * </p>
     * <p>
     * Enum 타입({@link Month} 제외) 필드에 그 Enum의 유효한 상수명이 아닌(trim 후 비어있지 않은) 임의의 문자열이
     * 입력되면, 변환 시 항상 {@code getClass() == TypeConversionException.class}인 예외가 던져지고 그
     * {@code getCause()}는 {@link InvocationTargetException}이 아닌 원본 {@link IllegalArgumentException}이다.
     * </p>
     * <p>
     * Validates: Requirements 3.1, 3.2
     * </p>
     */
    @Property
    void enumParsingFailureAlwaysThrowsTypeConversionException(
            @ForAll("invalidEnumConstantName") final String rawValue) {
        final String data = StringUtils.rightPad(rawValue, ENUM_FIELD_LENGTH);
        final InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> converter.convert(inputStream, EnumFieldFixture.class))
                .isExactlyInstanceOf(TypeConversionException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * {@link FixtureEnum}의 유효한 상수명이 아닌(trim 후 비어있지 않은) ASCII 문자열을 생성한다. 절단 없이
     * {@code ofMaxLength}로 제한한 문자열을 그대로 사용한다. Enum 타입도 컴파일러가 생성한 공개 정적
     * {@code valueOf(String)} 메서드를 가지므로 {@link #isParsable}을 그대로 재사용해 파싱 가능 여부를 판정한다.
     */
    @Provide
    Arbitrary<String> invalidEnumConstantName() {
        return Arbitraries.strings().ascii().ofMaxLength(ENUM_FIELD_LENGTH)
                // ConversionHelper#invokeSetValueByFieldType과 동일하게 trim 이후 blank 여부로 판정한다(9.1에서
                // 확인한 Character.isWhitespace vs String.trim 기준 불일치 문제를 반복하지 않기 위함).
                .filter(value -> StringUtils.isNotBlank(StringUtils.trim(value)))
                .filter(value -> !isParsable(FixtureEnum.class, StringUtils.trim(value)));
    }

    /**
     * 파싱 실패를 재현할 픽스처 클래스와, 그 필드 타입으로 파싱 불가능한 원본 문자열(패딩 전) 조합.
     */
    private record NumericFailureCase(Class<?> fixtureType, String rawValue) {
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class ByteFieldFixture {
        @ConvertData(FIELD_LENGTH)
        private Byte value;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class ShortFieldFixture {
        @ConvertData(FIELD_LENGTH)
        private Short value;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class IntegerFieldFixture {
        @ConvertData(FIELD_LENGTH)
        private Integer value;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class LongFieldFixture {
        @ConvertData(FIELD_LENGTH)
        private Long value;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class FloatFieldFixture {
        @ConvertData(FIELD_LENGTH)
        private Float value;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class DoubleFieldFixture {
        @ConvertData(FIELD_LENGTH)
        private Double value;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class LocalDateFieldFixture {
        @ConvertData(value = LOCAL_DATE_FIELD_LENGTH, format = LOCAL_DATE_FORMAT_PATTERN)
        private LocalDate value;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class MonthFieldFixture {
        @ConvertData(MONTH_FIELD_LENGTH)
        private Month value;
    }

    /**
     * <p>
     * Property 3 검증에 쓰이는 테스트용 Enum 상수명 집합({@code ByteToObjectConverterTest.Week}와 동일한
     * 목적이나, 이 파일을 자기 완결적으로 유지하기 위해 별도로 둔다).
     * </p>
     * <p>
     * {@code public}이어야 한다: Enum이 컴파일러가 생성하는 {@code valueOf(String)}은 항상 public static이지만,
     * 선언 클래스 자체가 public이 아니면 {@code MethodUtils.invokeStaticMethod}(리플렉션)가 그 메서드를 호출할 때
     * {@code IllegalAccessException}을 던진다 — 이는 이 스펙이 다루는 "파싱 실패"가 아니라 순수 리플렉션 접근성
     * 문제이므로, 의도치 않게 {@code FieldAccessException}으로 귀결되어 Property 3 자체를 검증하지 못하게 된다.
     * </p>
     */
    public enum FixtureEnum {
        MON, TUE, WED, THU, FRI, SAT, SUN;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class EnumFieldFixture {
        @ConvertData(ENUM_FIELD_LENGTH)
        private FixtureEnum value;
    }

}
