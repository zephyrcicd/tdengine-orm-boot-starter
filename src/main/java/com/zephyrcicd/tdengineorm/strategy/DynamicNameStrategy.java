package com.zephyrcicd.tdengineorm.strategy;

/**
 * 表名称获取策略
 * <p>
 * 支持基于超级表或实体类的灵活表名称策略:
 * <ul>
 *     <li>基于类名的全局命名 - 适用于统一的表名规则(如按日期分表)</li>
 *     <li>基于实体数据的动态命名 - 适用于TDengine子表场景(如根据设备ID生成表名)</li>
 * </ul>
 *
 * @param <T> 实体类型
 * @author Zephyr
 */
@FunctionalInterface
public interface DynamicNameStrategy<T> {

    /**
     * 动态表名生成
     * <p>
     * 根据实体数据动态生成表名,适用于TDengine子表场景。
     * 例如:根据设备ID生成子表名 sensor_device001
     *
     * @param entity         数据实体对象
     * @return 根据策略修改后的表名
     */
    String getTableName(T entity);

}
