package com.zephyrcicd.tdengineorm.typehandler;

import com.zephyrcicd.tdengineorm.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Object类型处理器
 * <p>
 * 对于Object类型字段，根据实际类型智能处理：
 * <ul>
 *     <li>基础类型（String、Number、Boolean）- 直接使用</li>
 *     <li>复杂对象（POJO、Map、Collection）- 序列化为JSON字符串</li>
 * </ul>
 *
 * @author zjarlin
 * @since 2.4.0
 */
@Slf4j
public class ObjectTypeHandler extends BaseTypeHandler<Object> {

    public ObjectTypeHandler() {
        super(Object.class);
    }

    @Override
    protected int getSqlType() {
        return Types.NVARCHAR;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, Object parameter) throws SQLException {
        String value = convertToString(parameter);
        ps.setString(index, value);
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
        return convertToString(value);
    }

    @Override
    protected Object convertFromSqlValue(Object sqlValue) {
        return sqlValue;
    }

    /**
     * 将对象转换为字符串
     * 基础类型直接toString，复杂对象序列化为JSON
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (isSimpleType(value)) {
            return value.toString();
        }
        return JsonUtil.toJson(value);
    }

    /**
     * 判断是否为简单类型（不需要JSON序列化）
     */
    private boolean isSimpleType(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value.getClass().isPrimitive()
                || value.getClass().isEnum();
    }
}
