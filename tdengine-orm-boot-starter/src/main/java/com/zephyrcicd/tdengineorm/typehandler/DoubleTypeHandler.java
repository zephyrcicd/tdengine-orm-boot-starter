package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Double类型处理器
 *
 * @since 2.4.0
 */
public class DoubleTypeHandler extends BaseTypeHandler<Double> {

    public DoubleTypeHandler() {
        super(Double.class);
    }

    @Override
    protected int getSqlType() {
        return Types.DOUBLE;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, Double parameter) throws SQLException {
        ps.setDouble(index, parameter);
    }

    @Override
    protected Double getNullableResult(ResultSet rs, String columnName) throws SQLException {
        double result = rs.getDouble(columnName);
        return rs.wasNull() ? null : result;
    }

    @Override
    protected Double getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        double result = rs.getDouble(columnIndex);
        return rs.wasNull() ? null : result;
    }

    @Override
    protected Double convertFromSqlValue(Object sqlValue) {
        if (sqlValue instanceof Number) {
            return ((Number) sqlValue).doubleValue();
        }
        return Double.parseDouble(String.valueOf(sqlValue));
    }
}
