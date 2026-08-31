package io.github.libedi.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;

import autoparams.AutoSource;
import autoparams.customization.Customization;
import autoparams.lombok.BuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.libedi.converter.exception.ConvertFailException;
import io.github.libedi.converter.exception.DateParsingException;
import io.github.libedi.converter.exception.FieldAccessException;
import io.github.libedi.converter.exception.NumberParsingException;
import io.github.libedi.converter.exception.TypeConversionException;
import io.github.libedi.converter.annotation.ConvertData;
import io.github.libedi.converter.annotation.Embeddable;
import io.github.libedi.converter.annotation.Ignorable;
import io.github.libedi.converter.annotation.Iteration;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

class ByteToObjectConverterTest {

    static final Charset DATA_CHARSET = StandardCharsets.UTF_8;
    static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    static final String DATE_FORMAT = "yyyy-MM-dd";

    ByteToObjectConverter converter;

    @BeforeEach
    void init() {
        converter = new ByteToObjectConverter(DATA_CHARSET);
        assertThat(converter).isNotNull();
    }

    @DisplayName("byte 데이터를 Object로 변환")
    @ParameterizedTest
    @AutoSource
    @Customization(BuilderCustomizer.class)
    void convert(final TestObject expected) throws Exception {
        // given
        FieldUtils.writeField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT)), true); // truncate milliseconds
        FieldUtils.writeField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size(), true);
        FieldUtils.writeField(expected, "voList", expected.getVoList().subList(0, 2), true);
        FieldUtils.writeField(expected, "ignorable", null, true);

        final InputStream inputStream = new ByteArrayInputStream(convertTestData(expected));

        // when
        final TestObject actual = converter.convert(inputStream, TestObject.class);

        // then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @DisplayName("byte 데이터가 없어도 대상 Object의 List 필드는 empty List가 반환되는지 테스트")
    @ParameterizedTest
    @AutoSource
    @Customization(BuilderCustomizer.class)
    void convert_whenNoDataThenReturnEmptyListField() {
        // given
        final InputStream inputStream = new ByteArrayInputStream(new byte[] {});

        // when
        final TestObject actual = converter.convert(inputStream, TestObject.class);

        // then
        assertThat(actual).isNotNull();
        assertThat(actual.getVoList()).isNotNull().isEmpty();
    }

    @DisplayName("Object를 byte[]로 변환")
    @ParameterizedTest
    @AutoSource
    @Customization(BuilderCustomizer.class)
    void deconvert(final TestObject expected) throws Exception {
        // given
        FieldUtils.writeField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT)), true); // truncate milliseconds
        FieldUtils.writeField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size(), true);
        FieldUtils.writeField(expected, "voList", expected.getVoList().subList(0, 2), true);
        FieldUtils.writeField(expected, "ignorable", null, true);

        final byte[] expectedBytes = convertTestData(expected);

        // when
        final byte[] actual = converter.deconvert(expected, DataAlignment.LEFT);

        // then
        assertThat(actual).isEqualTo(expectedBytes);
    }

    @DisplayName("Object를 byte[]로 변환 - DataAlignment.RIGHT (왼쪽 패딩)")
    @ParameterizedTest
    @AutoSource
    @Customization(BuilderCustomizer.class)
    void testDeconvert_DataAlignmentRIGHT(final TestObject expected) throws Exception {
        // given
        FieldUtils.writeField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT)), true); // truncate milliseconds
        FieldUtils.writeField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size(), true);
        FieldUtils.writeField(expected, "voList", expected.getVoList().subList(0, 2), true);
        FieldUtils.writeField(expected, "ignorable", null, true);

        // when
        final byte[] rightAligned = converter.deconvert(expected, DataAlignment.RIGHT);
        final byte[] leftAligned = converter.deconvert(expected, DataAlignment.LEFT);

        // then
        assertThat(rightAligned).isNotEmpty();
        // RIGHT 정렬은 데이터 앞에 패딩이 있으므로 LEFT와 다른 결과여야 함
        assertThat(rightAligned).isNotEqualTo(leftAligned);

        // RIGHT alignment는 첫 번째 필드(intValue, 15자)의 앞에 공백이 있어야 함
        final String rightAlignedStr = new String(rightAligned, DATA_CHARSET).substring(0, 15);
        final String leftAlignedStr = new String(leftAligned, DATA_CHARSET).substring(0, 15);
        // RIGHT는 왼쪽에 공백이 있고, LEFT는 오른쪽에 공백이 있음
        assertThat(rightAlignedStr.trim()).isEqualTo(leftAlignedStr.trim()); // 실제 데이터는 같음
        assertThat(rightAlignedStr.charAt(0)).isEqualTo(' '); // RIGHT는 왼쪽에 공백
        assertThat(leftAlignedStr.charAt(leftAlignedStr.length() - 1)).isEqualTo(' '); // LEFT는 오른쪽에 공백
    }

    @DisplayName("@Ignorable 필드가 null이 아닐 때 정상 직렬화")
    @ParameterizedTest
    @AutoSource
    @Customization(BuilderCustomizer.class)
    void testDeconvert_IgnorableNonNullField(final TestObject expected) throws Exception {
        // given
        FieldUtils.writeField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT)), true); // truncate milliseconds
        FieldUtils.writeField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size(), true);
        FieldUtils.writeField(expected, "voList", expected.getVoList().subList(0, 2), true);
        FieldUtils.writeField(expected, "ignorable", "testValue", true); // Ignorable 필드에 값 설정

        // when
        final byte[] actual = converter.deconvert(expected, DataAlignment.LEFT);

        // then
        assertThat(actual).isNotEmpty();
        // ignorable 필드가 포함되어야 하므로 결과는 null일 때보다 더 길어야 함
        FieldUtils.writeField(expected, "ignorable", null, true);
        final byte[] withoutIgnorable = converter.deconvert(expected, DataAlignment.LEFT);
        assertThat(actual.length).isGreaterThan(withoutIgnorable.length);
    }

    @DisplayName("@Ignorable 필드가 null일 때 제외")
    @ParameterizedTest
    @AutoSource
    @Customization(BuilderCustomizer.class)
    void testDeconvert_IgnorableNullField(final TestObject expected) throws Exception {
        // given
        FieldUtils.writeField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT)), true); // truncate milliseconds
        FieldUtils.writeField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size(), true);
        FieldUtils.writeField(expected, "voList", expected.getVoList().subList(0, 2), true);
        FieldUtils.writeField(expected, "ignorable", null, true); // Ignorable 필드를 null로 명시

        // when
        final byte[] resultWithNullIgnorable = converter.deconvert(expected, DataAlignment.LEFT);

        // then - ignorable 필드가 null이므로 해당 필드는 제외되고 직렬화됨
        assertThat(resultWithNullIgnorable).isNotEmpty();

        // ignorable 필드가 null일 때와 값이 있을 때의 바이트 길이 비교
        FieldUtils.writeField(expected, "ignorable", "testValue", true);
        final byte[] resultWithValueIgnorable = converter.deconvert(expected, DataAlignment.LEFT);
        // null일 때는 ignorable 필드가 제외되므로 더 작은 바이트 길이여야 함
        assertThat(resultWithNullIgnorable.length).isLessThan(resultWithValueIgnorable.length);
    }

    @DisplayName("deconvert() 메서드 - null targetObject 입력 시 예외 발생")
    @Test
    void testDeconvert_NullTargetObjectThrowsException() {
        // when & then
        assertThatThrownBy(() -> converter.deconvert(null, DataAlignment.LEFT))
                .isInstanceOf(ConvertFailException.class);
    }

    @DisplayName("숫자 Wrapper 타입 필드 파싱 실패 시 NumberParsingException 발생 (cause는 원본 NumberFormatException)")
    @Test
    void convert_whenNumericWrapperFieldParsingFails_thenThrowsNumberParsingException() {
        // given
        final String invalidNumber = StringUtils.rightPad("abc", 15); // intValue: @ConvertData(15)
        final InputStream inputStream = new ByteArrayInputStream(invalidNumber.getBytes(DATA_CHARSET));

        // when & then
        assertThatThrownBy(() -> converter.convert(inputStream, TestObject.class))
                .isInstanceOf(NumberParsingException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(NumberFormatException.class);
    }

    @DisplayName("Enum 타입 필드 파싱 실패 시 하위 타입이 아닌 TypeConversionException 발생 (cause는 원본 IllegalArgumentException)")
    @Test
    void convert_whenEnumFieldParsingFails_thenThrowsTypeConversionException() {
        // given
        // TestObject 필드 선언 순서: intValue, longValue, doubleValue, stringValue, monthValue, dateValue,
        // dateTimeValue, enumValue(파싱 실패 대상, 유효하지 않은 Week 상수명) 순으로 읽히므로 enumValue 앞의 필드는
        // 모두 유효한 값으로 채워 enumValue에서만 예외가 발생하도록 한다.
        final String dataString =
                StringUtils.rightPad("1", 15)                          // intValue
                        + StringUtils.rightPad("1", 30)                // longValue
                        + StringUtils.rightPad("1.0", 30)               // doubleValue
                        + StringUtils.rightPad("test", 40)              // stringValue
                        + StringUtils.rightPad("1", 2)                  // monthValue
                        + StringUtils.rightPad("2024-01-01", 10)        // dateValue
                        + StringUtils.rightPad("2024-01-01 00:00:00", 19) // dateTimeValue
                        + StringUtils.rightPad("XXX", 3);               // enumValue: 유효하지 않은 Week 상수명
        final InputStream inputStream = new ByteArrayInputStream(dataString.getBytes(DATA_CHARSET));

        // when & then
        assertThatThrownBy(() -> converter.convert(inputStream, TestObject.class))
                .isExactlyInstanceOf(TypeConversionException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("Month 필드 값이 정수로 파싱되지 않으면 DateParsingException 발생 (cause는 원본 NumberFormatException)")
    @Test
    void convert_whenMonthFieldValueIsNotParsableAsInteger_thenThrowsDateParsingException() {
        // given
        // TestObject 필드 선언 순서: intValue, longValue, doubleValue, stringValue, monthValue(파싱 실패 대상) 순으로
        // 읽히므로 monthValue 앞의 필드는 모두 유효한 값으로 채워 monthValue에서만 예외가 발생하도록 한다.
        final String dataString =
                StringUtils.rightPad("1", 15)              // intValue
                        + StringUtils.rightPad("1", 30)    // longValue
                        + StringUtils.rightPad("1.0", 30)  // doubleValue
                        + StringUtils.rightPad("test", 40) // stringValue
                        + StringUtils.rightPad("ab", 2);   // monthValue: 정수로 파싱 불가능
        final InputStream inputStream = new ByteArrayInputStream(dataString.getBytes(DATA_CHARSET));

        // when & then
        assertThatThrownBy(() -> converter.convert(inputStream, TestObject.class))
                .isInstanceOf(DateParsingException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(NumberFormatException.class);
    }

    @DisplayName("Month 필드 값이 정수이지만 1~12 범위를 벗어나면 DateParsingException 발생 (cause는 원본 DateTimeException)")
    @Test
    void convert_whenMonthFieldValueOutOfRange_thenThrowsDateParsingException() {
        // given
        // TestObject 필드 선언 순서: intValue, longValue, doubleValue, stringValue, monthValue(파싱 실패 대상) 순으로
        // 읽히므로 monthValue 앞의 필드는 모두 유효한 값으로 채워 monthValue에서만 예외가 발생하도록 한다.
        final String dataString =
                StringUtils.rightPad("1", 15)              // intValue
                        + StringUtils.rightPad("1", 30)    // longValue
                        + StringUtils.rightPad("1.0", 30)  // doubleValue
                        + StringUtils.rightPad("test", 40) // stringValue
                        + StringUtils.rightPad("13", 2);   // monthValue: 정수이지만 1~12 범위 초과
        final InputStream inputStream = new ByteArrayInputStream(dataString.getBytes(DATA_CHARSET));

        // when & then
        assertThatThrownBy(() -> converter.convert(inputStream, TestObject.class))
                .isInstanceOf(DateParsingException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(DateTimeException.class);
    }

    @DisplayName("format 패턴으로 파싱 불가능한 java.time date-time 필드 값은 DateParsingException 발생 (cause는 원본 DateTimeParseException)")
    @Test
    void convert_whenDateTimeFieldValueDoesNotMatchFormat_thenThrowsDateParsingException() {
        // given
        // TestObject 필드 선언 순서: intValue, longValue, doubleValue, stringValue, monthValue, dateValue(파싱 실패
        // 대상, format = DATE_FORMAT) 순으로 읽히므로 dateValue 앞의 필드는 모두 유효한 값으로 채워 dateValue에서만
        // 예외가 발생하도록 한다.
        final String dataString =
                StringUtils.rightPad("1", 15)                 // intValue
                        + StringUtils.rightPad("1", 30)       // longValue
                        + StringUtils.rightPad("1.0", 30)     // doubleValue
                        + StringUtils.rightPad("test", 40)    // stringValue
                        + StringUtils.rightPad("1", 2)        // monthValue
                        + StringUtils.rightPad("XXXX-XX-XX", 10); // dateValue: DATE_FORMAT("yyyy-MM-dd")로 파싱 불가능
        final InputStream inputStream = new ByteArrayInputStream(dataString.getBytes(DATA_CHARSET));

        // when & then
        assertThatThrownBy(() -> converter.convert(inputStream, TestObject.class))
                .isInstanceOf(DateParsingException.class)
                .extracting(Throwable::getCause)
                .isInstanceOf(DateTimeParseException.class);
    }

    @DisplayName("valueOf(String) 정적 메서드 자체가 없는 타입(Character) 필드는 파싱과 무관한 리플렉션 오류이므로 회귀 없이 FieldAccessException 발생")
    @Test
    void convert_whenFieldTypeHasNoValueOfStringMethod_thenThrowsFieldAccessException() {
        // given
        // Character는 valueOf(String) 정적 메서드 자체가 없어 파싱 실패가 아닌 리플렉션 오류(NoSuchMethodException)로
        // 귀결되므로, Requirement 1~3의 변경과 무관하게 기존과 동일하게 FieldAccessException이 던져져야 한다.
        final String dataString = StringUtils.rightPad("A", 1); // charValue: @ConvertData(1)
        final InputStream inputStream = new ByteArrayInputStream(dataString.getBytes(DATA_CHARSET));

        // when & then
        assertThatThrownBy(() -> converter.convert(inputStream, CharacterFieldFixture.class))
                .isInstanceOf(FieldAccessException.class);
    }

    @DisplayName("숫자 Wrapper 타입 필드의 trim 후 빈 문자열은 예외 없이 null로 유지됨")
    @Test
    void convert_whenNumericWrapperFieldValueIsBlank_thenFieldRemainsNullWithoutException() {
        // given
        final String dataString = StringUtils.rightPad("", 15); // intWrapperValue: @ConvertData(15), 공백
        final InputStream inputStream = new ByteArrayInputStream(dataString.getBytes(DATA_CHARSET));

        // when
        final IntegerFieldFixture actual = converter.convert(inputStream, IntegerFieldFixture.class);

        // then
        assertThat(actual.getIntWrapperValue()).isNull();
    }

    @DisplayName("Month 타입 필드의 trim 후 빈 문자열은 예외 없이 null로 유지됨")
    @Test
    void convert_whenMonthFieldValueIsBlank_thenFieldRemainsNullWithoutException() {
        // given
        // TestObject 필드 선언 순서: intValue, longValue, doubleValue, stringValue, monthValue(빈 값 대상) 순으로
        // 읽히므로 monthValue 앞의 필드는 모두 유효한 값으로 채운다.
        final String dataString =
                StringUtils.rightPad("1", 15)              // intValue
                        + StringUtils.rightPad("1", 30)    // longValue
                        + StringUtils.rightPad("1.0", 30)  // doubleValue
                        + StringUtils.rightPad("test", 40) // stringValue
                        + StringUtils.rightPad("", 2);     // monthValue: 공백
        final InputStream inputStream = new ByteArrayInputStream(dataString.getBytes(DATA_CHARSET));

        // when
        final TestObject actual = converter.convert(inputStream, TestObject.class);

        // then
        assertThat(actual.getMonthValue()).isNull();
    }

    @DisplayName("Enum 타입 필드의 trim 후 빈 문자열은 예외 없이 null로 유지됨")
    @Test
    void convert_whenEnumFieldValueIsBlank_thenFieldRemainsNullWithoutException() {
        // given
        // TestObject 필드 선언 순서: intValue, longValue, doubleValue, stringValue, monthValue, dateValue,
        // dateTimeValue, enumValue(빈 값 대상) 순으로 읽히므로 enumValue 앞의 필드는 모두 유효한 값으로 채운다.
        final String dataString =
                StringUtils.rightPad("1", 15)                          // intValue
                        + StringUtils.rightPad("1", 30)                // longValue
                        + StringUtils.rightPad("1.0", 30)               // doubleValue
                        + StringUtils.rightPad("test", 40)              // stringValue
                        + StringUtils.rightPad("1", 2)                  // monthValue
                        + StringUtils.rightPad("2024-01-01", 10)        // dateValue
                        + StringUtils.rightPad("2024-01-01 00:00:00", 19) // dateTimeValue
                        + StringUtils.rightPad("", 3);                  // enumValue: 공백
        final InputStream inputStream = new ByteArrayInputStream(dataString.getBytes(DATA_CHARSET));

        // when
        final TestObject actual = converter.convert(inputStream, TestObject.class);

        // then
        assertThat(actual.getEnumValue()).isNull();
    }

    @DisplayName("java.time date-time 타입 필드의 trim 후 빈 문자열은 예외 없이 null로 유지됨")
    @Test
    void convert_whenDateTimeFieldValueIsBlank_thenFieldRemainsNullWithoutException() {
        // given
        // TestObject 필드 선언 순서: intValue, longValue, doubleValue, stringValue, monthValue, dateValue(빈 값
        // 대상) 순으로 읽히므로 dateValue 앞의 필드는 모두 유효한 값으로 채운다.
        final String dataString =
                StringUtils.rightPad("1", 15)              // intValue
                        + StringUtils.rightPad("1", 30)    // longValue
                        + StringUtils.rightPad("1.0", 30)  // doubleValue
                        + StringUtils.rightPad("test", 40) // stringValue
                        + StringUtils.rightPad("1", 2)     // monthValue
                        + StringUtils.rightPad("", 10);    // dateValue: 공백
        final InputStream inputStream = new ByteArrayInputStream(dataString.getBytes(DATA_CHARSET));

        // when
        final TestObject actual = converter.convert(inputStream, TestObject.class);

        // then
        assertThat(actual.getDateValue()).isNull();
    }

    @DisplayName("convertInputStream() 메서드 - InputStream에서 지정 길이만큼 읽기")
    @ParameterizedTest
    @AutoSource
    @Customization(BuilderCustomizer.class)
    void testConvertInputStream_DirectMethod(final TestObject expected) throws Exception {
        // given
        FieldUtils.writeField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT)), true); // truncate milliseconds
        FieldUtils.writeField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size(), true);
        FieldUtils.writeField(expected, "voList", expected.getVoList().subList(0, 2), true);
        FieldUtils.writeField(expected, "ignorable", null, true);

        final byte[] testData = convertTestData(expected);
        final InputStream inputStream = new ByteArrayInputStream(testData);
        final int intValueLength = 15; // @ConvertData(15) intValue의 길이

        // when
        final String actual = converter.convertInputStream(inputStream, intValueLength);

        // then
        // convertInputStream()은 읽은 데이터를 trim하여 반환하므로 원본 intValue와 동일해야 함
        assertThat(actual).isEqualTo(String.valueOf(expected.getIntValue()));
        // 읽은 후 다음 바이트를 읽으면 longValue의 첫 부분이어야 함 (raw 데이터와 직접 비교하여 읽기 위치 검증)
        final String nextBytes = converter.convertInputStream(inputStream, 5);
        final String expectedNextBytes = new String(testData, intValueLength, 5, DATA_CHARSET).trim();
        assertThat(nextBytes).isEqualTo(expectedNextBytes);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    public static class TestObject {
        @ConvertData(15)
        private int intValue;
        @ConvertData(30)
        private long longValue;
        @ConvertData(30)
        private double doubleValue;
        @ConvertData(40)
        private String stringValue;
        @ConvertData(2)
        private Month monthValue;
        @ConvertData(value = 10, format = DATE_FORMAT)
        private LocalDate dateValue;
        @ConvertData(value = 19, format = DATETIME_FORMAT)
        private LocalDateTime dateTimeValue;
        @ConvertData(3)
        private Week enumValue;
        @ConvertData(6)
        private Boolean boolValue;
        @ConvertData(3)
        private byte[] byteValue;

        @Embeddable
        private TestVO voValue;
        @Embeddable
        private TestNestedLoop nestedLoopValue;

        @Iteration(2)
        private List<TestVO> voList;

        @Ignorable
        @ConvertData(10)
        private String ignorable;

    }

    public enum Week {
        MON, TUE, WED, THU, FRI, SAT, SUN;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    public static class TestVO {
        @ConvertData(100)
        private String voStringValue;
        @ConvertData(15)
        private int voIntValue;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @Builder
    @ToString
    @EqualsAndHashCode
    public static class TestNestedLoop {
        @ConvertData(4)
        private int count;
        @Iteration(countField = "count")
        private List<TestVO> list;
    }

    /**
     * {@code Character}는 {@code valueOf(String)} 정적 메서드 자체가 없어 파싱과 무관한 리플렉션 오류
     * ({@code NoSuchMethodException})로 귀결되는지 검증하기 위한 최소 픽스처.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public static class CharacterFieldFixture {
        @ConvertData(1)
        private Character charValue;
    }

    /**
     * 숫자 Wrapper 타입 필드의 trim 후 빈 문자열이 예외 없이 {@code null}로 유지되는지 검증하기 위한 최소 픽스처.
     * {@code TestObject}의 숫자 필드는 모두 primitive(intValue/longValue/doubleValue)라 Requirement 5.1이 다루는
     * "참조 타입(숫자 Wrapper) 필드"를 검증하려면 실제 Wrapper 타입 필드를 가진 별도 픽스처가 필요하다.
     */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public static class IntegerFieldFixture {
        @ConvertData(15)
        private Integer intWrapperValue;
    }

    private byte[] convertTestData(final TestObject expected) throws IOException {
        final String dataString1 =
                StringUtils.rightPad(String.valueOf(expected.getIntValue()), 15)
                        + StringUtils.rightPad(String.valueOf(expected.getLongValue()), 30)
                        + StringUtils.rightPad(String.valueOf(expected.getDoubleValue()), 30)
                        + StringUtils.rightPad(expected.getStringValue(), 40)
                + StringUtils.rightPad(String.valueOf(expected.getMonthValue().getValue()), 2)
                + StringUtils.rightPad(expected.getDateValue().format(DateTimeFormatter.ofPattern(DATE_FORMAT)), 10)
                + StringUtils.rightPad(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)), 19)
                + StringUtils.rightPad(expected.getEnumValue().toString(), 3)
                + StringUtils.rightPad(String.valueOf(expected.getBoolValue()), 6)
        ;
        final TestVO voValue = expected.getVoValue();
        final String dataString2 =
                StringUtils.rightPad(voValue.getVoStringValue(), 100)
                        + StringUtils.rightPad(String.valueOf(voValue.getVoIntValue()), 15);

        final TestNestedLoop loopValue = expected.getNestedLoopValue();
        final String dataString3 = StringUtils.rightPad(String.valueOf(loopValue.getCount()), 4)
                + loopValue.getList().stream()
                        .map(vo -> StringUtils.rightPad(vo.getVoStringValue(), 100)
                                + StringUtils.rightPad(String.valueOf(vo.getVoIntValue()), 15))
                        .collect(Collectors.joining());

        final List<TestVO> voList = expected.getVoList();
        final String dataString4 = voList.stream()
                .map(vo -> StringUtils.rightPad(vo.getVoStringValue(), 100)
                        + StringUtils.rightPad(String.valueOf(vo.getVoIntValue()), 15))
                .collect(Collectors.joining());

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(dataString1.getBytes(DATA_CHARSET));
        baos.write(expected.getByteValue());
        baos.write(dataString2.getBytes(DATA_CHARSET));
        baos.write(dataString3.getBytes(DATA_CHARSET));
        baos.write(dataString4.getBytes(DATA_CHARSET));
        return baos.toByteArray();
    }

    public static void main(final String[] args) {
        System.out.println(Integer.valueOf("-102"));
    }

}
