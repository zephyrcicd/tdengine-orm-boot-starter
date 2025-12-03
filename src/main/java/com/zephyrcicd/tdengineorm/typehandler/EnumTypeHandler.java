package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 枚举类型处理器（按name存储）
 * <p>
 * 将枚举转换为其name()字符串存储，反序列化时按name还原。
 * </p>
 *
 * @param <E> 枚举类型
 * @author Zephyr
 */
public class EnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private final Class<E> enumType;

    public EnumTypeHandler(Class<E> enumType) {
        super(enumType);
        this.enumType = enumType;
    }

    @Override
    protected int getSqlType() {
        return Types.NVARCHAR;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, E parameter) throws SQLException {
        ps.setString(index, parameter.name());
    }

    @Override
    protected E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String name = rs.getString(columnName);
        return parseEnum(name);
    }

    @Override
    protected E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String name = rs.getString(columnIndex);
        return parseEnum(name);
    }

    @Override
    protected Object convertToSqlValue(E value) {
        return value.name();
    }

    @Override
    protected E convertFromSqlValue(Object sqlValue) {
        if (sqlValue == null) {
            return null;
        }
        return parseEnum(String.valueOf(sqlValue));
    }

    private E parseEnum(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return Enum.valueOf(enumType, name);
    }
}
