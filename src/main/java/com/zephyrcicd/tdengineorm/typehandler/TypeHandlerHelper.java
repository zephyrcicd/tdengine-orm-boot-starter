package com.zephyrcicd.tdengineorm.typehandler;

import com.zephyrcicd.tdengineorm.annotation.TdTypeHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TypeHandler辅助工具类
 * <p>
 * 提供字段级别的TypeHandler获取和类型转换功能，
 * 用于在ORM读写时自动应用TypeHandler进行序列化和反序列化。
 * </p>
 *
 * @author Zephyr
 */
@Slf4j
public final class TypeHandlerHelper {

    private static final Map<Field, TypeHandler<?>> FIELD_HANDLER_CACHE = new ConcurrentHashMap<>();
    private static final TypeHandlerRegistry REGISTRY = TypeHandlerRegistry.getInstance();

    private TypeHandlerHelper() {
    }

    /**
     * 获取字段对应的TypeHandler
     * <p>
     * 优先级：
     * 1. @TdTypeHandler注解指定的处理器
     * 2. TypeHandlerRegistry中按类型注册的处理器
     * 3. null（使用默认处理）
     * </p>
     *
     * @param field 字段
     * @return TypeHandler或null
     */
    @SuppressWarnings("unchecked")
    public static <T> TypeHandler<T> getHandler(Field field) {
        return (TypeHandler<T>) FIELD_HANDLER_CACHE.computeIfAbsent(field, TypeHandlerHelper::resolveHandler);
    }

    private static TypeHandler<?> resolveHandler(Field field) {
        // 1. 检查@TdTypeHandler注解
        TdTypeHandler annotation = field.getAnnotation(TdTypeHandler.class);
        if (annotation != null) {
            try {
                return annotation.value().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.warn("Failed to instantiate TypeHandler: {}, using default", annotation.value().getName(), e);
            }
        }

        // 2. 从注册表获取
        Class<?> fieldType = field.getType();
        TypeHandler<?> handler = REGISTRY.getHandler(fieldType);
        if (handler != null) {
            return handler;
        }

        // 3. 对于复杂对象类型，使用ObjectTypeHandler
        if (isComplexType(fieldType)) {
            return REGISTRY.getHandler(Object.class);
        }

        return null;
    }

    /**
     * 写入时：将Java对象转换为SQL值
     *
     * @param field 字段
     * @param value Java对象值
     * @return 转换后的SQL值
     */
    @SuppressWarnings("unchecked")
    public static Object toSqlValue(Field field, Object value) {
        if (value == null) {
            return null;
        }

        TypeHandler<Object> handler = getHandler(field);
        if (handler != null) {
            return handler.toSqlValue(value);
        }

        // 没有handler时，对复杂类型使用ObjectTypeHandler
        if (isComplexType(value.getClass())) {
            TypeHandler<Object> objectHandler = REGISTRY.getHandler(Object.class);
            if (objectHandler != null) {
                return objectHandler.toSqlValue(value);
            }
        }

        return value;
    }

    /**
     * 读取时：将SQL值转换为Java对象
     *
     * @param field    字段
     * @param sqlValue 数据库返回的值
     * @return 转换后的Java对象
     */
    @SuppressWarnings("unchecked")
    public static Object fromSqlValue(Field field, Object sqlValue) {
        if (sqlValue == null) {
            return null;
        }

        TypeHandler<Object> handler = getHandler(field);
        if (handler != null) {
            return handler.fromSqlValue(sqlValue);
        }

        return sqlValue;
    }

    /**
     * 判断是否为需要特殊处理的复杂类型
     */
    private static boolean isComplexType(Class<?> type) {
        if (type == null) {
            return false;
        }
        // 基础类型不需要处理
        if (type.isPrimitive() || type.isEnum()) {
            return false;
        }
        if (String.class.isAssignableFrom(type) ||
            Number.class.isAssignableFrom(type) ||
            Boolean.class.isAssignableFrom(type) ||
            Character.class.isAssignableFrom(type) ||
            java.util.Date.class.isAssignableFrom(type) ||
            java.time.temporal.Temporal.class.isAssignableFrom(type) ||
            byte[].class.isAssignableFrom(type)) {
            return false;
        }
        // 其他类型视为复杂类型
        return true;
    }

    /**
     * 清除字段处理器缓存
     */
    public static void clearCache() {
        FIELD_HANDLER_CACHE.clear();
    }
}
