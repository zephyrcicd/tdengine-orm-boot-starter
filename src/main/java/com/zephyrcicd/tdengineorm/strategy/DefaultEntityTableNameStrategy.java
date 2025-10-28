package com.zephyrcicd.tdengineorm.strategy;

import com.zephyrcicd.tdengineorm.util.TdSqlUtil;

/**
 * 默认动态命名策略 - 直接返回实体类上标注的表名称
 * <p>
 * 不做任何转换,适用于不需要动态表名的场景
 * </p>
 *
 * @author Zephyr
 */
public class DefaultEntityTableNameStrategy<T> implements EntityTableNameStrategy<T> {

    @Override
    public String getTableName(T entity) {
        return TdSqlUtil.getTbName(entity.getClass());
    }

}
