package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Long类型处理器
 */
public class LongTypeHandler extends BaseTypeHandler<Long> {

    public LongTypeHandler() {
        super(Long.class);
    }

    @Override
    protected int getSqlType() {
        return Types.BIGINT;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, Long parameter) throws SQLException {
        ps.setLong(index, parameter);
    }

    @Override
    protected Long getNullableResult(ResultSet rs, String columnName) throws SQLException {
        long result = rs.getLong(columnName);
        return rs.wasNull() ? null : result;
    }

    @Override
    protected Long getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        long result = rs.getLong(columnIndex);
        return rs.wasNull() ? null : result;
    }

    @Override
    protected Long convertFromSqlValue(Object sqlValue) {
        if (sqlValue instanceof Number) {
            return ((Number) sqlValue).longValue();
        }
        return Long.parseLong(String.valueOf(sqlValue));
    }
}
