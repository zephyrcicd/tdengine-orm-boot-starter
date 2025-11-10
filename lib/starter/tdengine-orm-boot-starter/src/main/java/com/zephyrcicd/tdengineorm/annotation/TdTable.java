package com.zephyrcicd.tdengineorm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TDengine表注解
 * 用于标记实体类对应的表名
 *
 * @author Zephyr
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TdTable {
    /**
     * 表名
     */
    String value() default "";
    
    /**
     * 数据库名
     */
    String database() default "";
    
    /**
     * 表注释
     */
    String comment() default "";

}