package com.zephyrcicd.tdengineorm.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

/**
 * JSON字符串与Map&lt;String, Object&gt;互转类型处理器
 * <p>
 * 使用场景：
 * <ul>
 *     <li>数据库字段存储JSON字符串，需要映射为Java的Map&lt;String, Object&gt;类型</li>
 *     <li>从数据库查询时，将JSON字符串转换为Map&lt;String, Object&gt;</li>
 *     <li>向数据库插入或更新时，将Map&lt;String, Object&gt;转换为JSON字符串</li>
 * </ul>
 *
 * @author zjarlin
 * @since 2.4.0
 */
@Slf4j
@SuppressWarnings("unchecked")
public class JsonMapTypeHandler extends BaseTypeHandler<Map<String, Object>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> TYPE_REF = new TypeReference<Map<String, Object>>() {
    };

    public JsonMapTypeHandler() {
        super((Class<Map<String, Object>>) (Class<?>) Map.class);
    }

    @Override
    protected int getSqlType() {
        return Types.NVARCHAR;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, Map<String, Object> parameter) throws SQLException {
        ps.setString(index, toJson(parameter));
    }

    @Override
    protected Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toMap(rs.getString(columnName));
    }

    @Override
    protected Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toMap(rs.getString(columnIndex));
    }

    @Override
    protected Object convertToSqlValue(Map<String, Object> value) {
        return toJson(value);
    }

    @Override
    protected Map<String, Object> convertFromSqlValue(Object sqlValue) {
        if (sqlValue == null) {
            return null;
        }
        if (sqlValue instanceof Map) {
            return (Map<String, Object>) sqlValue;
        }
        return toMap(String.valueOf(sqlValue));
    }

    private String toJson(Map<String, Object> map) {
        try {
            return MAPPER.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize Map to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private Map<String, Object> toMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, TYPE_REF);
        } catch (Exception e) {
            log.warn("Failed to deserialize JSON to Map: {}", e.getMessage());
            return null;
        }
    }
}
