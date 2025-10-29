package com.zephyrcicd.tdengineorm.annotation;

import com.zephyrcicd.tdengineorm.enums.TdFieldTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TDengine列注解
 * 用于标记实体类字段对应的列信息
 *
 * @author Zephyr
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TdColumn {
    /**
     * 列名
     */
    String value() default "";

    /**
     * 字段类型
     */
    TdFieldTypeEnum type() default TdFieldTypeEnum.NCHAR;

    /**
     * 字段长度
     */
    int length() default 0;

    /**
     * 列注释
     */
    String comment() default "";

    /**
     * 是否允许为空
     */
    boolean nullable() default true;

    /**
     * 是否为复合组件
     * 注意：仅支持TDengine3.3.x.x以上的版本
     *
     * @return boolean
     */
    boolean compositeKey() default false;
} 