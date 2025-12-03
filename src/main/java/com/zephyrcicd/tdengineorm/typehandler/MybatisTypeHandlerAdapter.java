package com.zephyrcicd.tdengineorm.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler 桥接适配器
 * <p>
 * 将 MyBatis 的 {@link org.apache.ibatis.type.TypeHandler} 适配为本框架的 {@link TypeHandler}。
 * 复用已有的 MyBatis TypeHandler 实现，避免重复造轮子。
 * </p>
 *
 * <pre>
 * // 单个适配
 * TypeHandler&lt;MyType&gt; handler = MybatisTypeHandlerAdapter.wrap(new MyMybatisTypeHandler());
 *
 * // 批量注册
 * TypeHandlerRegistry.getInstance().fromMybatis(
 *     new JsonTypeHandler(),
 *     new EnumTypeHandler(),
 *     new CustomTypeHandler()
 * );
 * </pre>
 *
 * @param <T> 处理的Java类型
 * @author Zephyr
 */
public class MybatisTypeHandlerAdapter<T> implements TypeHandler<T> {

    private final org.apache.ibatis.type.TypeHandler<T> delegate;
    private final Class<T> javaType;

    @SuppressWarnings("unchecked")
    public MybatisTypeHandlerAdapter(org.apache.ibatis.type.TypeHandler<T> delegate) {
        this.delegate = delegate;
        if (delegate instanceof BaseTypeHandler) {
            this.javaType = (Class<T>) extractRawType((BaseTypeHandler<T>) delegate);
        } else {
            this.javaType = (Class<T>) Object.class;
        }
    }

    public MybatisTypeHandlerAdapter(org.apache.ibatis.type.TypeHandler<T> delegate, Class<T> javaType) {
        this.delegate = delegate;
        this.javaType = javaType;
    }

    /**
     * 包装MyBatis TypeHandler
     */
    public static <T> TypeHandler<T> wrap(org.apache.ibatis.type.TypeHandler<T> mybatisHandler) {
        return new MybatisTypeHandlerAdapter<>(mybatisHandler);
    }

    /**
     * 包装MyBatis TypeHandler并指定Java类型
     */
    public static <T> TypeHandler<T> wrap(org.apache.ibatis.type.TypeHandler<T> mybatisHandler, Class<T> javaType) {
        return new MybatisTypeHandlerAdapter<>(mybatisHandler, javaType);
    }

    public Class<T> getJavaType() {
        return javaType;
    }

    public org.apache.ibatis.type.TypeHandler<T> getDelegate() {
        return delegate;
    }

    @Override
    public void setParameter(PreparedStatement ps, int index, T parameter) throws SQLException {
        delegate.setParameter(ps, index, parameter, null);
    }

    @Override
    public T getResult(ResultSet rs, String columnName) throws SQLException {
        return delegate.getResult(rs, columnName);
    }

    @Override
    public T getResult(ResultSet rs, int columnIndex) throws SQLException {
        return delegate.getResult(rs, columnIndex);
    }

    @Override
    public Object toSqlValue(T value) {
        return value;
    }

    @Override
    public T fromSqlValue(Object sqlValue) {
        if (sqlValue == null) {
            return null;
        }
        if (javaType.isInstance(sqlValue)) {
            return javaType.cast(sqlValue);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private Class<?> extractRawType(BaseTypeHandler handler) {
        try {
            java.lang.reflect.Type superclass = handler.getClass().getGenericSuperclass();
            if (superclass instanceof java.lang.reflect.ParameterizedType) {
                java.lang.reflect.Type[] typeArgs = ((java.lang.reflect.ParameterizedType) superclass).getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    return (Class<?>) typeArgs[0];
                }
            }
        } catch (Exception ignored) {
        }
        return Object.class;
    }
}
