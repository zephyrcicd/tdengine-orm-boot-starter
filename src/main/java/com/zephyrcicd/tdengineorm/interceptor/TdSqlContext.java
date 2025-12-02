package com.zephyrcicd.tdengineorm.interceptor;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * SQL 执行上下文
 * <p>
 * 封装 SQL 执行相关的所有信息，供拦截器使用。
 * </p>
 *
 * @author Zephyr
 * @since 2.2.0
 */
@Getter
@Builder
public class TdSqlContext {

    /**
     * 执行的 SQL 语句
     */
    private final String sql;

    /**
     * SQL 参数
     */
    private final Map<String, Object> params;

    /**
     * SQL 类型
     */
    private final SqlType sqlType;

    /**
     * 结果类型（仅查询操作有效）
     */
    private final Class<?> resultClass;

    /**
     * 自定义属性存储（用于拦截器之间传递数据）
     */
    @Builder.Default
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * SQL 执行开始时间戳（毫秒）
     */
    private final long startTime;

    /**
     * 设置自定义属性
     *
     * @param key   属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 获取自定义属性
     *
     * @param key 属性键
     * @param <T> 属性值类型
     * @return 属性值，不存在则返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 获取自定义属性，不存在时返回默认值
     *
     * @param key          属性键
     * @param defaultValue 默认值
     * @param <T>          属性值类型
     * @return 属性值，不存在则返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        Object value = attributes.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 判断是否为更新操作（INSERT/DELETE/CREATE 等）
     *
     * @return true 如果是更新操作
     */
    public boolean isUpdateOperation() {
        return sqlType == SqlType.UPDATE;
    }

    /**
     * 判断是否为查询操作
     *
     * @return true 如果是查询操作
     */
    public boolean isQueryOperation() {
        return sqlType == SqlType.QUERY || sqlType == SqlType.QUERY_ONE;
    }

    /**
     * SQL 类型枚举
     */
    public enum SqlType {
        /**
         * 更新操作（INSERT/DELETE/CREATE 等）
         */
        UPDATE,

        /**
         * 列表查询
         */
        QUERY,

        /**
         * 单条查询
         */
        QUERY_ONE
    }
}
