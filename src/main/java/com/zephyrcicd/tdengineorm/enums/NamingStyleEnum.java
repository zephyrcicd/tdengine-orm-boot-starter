package com.zephyrcicd.tdengineorm.enums;

/**
 * 命名风格枚举
 *
 * @author zjarlin
 */
public enum NamingStyleEnum {
    /**
     * 默认风格 - 使用超级表名作为表名
     */
    DEFAULT,

    /**
     * Tag拼接风格 - 使用超级表名 + Tag字段值拼接
     */
    TAG_JOIN;

    public static NamingStyleEnum match(String styleStr) {
        for (NamingStyleEnum style : values()) {
            if (style.name().equalsIgnoreCase(styleStr)) {
                return style;
            }
        }
        return DEFAULT;
    }
}
