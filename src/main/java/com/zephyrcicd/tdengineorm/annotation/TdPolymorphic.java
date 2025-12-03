package com.zephyrcicd.tdengineorm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 多态字段注解
 * <p>
 * 标注在需要根据type字段动态反序列化的字段上。
 * </p>
 *
 * <pre>
 * public class Event {
 *     private String type;
 *
 *     &#64;TdPolymorphic(
 *         typeField = "type",
 *         mappings = {
 *             &#64;TypeMapping(type = "SENSOR", target = SensorData.class),
 *             &#64;TypeMapping(type = "ALARM", target = AlarmData.class)
 *         }
 *     )
 *     private Object data;
 * }
 * </pre>
 *
 * @author Zephyr
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TdPolymorphic {

    /**
     * type字段名（实体属性名）
     */
    String typeField();

    /**
     * type值到目标类型的映射
     */
    TypeMapping[] mappings() default {};

    /**
     * 默认类型（当type值无法匹配时使用）
     */
    Class<?> defaultType() default Object.class;
}
