package com.zephyrcicd.tdengineorm.typehandler;

import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 类型处理器抽象基类
 * <p>
 * 提供模板方法模式的实现，处理null值和异常封装。
 * 子类只需实现具体的非空值处理逻辑。
 * </p>
 *
 * @param <T> Java类型
 * @author Zephyr
 */
@Slf4j
public abstract class BaseTypeHandler<T> implements TypeHandler<T> {

    private final Class<T> rawType;

    @SuppressWarnings("unchecked")
    protected BaseTypeHandler() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            Type[] typeArguments = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
            this.rawType = (Class<T>) typeArguments[0];
        } else {
            this.rawType = (Class<T>) Object.class;
        }
    }

    protected BaseTypeHandler(Class<T> rawType) {
        this.rawType = rawType;
    }

    public Class<T> getRawType() {
        return rawType;
    }

    @Override
    public void setParameter(PreparedStatement ps, int index, T parameter) throws SQLException {
        if (parameter == null) {
            setNullParameter(ps, index);
        } else {
            try {
                setNonNullParameter(ps, index, parameter);
            } catch (Exception e) {
                throw new TdOrmException("Error setting parameter #" + index + " for type " + rawType.getName() + ". Cause: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public T getResult(ResultSet rs, String columnName) throws SQLException {
        try {
            T result = getNullableResult(rs, columnName);
            return rs.wasNull() ? null : result;
        } catch (Exception e) {
            throw new TdOrmException("Error getting column '" + columnName + "' for type " + rawType.getName() + ". Cause: " + e.getMessage(), e);
        }
    }

    @Override
    public T getResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            T result = getNullableResult(rs, columnIndex);
            return rs.wasNull() ? null : result;
        } catch (Exception e) {
            throw new TdOrmException("Error getting column #" + columnIndex + " for type " + rawType.getName() + ". Cause: " + e.getMessage(), e);
        }
    }

    @Override
    public Object toSqlValue(T value) {
        if (value == null) {
            return null;
        }
        return convertToSqlValue(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T fromSqlValue(Object sqlValue) {
        if (sqlValue == null) {
            return null;
        }
        if (rawType.isInstance(sqlValue)) {
            return (T) sqlValue;
        }
        return convertFromSqlValue(sqlValue);
    }

    /**
     * 设置null参数
     */
    protected void setNullParameter(PreparedStatement ps, int index) throws SQLException {
        ps.setNull(index, getSqlType());
    }

    /**
     * 获取对应的SQL类型（java.sql.Types常量）
     * 子类可重写以指定具体类型
     */
    protected int getSqlType() {
        return java.sql.Types.OTHER;
    }

    /**
     * 设置非空参数（子类必须实现）
     */
    protected abstract void setNonNullParameter(PreparedStatement ps, int index, T parameter) throws SQLException;

    /**
     * 获取可能为空的结果 - 按列名
     */
    protected abstract T getNullableResult(ResultSet rs, String columnName) throws SQLException;

    /**
     * 获取可能为空的结果 - 按索引
     */
    protected abstract T getNullableResult(ResultSet rs, int columnIndex) throws SQLException;

    /**
     * 将Java值转换为SQL值（默认直接返回）
     */
    protected Object convertToSqlValue(T value) {
        return value;
    }

    /**
     * 将SQL值转换为Java对象（子类按需重写）
     */
    protected abstract T convertFromSqlValue(Object sqlValue);
}
