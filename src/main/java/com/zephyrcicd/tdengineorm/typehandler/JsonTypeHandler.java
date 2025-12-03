package com.zephyrcicd.tdengineorm.typehandler;

import com.zephyrcicd.tdengineorm.util.JsonUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * JSON类型处理器
 * <p>
 * 将Java对象序列化为JSON字符串存储，反序列化时还原为指定类型。
 * 适用于TDengine的NCHAR/JSON字段类型。
 * </p>
 *
 * @param <T> 目标Java类型
 * @author Zephyr
 */
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private final Class<T> targetType;

    public JsonTypeHandler(Class<T> targetType) {
        super(targetType);
        this.targetType = targetType;
    }

    @Override
    protected int getSqlType() {
        return Types.NVARCHAR;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, T parameter) throws SQLException {
        ps.setString(index, JsonUtil.toJson(parameter));
    }

    @Override
    protected T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    protected T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    protected Object convertToSqlValue(T value) {
        return JsonUtil.toJson(value);
    }

    @Override
    protected T convertFromSqlValue(Object sqlValue) {
        if (sqlValue == null) {
            return null;
        }
        return parseJson(String.valueOf(sqlValue));
    }

    private T parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JsonUtil.fromJson(json, targetType);
    }
}
