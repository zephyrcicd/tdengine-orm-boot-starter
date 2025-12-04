package com.zephyrcicd.tdengineorm.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类型映射定义
 * <p>
 * 用于 {@link TdPolymorphic} 中定义type值到目标类型的映射关系。
 * </p>
 *
 * @author zjarlin
 * @since 2.4.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeMapping {

    /**
     * type字段的值
     */
    String type();

    /**
     * 对应的反序列化目标类型
     */
    Class<?> target();
}
