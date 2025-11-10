package com.zephyrcicd.tdengineorm.util;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Zephyr
 */
public class ClassUtil {

    private ClassUtil() {
    }

    /**
     * 获取class对象以及其父类的所有字段
     *
     * @param clazz 待获取的class对象
     * @return List<Field>
     */
    public static List<Field> getAllFields(Class<?> clazz) {
        Assert.notNull(clazz, "ClassUtil#getAllFields: Class must not be null");
        List<Field> fieldList = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Field[] declaredFields = current.getDeclaredFields();
            for (Field field : declaredFields) {
                field.setAccessible(true);
                fieldList.add(field);
            }
            current = current.getSuperclass();
        }
        return fieldList;
    }

    /**
     * 获取目标类及其父类的所有Field, 并按照指定过滤条件过滤
     *
     * @param clazz  目标类Class
     * @param filter 过滤器
     * @return List<Field>
     */
    public static List<Field> getAllFields(Class<?> clazz, Predicate<Field> filter) {
        return getAllFields(clazz).stream().filter(filter).collect(Collectors.toList());
    }

    /**
     * 获取字段(向上查询)
     *
     * @param clazz clazz
     * @param name  名字
     * @return {@link Field }
     */
    public static Field getFieldByName(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // continue searching super classes
            }
            current = current.getSuperclass();
        }
        return null;
    }


    /**
     * 获取类名的下划线名称
     *
     * @param clazz clazz
     * @return {@link String }
     */
    public static String getClassUnderLineName(Class<?> clazz) {
        Assert.notNull(clazz, "ClassUtil#getClassUnderLineName: Class must not be null");
        return FieldUtil.toUnderlineCase(clazz.getSimpleName());
}

    /**
     * 获取所有字段名
     *
     * @param clazz clazz
     * @return {@link List }<{@link String }>
     */
    public static List<String> getAllFieldNames(Class<?> clazz) {
        List<Field> allFields = getAllFields(clazz);
        if (CollectionUtils.isEmpty(allFields)) {
            return Collections.emptyList();
        }
        return allFields.stream().map(Field::getName).collect(Collectors.toList());
    }

    /**
     * @param name
     * @param classLoader 类装入器
     * @return {@link Class }<{@link ? }>
     */
    public static Class<?> toClassConfident(String name, ClassLoader classLoader) {
        try {
            return loadClass(name, getClassLoaders(classLoader));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load class: " + name, e);
        }
    }

    private static Class<?> loadClass(String className, ClassLoader[] classLoaders) throws ClassNotFoundException {
        for (ClassLoader classLoader : classLoaders) {
            if (classLoader != null) {
                try {
                    return Class.forName(className, true, classLoader);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
        }
        throw new ClassNotFoundException("Cannot find class: " + className);
    }

    private static ClassLoader[] getClassLoaders(ClassLoader classLoader) {
        return new ClassLoader[]{
                classLoader,
                Thread.currentThread().getContextClassLoader(),
                ClassUtil.class.getClassLoader(),
        };
    }
}
