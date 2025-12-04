package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Boolean类型处理器
 *
 * @since 2.4.0
 */
public class BooleanTypeHandler extends BaseTypeHandler<Boolean> {

    public BooleanTypeHandler() {
        super(Boolean.class);
    }

    @Override
    protected int getSqlType() {
        return Types.BOOLEAN;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, Boolean parameter) throws SQLException {
        ps.setBoolean(index, parameter);
    }

    @Override
    protected Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        boolean result = rs.getBoolean(columnName);
        return rs.wasNull() ? null : result;
    }

    @Override
    protected Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        boolean result = rs.getBoolean(columnIndex);
        return rs.wasNull() ? null : result;
    }

    @Override
    protected Boolean convertFromSqlValue(Object sqlValue) {
        if (sqlValue instanceof Boolean) {
            return (Boolean) sqlValue;
        }
        if (sqlValue instanceof Number) {
            return ((Number) sqlValue).intValue() != 0;
        }
        String str = String.valueOf(sqlValue).toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }
}
