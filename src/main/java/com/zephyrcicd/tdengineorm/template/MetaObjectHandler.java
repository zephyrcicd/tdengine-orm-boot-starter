package com.zephyrcicd.tdengineorm.template;

/**
 * 元对象字段处理器接口
 * <p>用于在插入操作前自动填充字段值，类似于MyBatis-Plus的MetaObjectHandler</p>
 *
 * @author Zephyr
 */
public interface MetaObjectHandler {

    /**
     * 插入元对象字段填充（用于插入时对公共字段的填充）
     *
     * @param object 实体对象或Map
     * @param <T>    实体类型或Map类型
     */
    <T> void insertFill(T object);
}