package io.github.libedi.converter;

import java.nio.charset.Charset;
import java.util.function.BiFunction;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * 변환될 데이터의 정렬 위치를 지정하기 위한 enum.
 * </p>
 *
 * @author "Sangjun,Park"
 *
 */
public enum DataAlignment {

    /** 데이터 오른쪽 정렬 : "____data" */
    RIGHT(StringUtils::leftPad, (bytes, padding) -> ArrayUtils.insert(0, bytes, padding)),
    /** 데이터 왼쪽 정렬 : "data____" */
    LEFT(StringUtils::rightPad, ArrayUtils::addAll);

    private final BiFunction<String, Integer, String> stringPaddingFunction;
    private final BiFunction<byte[], byte[], byte[]>  bytesPaddingFunction;

    DataAlignment(final BiFunction<String, Integer, String> stringPaddingFunction,
            final BiFunction<byte[], byte[], byte[]> bytesPaddingFunction) {
        this.stringPaddingFunction = stringPaddingFunction;
        this.bytesPaddingFunction = bytesPaddingFunction;
    }

    /**
     * 데이터 정렬 방식에 맞게 패딩을 적용한 데이터 반환
     *
     * @param str
     * @param size
     * @return
     */
    public String applyPad(final String str, final int size) {
        return size == -1 ? str : stringPaddingFunction.apply(str, size);
    }

    /**
     * 데이터 정렬 방식에 맞게 패딩을 적용한 데이터 반환
     *
     * @param bytes
     * @param size
     * @param dataCharset
     * @return
     */
    public byte[] applyPad(final byte[] bytes, final int size, final Charset dataCharset) {
        return size == -1 ? bytes
                : bytesPaddingFunction.apply(bytes,
                        StringUtils.rightPad(StringUtils.EMPTY, size - bytes.length).getBytes(dataCharset));
    }

}
