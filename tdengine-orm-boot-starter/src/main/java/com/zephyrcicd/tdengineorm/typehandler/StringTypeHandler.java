package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * String类型处理器
 *
 * @author zjarlin
 * @since 2.4.0
 */
public class StringTypeHandler extends BaseTypeHandler<String> {

    public StringTypeHandler() {
        super(String.class);
    }

    @Override
    protected int getSqlType() {
        return Types.NVARCHAR;
    }

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int index, String parameter) throws SQLException {
        ps.setString(index, parameter);
    }

    @Override
    protected String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    protected String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    protected String convertFromSqlValue(Object sqlValue) {
        return String.valueOf(sqlValue);
    }
}
