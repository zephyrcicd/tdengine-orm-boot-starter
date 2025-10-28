package com.zephyrcicd.tdengineorm.strategy;

import java.util.Map;

/**
 * 以Map作为数据载体的表名称策略
 *
 * @author Zephyr
 */
@FunctionalInterface
public interface MapTableNameStrategy {

    String getTableName(Map<String, Object> dataMap);

}
