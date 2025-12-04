package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 枚举类型处理器（按ordinal存储）
 * <p>
 * 将枚举转换为其ordinal()整数存储，反序列化时按ordinal还原。
 * 适用于需要节省存储空间的场景，但需注意枚举顺序变更的风险。
 * </p>
 *
 * @param <E> 枚举类型
 * @author zjarlin
 * @since 2.4.0
 */
public class EnumOrdinalTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> enumType;
    private final E[] enumConstants;

    public EnumOrdinalTypeHandler(Class<E> enumType) {
        super(enumType);
        this.enumType = enumType;
        this.enumConstants = enumType.getEnumConstants();
    }

    @Override
    protected int getSqlType() {
        return Types.INTEGER;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, E parameter) throws SQLException {
        ps.setInt(index, parameter.ordinal());
    }

    @Override
    protected E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int ordinal = rs.getInt(columnName);
        return rs.wasNull() ? null : parseOrdinal(ordinal);
    }

    @Override
    protected E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int ordinal = rs.getInt(columnIndex);
        return rs.wasNull() ? null : parseOrdinal(ordinal);
    }

    @Override
    protected Object convertToSqlValue(E value) {
        return value.ordinal();
    }

    @Override
    protected E convertFromSqlValue(Object sqlValue) {
        if (sqlValue == null) {
            return null;
        }
        int ordinal = sqlValue instanceof Number
                ? ((Number) sqlValue).intValue()
                : Integer.parseInt(String.valueOf(sqlValue));
        return parseOrdinal(ordinal);
    }

    private E parseOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= enumConstants.length) {
            throw new IllegalArgumentException("Invalid ordinal " + ordinal + " for enum " + enumType.getName());
        }
        return enumConstants[ordinal];
    }
}
