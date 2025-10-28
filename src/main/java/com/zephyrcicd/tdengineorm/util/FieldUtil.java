package com.zephyrcicd.tdengineorm.util;

/**
 * @author Zephyr
 */
public class FieldUtil {


    public static String getFieldNameByMethod(String methodName) {
        if (methodName.startsWith("get")) {
            methodName = methodName.substring(3);
        } else if (methodName.startsWith("is")) {
            methodName = methodName.substring(2);
        }
        return methodName;
    }
}
