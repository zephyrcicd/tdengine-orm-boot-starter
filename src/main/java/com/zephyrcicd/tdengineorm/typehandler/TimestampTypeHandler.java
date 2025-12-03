package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Timestamp类型处理器
 */
public class TimestampTypeHandler extends BaseTypeHandler<Timestamp> {

    public TimestampTypeHandler() {
        super(Timestamp.class);
    }

    @Override
    protected int getSqlType() {
        return Types.TIMESTAMP;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, Timestamp parameter) throws SQLException {
        ps.setTimestamp(index, parameter);
    }

    @Override
    protected Timestamp getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getTimestamp(columnName);
    }

    @Override
    protected Timestamp getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getTimestamp(columnIndex);
    }

    @Override
    protected Timestamp convertFromSqlValue(Object sqlValue) {
        if (sqlValue instanceof Timestamp) {
            return (Timestamp) sqlValue;
        }
        if (sqlValue instanceof Long) {
            return new Timestamp((Long) sqlValue);
        }
        if (sqlValue instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) sqlValue).getTime());
        }
        throw new IllegalArgumentException("Cannot convert " + sqlValue.getClass().getName() + " to Timestamp");
    }
}
