package io.github.libedi.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;

import autoparams.AutoSource;
import autoparams.customization.Customization;
import autoparams.lombok.BuilderCustomizer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.libedi.converter.exception.ConvertFailException;
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
        ReflectionTestUtils.setField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT))); // truncate milliseconds
        ReflectionTestUtils.setField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size());
        ReflectionTestUtils.setField(expected, "voList", expected.getVoList().subList(0, 2));
        ReflectionTestUtils.setField(expected, "ignorable", null);

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
        ReflectionTestUtils.setField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT))); // truncate milliseconds
        ReflectionTestUtils.setField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size());
        ReflectionTestUtils.setField(expected, "voList", expected.getVoList().subList(0, 2));
        ReflectionTestUtils.setField(expected, "ignorable", null);

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
        ReflectionTestUtils.setField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT))); // truncate milliseconds
        ReflectionTestUtils.setField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size());
        ReflectionTestUtils.setField(expected, "voList", expected.getVoList().subList(0, 2));
        ReflectionTestUtils.setField(expected, "ignorable", null);

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
        ReflectionTestUtils.setField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT))); // truncate milliseconds
        ReflectionTestUtils.setField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size());
        ReflectionTestUtils.setField(expected, "voList", expected.getVoList().subList(0, 2));
        ReflectionTestUtils.setField(expected, "ignorable", "testValue"); // Ignorable 필드에 값 설정

        // when
        final byte[] actual = converter.deconvert(expected, DataAlignment.LEFT);

        // then
        assertThat(actual).isNotEmpty();
        // ignorable 필드가 포함되어야 하므로 결과는 null일 때보다 더 길어야 함
        ReflectionTestUtils.setField(expected, "ignorable", null);
        final byte[] withoutIgnorable = converter.deconvert(expected, DataAlignment.LEFT);
        assertThat(actual.length).isGreaterThan(withoutIgnorable.length);
    }

    @DisplayName("@Ignorable 필드가 null일 때 제외")
    @ParameterizedTest
    @AutoSource
    @Customization(BuilderCustomizer.class)
    void testDeconvert_IgnorableNullField(final TestObject expected) throws Exception {
        // given
        ReflectionTestUtils.setField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT))); // truncate milliseconds
        ReflectionTestUtils.setField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size());
        ReflectionTestUtils.setField(expected, "voList", expected.getVoList().subList(0, 2));
        ReflectionTestUtils.setField(expected, "ignorable", null); // Ignorable 필드를 null로 명시

        // when
        final byte[] resultWithNullIgnorable = converter.deconvert(expected, DataAlignment.LEFT);

        // then - ignorable 필드가 null이므로 해당 필드는 제외되고 직렬화됨
        assertThat(resultWithNullIgnorable).isNotEmpty();

        // ignorable 필드가 null일 때와 값이 있을 때의 바이트 길이 비교
        ReflectionTestUtils.setField(expected, "ignorable", "testValue");
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

    @DisplayName("convertInputStream() 메서드 - InputStream에서 지정 길이만큼 읽기")
    @ParameterizedTest
    @AutoSource
    @Customization(BuilderCustomizer.class)
    void testConvertInputStream_DirectMethod(final TestObject expected) throws Exception {
        // given
        ReflectionTestUtils.setField(expected, "dateTimeValue",
                LocalDateTime.parse(expected.getDateTimeValue().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)),
                        DateTimeFormatter.ofPattern(DATETIME_FORMAT))); // truncate milliseconds
        ReflectionTestUtils.setField(expected.getNestedLoopValue(), "count", expected.getNestedLoopValue().list.size());
        ReflectionTestUtils.setField(expected, "voList", expected.getVoList().subList(0, 2));
        ReflectionTestUtils.setField(expected, "ignorable", null);

        final byte[] testData = convertTestData(expected);
        final InputStream inputStream = new ByteArrayInputStream(testData);
        final int intValueLength = 15; // @ConvertData(15) intValue의 길이

        // when
        final String actual = converter.convertInputStream(inputStream, intValueLength);

        // then
        // 읽은 데이터의 길이가 정확히 요청한 길이여야 함
        assertThat(actual).isNotNull();
        assertThat(actual.length()).isEqualTo(intValueLength);
        // 읽은 데이터를 trim하면 원본 intValue와 동일해야 함
        assertThat(actual.trim()).isEqualTo(String.valueOf(expected.getIntValue()));
        // 읽은 후 다음 바이트를 읽으면 longValue의 첫 부분이어야 함
        final String nextBytes = converter.convertInputStream(inputStream, 5);
        assertThat(nextBytes).isNotNull();
        assertThat(nextBytes.length()).isEqualTo(5);
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
