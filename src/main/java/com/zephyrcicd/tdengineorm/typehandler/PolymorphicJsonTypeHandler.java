package com.zephyrcicd.tdengineorm.typehandler;

import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.util.JsonUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 多态JSON类型处理器
 * <p>
 * 根据type字段值动态决定dataJson字段的反序列化目标类型。
 * 支持两种使用方式：
 * 1. 通过Builder注册type->Class映射
 * 2. 通过自定义Function动态解析
 * </p>
 *
 * <pre>
 * // 方式1: 静态映射
 * PolymorphicJsonTypeHandler handler = PolymorphicJsonTypeHandler.builder()
 *     .register("SENSOR", SensorData.class)
 *     .register("ALARM", AlarmData.class)
 *     .defaultType(BaseData.class)
 *     .build();
 *
 * // 方式2: 动态解析
 * PolymorphicJsonTypeHandler handler = PolymorphicJsonTypeHandler.builder()
 *     .typeResolver(type -> {
 *         switch(type) {
 *             case "SENSOR": return SensorData.class;
 *             case "ALARM": return AlarmData.class;
 *             default: return BaseData.class;
 *         }
 *     })
 *     .build();
 * </pre>
 *
 * @author Zephyr
 */
public class PolymorphicJsonTypeHandler extends BaseTypeHandler<Object> {

    private final Map<String, Class<?>> typeMapping;
    private final Function<String, Class<?>> typeResolver;
    private final Class<?> defaultType;

    private PolymorphicJsonTypeHandler(Builder builder) {
        super(Object.class);
        this.typeMapping = builder.typeMapping;
        this.typeResolver = builder.typeResolver;
        this.defaultType = builder.defaultType;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 根据type值解析目标类型
     */
    public Class<?> resolveType(String type) {
        if (type == null) {
            return defaultType;
        }
        if (typeResolver != null) {
            Class<?> resolved = typeResolver.apply(type);
            return resolved != null ? resolved : defaultType;
        }
        return typeMapping.getOrDefault(type, defaultType);
    }

    /**
     * 反序列化JSON - 需要外部传入type值
     */
    public Object deserialize(String json, String type) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        Class<?> targetType = resolveType(type);
        if (targetType == null) {
            throw new TdOrmException("Cannot resolve type for: " + type);
        }
        return JsonUtil.fromJson(json, targetType);
    }

    @Override
    protected int getSqlType() {
        return Types.NVARCHAR;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, Object parameter) throws SQLException {
        ps.setString(index, JsonUtil.toJson(parameter));
    }

    @Override
    protected Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    protected Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    protected Object convertToSqlValue(Object value) {
        return JsonUtil.toJson(value);
    }

    @Override
    protected Object convertFromSqlValue(Object sqlValue) {
        return sqlValue;
    }

    public static class Builder {
        private final Map<String, Class<?>> typeMapping = new HashMap<>();
        private Function<String, Class<?>> typeResolver;
        private Class<?> defaultType = Object.class;

        public Builder register(String type, Class<?> clazz) {
            typeMapping.put(type, clazz);
            return this;
        }

        public Builder registerAll(Map<String, Class<?>> mapping) {
            typeMapping.putAll(mapping);
            return this;
        }

        public Builder typeResolver(Function<String, Class<?>> resolver) {
            this.typeResolver = resolver;
            return this;
        }

        public Builder defaultType(Class<?> defaultType) {
            this.defaultType = defaultType;
            return this;
        }

        public PolymorphicJsonTypeHandler build() {
            return new PolymorphicJsonTypeHandler(this);
        }
    }
}
