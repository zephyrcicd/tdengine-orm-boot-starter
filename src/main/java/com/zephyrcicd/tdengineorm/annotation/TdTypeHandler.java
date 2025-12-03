package com.zephyrcicd.tdengineorm.annotation;

import com.zephyrcicd.tdengineorm.typehandler.TypeHandler;

import java.lang.annotation.*;

/**
 * 指定字段使用的TypeHandler
 * <p>
 * 用于标注实体类字段，指定序列化/反序列化时使用的类型处理器。
 * </p>
 *
 * <pre>
 * public class SensorData {
 *     &#64;TdTypeHandler(JsonTypeHandler.class)
 *     private SensorConfig config;
 *
 *     &#64;TdTypeHandler(value = EnumTypeHandler.class)
 *     private DeviceStatus status;
 * }
 * </pre>
 *
 * @author zjarlin
 * @since 2.4.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TdTypeHandler {

    /**
     * TypeHandler实现类
     */
    Class<? extends TypeHandler<?>> value();
}
