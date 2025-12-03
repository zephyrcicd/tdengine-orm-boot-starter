package com.zephyrcicd.tdengineorm.mapper;

import com.zephyrcicd.tdengineorm.typehandler.TypeHandlerHelper;
import com.zephyrcicd.tdengineorm.util.TdSqlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支持@TdColumn注解的RowMapper
 * 用于处理实体类字段与数据库列名的映射关系
 *
 * @author Zephyr
 */
@Slf4j
public class TdColumnRowMapper<T> implements RowMapper<T> {

    private final Class<T> mappedClass;
    private final Map<String, PropertyDescriptor> mappedFields;
    private final Map<String, String> columnToPropertyMap;
    private final Map<String, Field> propertyToFieldMap;
    
    // 缓存已创建的RowMapper实例，避免重复初始化
    private static final Map<Class<?>, TdColumnRowMapper<?>> MAPPER_CACHE = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> TdColumnRowMapper<T> getInstance(Class<T> mappedClass) {
        return (TdColumnRowMapper<T>) MAPPER_CACHE.computeIfAbsent(mappedClass, TdColumnRowMapper::new);
    }

    private TdColumnRowMapper(Class<?> mappedClass) {
        this.mappedClass = (Class<T>) mappedClass;
        this.mappedFields = new HashMap<>();
        this.columnToPropertyMap = new HashMap<>();
        this.propertyToFieldMap = new HashMap<>();
        initialize();
    }

    /**
     * 初始化映射关系
     */
    private void initialize() {
        PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(mappedClass);
        for (PropertyDescriptor pd : pds) {
            if (pd.getWriteMethod() != null) {
                String propertyName = pd.getName();
                mappedFields.put(propertyName.toLowerCase(), pd);
                // 获取字段上的@TdColumn注解
                try {
                    Field field = mappedClass.getDeclaredField(propertyName);
                    String columnName = TdSqlUtil.getColumnName(field);
                    columnToPropertyMap.put(columnName, propertyName);
                    propertyToFieldMap.put(propertyName, field);
                } catch (NoSuchFieldException e) {
                    // 如果找不到字段，使用默认映射
                    String columnName = camelCaseToUnderscore(propertyName).toLowerCase();
                    columnToPropertyMap.put(columnName, propertyName);
                }
            }
        }
    }

    /**
     * 驼峰转下划线
     */
    private String camelCaseToUnderscore(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));
        for (int i = 1; i < camelCase.length(); i++) {
            char ch = camelCase.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_');
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        T mappedObject = BeanUtils.instantiateClass(mappedClass);
        BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(mappedObject);

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        for (int index = 1; index <= columnCount; index++) {
            String column = JdbcUtils.lookupColumnName(rsmd, index).toLowerCase();
            String propertyName = columnToPropertyMap.get(column);

            if (propertyName != null) {
                PropertyDescriptor pd = mappedFields.get(propertyName.toLowerCase());
                if (pd != null) {
                    try {
                        Object value = JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
                        // 应用TypeHandler进行反序列化
                        Field field = propertyToFieldMap.get(propertyName);
                        if (field != null) {
                            value = TypeHandlerHelper.fromSqlValue(field, value);
                        }
                        bw.setPropertyValue(propertyName, value);
                    } catch (Exception ignored) {

                    }
                }
            } else {
                // 尝试直接匹配属性名（兼容性处理）
                PropertyDescriptor pd = mappedFields.get(column);
                if (pd != null) {
                    try {
                        Object value = JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
                        bw.setPropertyValue(pd.getName(), value);
                    } catch (Exception ex) {
                        if (log.isDebugEnabled()) {
                            log.debug("无法设置属性 '{}' 的值: {}", pd.getName(), ex.getMessage());
                        }
                    }
                }
            }
        }

        return mappedObject;
    }
}