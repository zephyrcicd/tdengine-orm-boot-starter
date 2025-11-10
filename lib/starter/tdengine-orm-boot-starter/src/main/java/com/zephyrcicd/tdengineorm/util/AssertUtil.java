package com.zephyrcicd.tdengineorm.util;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * @author Zephyr
 */
public class AssertUtil {


    public static void notNull(Object object, RuntimeException exception) {
        if (object == null) {
            throw exception;
        }
    }

    public static void isTrue(boolean expression, RuntimeException exception) {
        if (!expression) {
            throw exception;
        }
    }

    public static void notEmpty(Collection<?> collection, RuntimeException exception) {
        if (CollectionUtils.isEmpty(collection)) {
            throw exception;
        }
    }

    public static void notEmpty(Map<?, ?> map, RuntimeException exception) {
        if (CollectionUtils.isEmpty(map)) {
            throw exception;
        }
    }

    public static void notEmpty(Object obj, RuntimeException exception) {
        if (obj == null) {
            throw exception;
        }
        boolean isEmpty = false;
        if (obj instanceof Collection) {
            isEmpty = CollectionUtils.isEmpty((Collection<?>) obj);
        } else if (obj instanceof Map) {
            isEmpty = CollectionUtils.isEmpty((Map<?, ?>) obj);
        } else if (obj instanceof CharSequence) {
            isEmpty = !StringUtils.hasText((CharSequence) obj);
        } else if (obj.getClass().isArray()) {
            isEmpty = Array.getLength(obj) == 0;
        }
        if (isEmpty) {
            throw exception;
        }
    }

    public static void notBlank(String text, RuntimeException exception) {
        if (!StringUtils.hasText(text)) {
            throw exception;
        }
    }

}
