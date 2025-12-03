package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Integer类型处理器
 *
 * @author zjarlin
 * @since 2.4.0
 */
public class IntegerTypeHandler extends BaseTypeHandler<Integer> {

    public IntegerTypeHandler() {
        super(Integer.class);
    }

    @Override
    protected int getSqlType() {
        return Types.INTEGER;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, Integer parameter) throws SQLException {
        ps.setInt(index, parameter);
    }

    @Override
    protected Integer getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int result = rs.getInt(columnName);
        return rs.wasNull() ? null : result;
    }

    @Override
    protected Integer getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int result = rs.getInt(columnIndex);
        return rs.wasNull() ? null : result;
    }

    @Override
    protected Integer convertFromSqlValue(Object sqlValue) {
        if (sqlValue instanceof Number) {
            return ((Number) sqlValue).intValue();
        }
        return Integer.parseInt(String.valueOf(sqlValue));
    }
}
