package io.github.libedi.converter;

import java.nio.charset.Charset;
import java.util.function.BiFunction;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * Object를 byte[] 데이터로 역변환할 때 필드 데이터의 정렬 방식을 지정합니다.
 * </p>
 * <p>
 * {@link ConvertData @ConvertData}에서 지정한 길이보다 실제 필드 데이터가 짧은 경우,
 * 부족한 부분을 패딩 문자(공백)로 채웁니다. 이 enum은 패딩의 위치를 결정합니다.
 * </p>
 * <p>
 * 사용 예:
 * </p>
 * <pre>
 * ByteToObjectConverter converter = new ByteToObjectConverter();
 * MyObject obj = new MyObject();
 * obj.name = "John";    // 4 글자
 * obj.length = 10;      // name 필드의 길이는 10
 *
 * // LEFT 정렬: "John______" (10 byte)
 * byte[] leftAligned = converter.deconvert(obj, DataAlignment.LEFT);
 *
 * // RIGHT 정렬: "______John" (10 byte)
 * byte[] rightAligned = converter.deconvert(obj, DataAlignment.RIGHT);
 * </pre>
 *
 * @author "Sangjun,Park"
 * @see ByteToObjectConverter#deconvert(Object, DataAlignment)
 */
public enum DataAlignment {

    /**
     * <p>
     * 데이터를 오른쪽에 정렬하고 앞에 패딩을 추가합니다.
     * </p>
     * <p>
     * 형식: "____data"
     * </p>
     * <p>
     * 예: length=8, data="AB" → "______AB"
     * </p>
     * <p>
     * 숫자 데이터에 자주 사용됩니다 (우측 정렬).
     * </p>
     */
    RIGHT(StringUtils::leftPad, (bytes, padding) -> ArrayUtils.insert(0, bytes, padding)),

    /**
     * <p>
     * 데이터를 왼쪽에 정렬하고 뒤에 패딩을 추가합니다.
     * </p>
     * <p>
     * 형식: "data____"
     * </p>
     * <p>
     * 예: length=8, data="AB" → "AB______"
     * </p>
     * <p>
     * 문자열 데이터에 자주 사용됩니다 (좌측 정렬).
     * </p>
     */
    LEFT(StringUtils::rightPad, ArrayUtils::addAll);

    private final BiFunction<String, Integer, String> stringPaddingFunction;
    private final BiFunction<byte[], byte[], byte[]>  bytesPaddingFunction;

    DataAlignment(final BiFunction<String, Integer, String> stringPaddingFunction,
            final BiFunction<byte[], byte[], byte[]> bytesPaddingFunction) {
        this.stringPaddingFunction = stringPaddingFunction;
        this.bytesPaddingFunction = bytesPaddingFunction;
    }

    /**
     * <p>
     * 문자열 데이터에 정렬 방식에 맞게 패딩을 적용합니다.
     * </p>
     * <p>
     * size가 -1이면 패딩 없이 원본 문자열을 반환합니다.
     * </p>
     *
     * @param str 원본 문자열
     * @param size 최종 길이 (size == -1이면 패딩 미적용)
     * @return 패딩이 적용된 문자열
     * @see #LEFT
     * @see #RIGHT
     */
    public String applyPad(final String str, final int size) {
        return isNoPadding(size) ? str : stringPaddingFunction.apply(str, size);
    }

    /**
     * <p>
     * byte[] 데이터에 정렬 방식에 맞게 패딩을 적용합니다.
     * </p>
     * <p>
     * size가 -1이면 패딩 없이 원본 byte[]을 반환합니다.
     * 패딩은 지정된 dataCharset으로 인코딩된 공백 문자로 이루어집니다.
     * </p>
     *
     * @param bytes 원본 byte[]
     * @param size 최종 길이 (size == -1이면 패딩 미적용)
     * @param dataCharset 패딩 문자 인코딩에 사용할 Charset
     * @return 패딩이 적용된 byte[]
     * @see #LEFT
     * @see #RIGHT
     * @see ByteToObjectConverter#deconvert(Object, DataAlignment)
     */
    public byte[] applyPad(final byte[] bytes, final int size, final Charset dataCharset) {
        return isNoPadding(size) ? bytes
                : bytesPaddingFunction.apply(bytes,
                        StringUtils.rightPad(StringUtils.EMPTY, size - bytes.length).getBytes(dataCharset));
    }

    /**
     * <p>
     * 주어진 size가 패딩 미적용을 나타내는지 확인합니다.
     * </p>
     * <p>
     * size가 -1이면 {@code true} (패딩 미적용), 그 외는 {@code false}.
     * </p>
     *
     * @param size 검사할 길이값
     * @return {@code true}이면 패딩 미적용, {@code false}이면 패딩 적용
     */
    private boolean isNoPadding(final int size) {
        return size == -1;
    }

}
