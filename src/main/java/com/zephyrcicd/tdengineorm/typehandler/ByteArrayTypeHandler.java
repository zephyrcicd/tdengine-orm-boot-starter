package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * byte[]类型处理器
 */
public class ByteArrayTypeHandler extends BaseTypeHandler<byte[]> {

    public ByteArrayTypeHandler() {
        super(byte[].class);
    }

    @Override
    protected int getSqlType() {
        return Types.VARBINARY;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, byte[] parameter) throws SQLException {
        ps.setBytes(index, parameter);
    }

    @Override
    protected byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getBytes(columnName);
    }

    @Override
    protected byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBytes(columnIndex);
    }

    @Override
    protected byte[] convertFromSqlValue(Object sqlValue) {
        if (sqlValue instanceof byte[]) {
            return (byte[]) sqlValue;
        }
        if (sqlValue instanceof String) {
            return ((String) sqlValue).getBytes();
        }
        throw new IllegalArgumentException("Cannot convert " + sqlValue.getClass().getName() + " to byte[]");
    }
}
