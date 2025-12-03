package com.zephyrcicd.tdengineorm.typehandler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zephyrcicd.tdengineorm.exception.TdOrmException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * List类型处理器
 * <p>
 * 将List序列化为JSON数组存储，反序列化时还原为List。
 * </p>
 *
 * @param <E> 列表元素类型
 * @author zjarlin
 * @since 2.4.0
 */
public class ListTypeHandler<E> extends BaseTypeHandler<List<E>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final TypeReference<List<E>> typeReference;

    @SuppressWarnings("unchecked")
    public ListTypeHandler(Class<E> elementType) {
        super((Class<List<E>>) (Class<?>) List.class);
        this.typeReference = new TypeReference<List<E>>() {
        };
    }

    public ListTypeHandler(TypeReference<List<E>> typeReference) {
        super((Class<List<E>>) (Class<?>) List.class);
        this.typeReference = typeReference;
    }

    @Override
    protected int getSqlType() {
        return Types.NVARCHAR;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, List<E> parameter) throws SQLException {
        try {
            ps.setString(index, OBJECT_MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new TdOrmException("Failed to serialize List to JSON", e);
        }
    }

    @Override
    protected List<E> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    protected List<E> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    @Override
    protected Object convertToSqlValue(List<E> value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new TdOrmException("Failed to serialize List to JSON", e);
        }
    }

    @Override
    protected List<E> convertFromSqlValue(Object sqlValue) {
        if (sqlValue == null) {
            return null;
        }
        return parseJson(String.valueOf(sqlValue));
    }

    private List<E> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            throw new TdOrmException("Failed to deserialize JSON to List", e);
        }
    }
}
