package com.zephyrcicd.tdengineorm.util;

import org.springframework.util.StringUtils;

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

    public static String toUnderlineCase(String text) {
        if (!StringUtils.hasLength(text)) {
            return text;
        }
        StringBuilder builder = new StringBuilder(text.length() + 4);
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isUpperCase(c)) {
                boolean hasPrev = i > 0;
                boolean hasNext = i + 1 < chars.length;
                if (hasPrev) {
                    char prev = chars[i - 1];
                    if (prev != '_' && (Character.isLowerCase(prev) || (hasNext && Character.isLowerCase(chars[i + 1])))) {
                        builder.append('_');
                    }
                }
                builder.append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public static String lowerFirst(String text) {
        return StringUtils.uncapitalize(text);
    }
}
