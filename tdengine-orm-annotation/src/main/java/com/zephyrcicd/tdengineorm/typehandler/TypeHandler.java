package com.zephyrcicd.tdengineorm.typehandler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 类型处理器接口
 * <p>
 * 用于Java类型与数据库类型之间的序列化和反序列化转换。
 * 类似于MyBatis的TypeHandler，但适配Spring JDBC的使用方式。
 * </p>
 *
 * @param <T> Java类型
 * @author zjarlin
 * @since 2.4.0
 */
public interface TypeHandler<T> {

    /**
     * 设置PreparedStatement参数（序列化）
     *
     * @param ps        PreparedStatement
     * @param index     参数索引（1-based）
     * @param parameter Java对象
     * @throws SQLException SQL异常
     */
    void setParameter(PreparedStatement ps, int index, T parameter) throws SQLException;

    /**
     * 从ResultSet获取结果（反序列化）- 按列名
     *
     * @param rs         ResultSet
     * @param columnName 列名
     * @return Java对象
     * @throws SQLException SQL异常
     */
    T getResult(ResultSet rs, String columnName) throws SQLException;

    /**
     * 从ResultSet获取结果（反序列化）- 按列索引
     *
     * @param rs          ResultSet
     * @param columnIndex 列索引（1-based）
     * @return Java对象
     * @throws SQLException SQL异常
     */
    T getResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * 将Java对象转换为SQL值（用于拼接SQL语句）
     *
     * @param value Java对象
     * @return 转换后的值
     */
    Object toSqlValue(T value);

    /**
     * 将SQL值转换为Java对象
     *
     * @param sqlValue 数据库返回的值
     * @return Java对象
     */
    T fromSqlValue(Object sqlValue);
}
