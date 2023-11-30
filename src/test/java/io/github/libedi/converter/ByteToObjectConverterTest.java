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

import autoparams.AutoSource;
import autoparams.customization.Customization;
import autoparams.lombok.BuilderCustomizer;
import io.github.libedi.converter.annotation.ConvertData;
import io.github.libedi.converter.annotation.Embeddable;
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

        final byte[] expectedBytes = convertTestData(expected);

        // when
        final byte[] actual = converter.deconvert(expected, DataAlignment.LEFT);

        // then
        assertThat(actual).isEqualTo(expectedBytes);
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
