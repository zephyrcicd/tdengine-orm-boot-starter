package com.zephyrcicd.tdengineorm.strategy;

/**
 * 默认动态命名策略 - 直接返回原表名称
 * <p>
 * 不做任何转换,适用于不需要动态表名的场景
 *
 * @author Zephyr
 */
public class DefaultDynamicNameStrategy<T> implements DynamicNameStrategy<T> {

    @Override
    public String dynamicTableName(T entity, String defaultTableName) {
        return defaultTableName;
    }

}
