package com.zephyrcicd.tdengineorm.typehandler;

import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 多态字段处理器
 * <p>
 * 用于处理实体中type+dataJson这种多态反序列化场景。
 * 在RowMapper中根据type列的值，动态决定dataJson列的反序列化类型。
 * </p>
 *
 * <pre>
 * // 定义实体
 * public class Event {
 *     private String type;
 *     private Object data;  // 根据type动态反序列化
 * }
 *
 * // 使用方式
 * PolymorphicFieldHandler handler = PolymorphicFieldHandler.builder()
 *     .typeColumn("type")
 *     .dataColumn("data_json")
 *     .dataField("data")
 *     .register("SENSOR", SensorData.class)
 *     .register("ALARM", AlarmData.class)
 *     .build();
 *
 * // 在RowMapper中使用
 * Event event = new Event();
 * handler.handleRow(rs, event);
 * </pre>
 *
 * @author Zephyr
 */
@Slf4j
public class PolymorphicFieldHandler {

    private final String typeColumn;
    private final String dataColumn;
    private final String dataFieldName;
    private final Map<String, Class<?>> typeMapping;
    private final Function<String, Class<?>> typeResolver;
    private final Class<?> defaultType;

    private PolymorphicFieldHandler(Builder builder) {
        this.typeColumn = builder.typeColumn;
        this.dataColumn = builder.dataColumn;
        this.dataFieldName = builder.dataFieldName;
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
     * 处理ResultSet行，自动填充多态字段
     */
    public <T> void handleRow(ResultSet rs, T entity) throws SQLException {
        String type = rs.getString(typeColumn);
        String json = rs.getString(dataColumn);

        if (json == null || json.isEmpty()) {
            return;
        }

        Class<?> targetType = resolveType(type);
        if (targetType == null) {
            log.warn("Cannot resolve type for: {}, skip field: {}", type, dataFieldName);
            return;
        }

        Object data = JsonUtil.fromJson(json, targetType);
        setFieldValue(entity, dataFieldName, data);
    }

    /**
     * 从Map中处理多态字段
     */
    public Object deserializeFromMap(Map<String, Object> row) {
        Object typeValue = row.get(typeColumn);
        Object jsonValue = row.get(dataColumn);

        if (jsonValue == null) {
            return null;
        }

        String type = typeValue != null ? String.valueOf(typeValue) : null;
        String json = String.valueOf(jsonValue);

        Class<?> targetType = resolveType(type);
        if (targetType == null) {
            return json;
        }

        return JsonUtil.fromJson(json, targetType);
    }

    private void setFieldValue(Object entity, String fieldName, Object value) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new TdOrmException("Failed to set field: " + fieldName, e);
        }
    }

    /**
     * 创建包含多态处理的RowMapper
     */
    public <T> RowMapper<T> createRowMapper(Class<T> entityClass, RowMapper<T> baseMapper) {
        return (rs, rowNum) -> {
            T entity = baseMapper.mapRow(rs, rowNum);
            if (entity != null) {
                handleRow(rs, entity);
            }
            return entity;
        };
    }

    public static class Builder {
        private String typeColumn = "type";
        private String dataColumn = "data_json";
        private String dataFieldName = "data";
        private final Map<String, Class<?>> typeMapping = new HashMap<>();
        private Function<String, Class<?>> typeResolver;
        private Class<?> defaultType;

        public Builder typeColumn(String typeColumn) {
            this.typeColumn = typeColumn;
            return this;
        }

        public Builder dataColumn(String dataColumn) {
            this.dataColumn = dataColumn;
            return this;
        }

        public Builder dataField(String dataFieldName) {
            this.dataFieldName = dataFieldName;
            return this;
        }

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

        public PolymorphicFieldHandler build() {
            return new PolymorphicFieldHandler(this);
        }
    }
}
