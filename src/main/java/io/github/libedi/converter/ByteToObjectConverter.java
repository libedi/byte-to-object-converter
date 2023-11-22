package io.github.libedi.converter;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import io.github.libedi.converter.annotation.ConvertData;
import io.github.libedi.converter.annotation.Embeddable;
import io.github.libedi.converter.annotation.Iteration;

/**
 * <p>
 * byte의 데이터를 Object로 쉽게 변환해줍니다.
 * </p>
 * <p>
 * 기존 레거시 코드에서 byte 전문을 Object로 변환시 데이터 파싱에 많은 boilerplate 코드가 필요합니다.
 * {@link ByteToObjectConverter}는 이러한 번거로운 작업을 줄이고, 개발자가 비즈니스 도메인의 설게에 집중할 수 있게
 * 도와줍니다.
 * </p>
 * <p>
 * Object내 다양한 타입을 지원합니다. 지원되는 타입은 아래와 같습니다:
 * </p>
 * <ul>
 * <li><code>byte[]</code></li>
 * <li>{@link String}</li>
 * <li>Enum Type</li>
 * <li><code>int</code>/<code>long</code>/<code>double</code> 등
 * <code>void</code>를 제외한 Primitive Type / Wrapper class</li>
 * <li>java.time 패키지의 date-time class</li>
 * <li>사용자 정의 타입</li>
 * <li>{@link List}
 * <li>사용자 정의 Value Object</li>
 * </ul>
 * <p>
 * 대상 Object는 반드시 기본 생성자를 갖고 있어야 합니다. (private 접근자도 가능)
 * </p>
 * 
 * <p>
 * 기본적으로 {@link ConvertData @ConvertData} 애노테이션으로 변환 필드를 지정합니다. 필드 변환 순서는 클래스에
 * 정의한 필드 순서를 따릅니다. 데이터의 길이는 {@link ConvertData @ConvertData} 애노테이션의
 * {@link ConvertData#value() value} 속성으로 지정합니다. 만약, 데이터의 길이가 다른 필드값에 정의되어 있다면,
 * {@link ConvertData#lengthField() lengthField} 속성으로 해당 필드명을
 * <code>String</code>으로 지정하며, 해당하는 필드의 타입은 반드시 <code>int</code>여야 합니다. 변환 필드의
 * 타입이 date-time 타입이면, {@link ConvertData#format() format} 속성으로 데이터의 format을 지정할
 * 수 있습니다.
 * </p>
 * <p>
 * <code>List</code> 타입을 가진 반복되는 필드의 경우, {@link Iteration @Iteration} 애노테이션으로
 * 지정합니다. 반복 횟수는 고정 반복 횟수를 지정하는 {@link Iteration#value() value} 속성과 다른 필드값에 따라
 * 반복되는 {@link Iteration#countField() countField} 속성으로 지정합니다.
 * {@link Iteration#countField() countField} 는 필드명을 <code>String</code>으로 지정하며,
 * 해당 필드의 타입은 반드시 <code>int</code>여야 합니다.<br/>
 * {@link Iteration @Iteration} 애노테이션이 지정한 필드는 반드시 <code>List</code>의 제네릭 타입을
 * 지정해야 합니다. 해당 제네릭 타입의 클래스 필드는 반드시 {@link ConvertData @ConvertData},
 * {@link Iteration @Iteration} 또는 {@link Embeddable @Embeddable} 애노테이션이 지정되어
 * 있어야 합니다.
 * </p>
 * <p>
 * 대상 Object의 필드로 사용자 정의 Value Object를 사용할 수 있습니다. 해당 필드는
 * {@link Embeddable @Embeddable} 로 지정합니다. VO 내부 필드는 반드시
 * {@link ConvertData @ConvertData}, {@link Iteration @Iteration} 또는
 * {@link Embeddable @Embeddable} 애노테이션이 지정되어 있어야 합니다.
 * </p>
 * 
 * @author "Sangjun, Park"
 * 
 * @see ConvertData
 * @see Iteration
 * @see Embeddable
 */
public class ByteToObjectConverter {

    private final ConversionHelper conversionHelper;

    public ByteToObjectConverter() {
        this(Charset.defaultCharset());
    }

    public ByteToObjectConverter(final String dataCharset) {
        this(Charset.forName(dataCharset));
    }

    public ByteToObjectConverter(final Charset dataCharset) {
        conversionHelper = new ConversionHelper(dataCharset, fieldType -> {
            try {
                return hasAdditionalType(fieldType);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, (fieldType, value) -> {
            try {
                return invokeAdditionalField(fieldType, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * byte[] 데이터를 Object로 변환
     *
     * @param <T>
     * @param inputStream 데이터 입력 스트림
     * @param type        대상 Object 타입
     * @return
     */
    public <T> T convert(final InputStream inputStream, final Class<T> type) {
        return conversionHelper.convert(inputStream, type);
    }

    /**
     * InputStream에서 특정 length만큼 String으로 변환하기
     *
     * @param inputStream
     * @param length
     * @return
     */
    public String convertInputStream(final InputStream inputStream, final int length) {
        return conversionHelper.convertInputStream(inputStream, length);
    }

    /**
     * 사용자 정의 필드 타입 여부
     *
     * @param fieldType
     * @return
     * @throws Exception
     */
    protected boolean hasAdditionalType(final Class<?> fieldType) throws Exception {
        return false;
    }

    /**
     * 사용자 정의 필드 타입 값 설정
     *
     * @param fieldType
     * @param value
     * @return
     * @throws Exception
     */
    protected Object invokeAdditionalField(final Class<?> fieldType, final String value) throws Exception {
        return null;
    }

}
