package com.zephyrcicd.tdengineorm.typehandler;

import com.zephyrcicd.tdengineorm.annotation.TdPolymorphic;
import com.zephyrcicd.tdengineorm.annotation.TypeMapping;
import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 多态类型解析器
 * <p>
 * 解析 {@link TdPolymorphic} 注解，提供多态字段的反序列化能力。
 * 支持缓存以提高性能。
 * </p>
 *
 * @author zjarlin
 * @since 2.4.0
 */
@Slf4j
public final class PolymorphicTypeResolver {

    private static final Map<Field, PolymorphicMeta> META_CACHE = new ConcurrentHashMap<>();

    private PolymorphicTypeResolver() {
    }

    /**
     * 解析多态字段的元信息
     */
    public static PolymorphicMeta getMeta(Field field) {
        return META_CACHE.computeIfAbsent(field, PolymorphicTypeResolver::parseMeta);
    }

    private static PolymorphicMeta parseMeta(Field field) {
        TdPolymorphic annotation = field.getAnnotation(TdPolymorphic.class);
        if (annotation == null) {
            return null;
        }

        Map<String, Class<?>> typeMapping = Arrays.stream(annotation.mappings())
                .collect(Collectors.toMap(TypeMapping::type, TypeMapping::target));

        return new PolymorphicMeta(
                annotation.typeField(),
                field.getName(),
                typeMapping,
                annotation.defaultType()
        );
    }

    /**
     * 处理实体的多态字段
     *
     * @param entity  实体对象
     * @param jsonMap 包含type和dataJson的Map
     */
    public static void resolvePolymorphicFields(Object entity, Map<String, Object> jsonMap) {
        if (entity == null) {
            return;
        }

        Class<?> clazz = entity.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            PolymorphicMeta meta = getMeta(field);
            if (meta == null) {
                continue;
            }

            Object typeValue = getFieldValue(entity, meta.getTypeFieldName());
            if (typeValue == null) {
                typeValue = jsonMap.get(meta.getTypeFieldName());
            }

            String type = typeValue != null ? String.valueOf(typeValue) : null;
            Object jsonValue = jsonMap.get(meta.getDataFieldName());

            if (jsonValue == null) {
                continue;
            }

            Class<?> targetType = meta.resolveType(type);
            String json = jsonValue instanceof String ? (String) jsonValue : JsonUtil.toJson(jsonValue);
            Object data = JsonUtil.fromJson(json, targetType);

            setFieldValue(entity, field, data);
        }
    }

    /**
     * 反序列化多态JSON
     */
    public static Object deserialize(Field field, String json, String type) {
        PolymorphicMeta meta = getMeta(field);
        if (meta == null) {
            throw new TdOrmException("Field " + field.getName() + " is not annotated with @TdPolymorphic");
        }

        Class<?> targetType = meta.resolveType(type);
        return JsonUtil.fromJson(json, targetType);
    }

    private static Object getFieldValue(Object entity, String fieldName) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }

    private static void setFieldValue(Object entity, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new TdOrmException("Failed to set field: " + field.getName(), e);
        }
    }

    /**
     * 多态字段元信息
     */
    public static class PolymorphicMeta {
        private final String typeFieldName;
        private final String dataFieldName;
        private final Map<String, Class<?>> typeMapping;
        private final Class<?> defaultType;

        public PolymorphicMeta(String typeFieldName, String dataFieldName,
                               Map<String, Class<?>> typeMapping, Class<?> defaultType) {
            this.typeFieldName = typeFieldName;
            this.dataFieldName = dataFieldName;
            this.typeMapping = typeMapping;
            this.defaultType = defaultType;
        }

        public String getTypeFieldName() {
            return typeFieldName;
        }

        public String getDataFieldName() {
            return dataFieldName;
        }

        public Class<?> resolveType(String type) {
            if (type == null) {
                return defaultType;
            }
            return typeMapping.getOrDefault(type, defaultType);
        }
    }
}
