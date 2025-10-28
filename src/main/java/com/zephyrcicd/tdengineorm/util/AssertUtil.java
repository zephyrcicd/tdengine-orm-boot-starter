package com.zephyrcicd.tdengineorm.util;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Map;

/**
 * @author Zephyr
 */
public class AssertUtil {


    public static void notNull(Object object, RuntimeException exception) {
        if (ObjUtil.isNull(object)) {
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
        if (ArrayUtil.isEmpty(obj)) {
            throw exception;
        }
    }

    public static void notBlank(String text, RuntimeException exception) {
        if (StrUtil.isBlank(text)) {
            throw exception;
        }
    }

}
