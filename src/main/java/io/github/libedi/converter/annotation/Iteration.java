package io.github.libedi.converter.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.github.libedi.converter.ByteToObjectConverter;

/**
 * <p>
 * {@link java.util.List} 타입의 반복되는 필드를 변환하기 위한 annotation입니다.
 * </p>
 * <p>
 * byte 데이터에서 동일한 구조의 여러 항목을 읽어 List로 변환할 때 사용합니다.
 * 반복 횟수는 고정 값 또는 다른 필드의 값으로 동적으로 지정할 수 있습니다.
 * </p>
 * <p>
 * <strong>필수 조건:</strong>
 * </p>
 * <ul>
 * <li>필드는 {@code List<T>} 형태여야 하며, 제네릭 타입 {@code T}가 명시되어야 합니다</li>
 * <li>List의 요소 타입 {@code T}는 반드시:
 *   <ul>
 *     <li>기본 생성자(매개변수 없는 생성자)를 가져야 합니다</li>
 *     <li>내부 필드는 {@link ConvertData @ConvertData},
 *         {@link Iteration @Iteration}, 또는 {@link Embeddable @Embeddable}
 *         annotation을 가져야 합니다</li>
 *   </ul>
 * </li>
 * <li>{@link #value()}와 {@link #countField()} 중 정확히 하나만 지정해야 합니다
 *   (둘 다 지정하면 value가 우선됨)</li>
 * </ul>
 * <p>
 * <strong>반복 횟수 지정:</strong>
 * </p>
 * <ul>
 * <li>{@code value > 0} - 고정 반복 횟수. 예: {@code @Iteration(5)}는 5개 항목 읽음</li>
 * <li>{@code value == 0} (기본값) - {@link #countField()}로 동적 반복 횟수 지정</li>
 * <li>{@link #countField()} 지정 - 해당 int 필드의 값만큼 반복합니다</li>
 * </ul>
 * <p>
 * <strong>사용 예:</strong>
 * </p>
 *
 * <pre>
 * // 고정 반복 - 정확히 3개의 Item 읽음
 * &#64;Iteration(3)
 * List&lt;Item&gt; fixedItems;
 *
 * // 동적 반복 - itemCount 필드의 값만큼 Item 읽음
 * &#64;ConvertData(4)
 * int itemCount;
 *
 * &#64;Iteration(countField = "itemCount")
 * List&lt;Item&gt; dynamicItems;
 *
 * // Item 클래스 (리스트 요소 타입)
 * public class Item {
 *     public Item() { }  // 필수: 기본 생성자
 *
 *     &#64;ConvertData(10)
 *     String name;
 *
 *     &#64;ConvertData(4)
 *     int price;
 * }
 * </pre>
 * <p>
 * <strong>역변환 시 주의사항:</strong>
 * </p>
 * <p>
 * Object를 byte[]로 역변환할 때:
 * </p>
 * <ul>
 * <li>value가 0이면 countField로 지정된 필드의 현재 값을 반복 횟수로 사용합니다</li>
 * <li>List의 크기가 지정된 반복 횟수보다 작으면, 부족한 부분은 요소 타입의 기본 생성자로
 *     생성된 인스턴스로 채웁니다</li>
 * <li>List의 크기가 지정된 반복 횟수보다 크면, 초과분은 무시됩니다</li>
 * </ul>
 *
 * @author "Sangjun,Park"
 * @see ByteToObjectConverter
 * @see ConvertData
 * @see Embeddable
 */
@Documented
@Retention(RUNTIME)
@Target(FIELD)
public @interface Iteration {

    /**
     * <p>
     * 고정 반복 횟수를 지정합니다.
     * </p>
     * <p>
     * <ul>
     * <li>{@code > 0} - 고정 반복 횟수. 예: 5이면 5개의 항목을 읽음</li>
     * <li>{@code == 0} (기본값) - {@link #countField()}로 동적 반복 횟수 지정</li>
     * </ul>
     * </p>
     * <p>
     * value와 countField가 모두 지정된 경우 value가 우선됩니다.
     * </p>
     *
     * @return 고정 반복 횟수 (기본값: 0)
     * @see #countField()
     */
    int value() default 0;

    /**
     * <p>
     * 반복 횟수가 정의된 다른 필드의 이름을 지정합니다.
     * </p>
     * <p>
     * {@link #value()}가 0일 때 사용되며, 지정된 필드는 반드시 {@code int} 타입이어야 합니다.
     * 이 필드는 Object 내에서 {@link ConvertData @ConvertData}로 마킹되어 있어야 합니다.
     * </p>
     * <p>
     * 예: {@code countField = "itemCount"}는 클래스의 {@code int itemCount} 필드의
     * 값만큼 현재 List 필드를 반복하여 변환합니다.
     * </p>
     * <p>
     * 고정 반복이 필요하면 빈 문자열("")을 유지하고, {@link #value()}에 고정 횟수를 지정하세요.
     * </p>
     *
     * @return 카운트 필드명 (기본값: "")
     * @see #value()
     */
    String countField() default "";
}
