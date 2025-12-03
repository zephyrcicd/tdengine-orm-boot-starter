package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Float类型处理器
 */
public class FloatTypeHandler extends BaseTypeHandler<Float> {

    public FloatTypeHandler() {
        super(Float.class);
    }

    @Override
    protected int getSqlType() {
        return Types.FLOAT;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, Float parameter) throws SQLException {
        ps.setFloat(index, parameter);
    }

    @Override
    protected Float getNullableResult(ResultSet rs, String columnName) throws SQLException {
        float result = rs.getFloat(columnName);
        return rs.wasNull() ? null : result;
    }

    @Override
    protected Float getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        float result = rs.getFloat(columnIndex);
        return rs.wasNull() ? null : result;
    }

    @Override
    protected Float convertFromSqlValue(Object sqlValue) {
        if (sqlValue instanceof Number) {
            return ((Number) sqlValue).floatValue();
        }
        return Float.parseFloat(String.valueOf(sqlValue));
    }
}
