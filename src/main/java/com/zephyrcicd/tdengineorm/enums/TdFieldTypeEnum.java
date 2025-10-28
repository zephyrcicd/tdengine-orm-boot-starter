package com.zephyrcicd.tdengineorm.enums;

import lombok.Getter;

import java.sql.Timestamp;

/**
 * TDengine字段类型枚举
 *
 * @author Zephyr
 * @date 2024/05/15
 */
@Getter
public enum TdFieldTypeEnum {
    /**
     * TDengine唯一的时间类型
     */
    TIMESTAMP("TIMESTAMP", "时间戳", "Timestamp", false),
    INT("INT", "整型", "Integer", false),
    INT_UNSIGNED("INT UNSIGNED", "无符号整数", "Integer", false),
    BIGINT("BIGINT", "长整型", "Long", false),
    BIGINT_UNSIGNED("BIGINT UNSIGNED", "长整型", "Long", false),
    FLOAT("FLOAT", "浮点型", "Float", false),
    DOUBLE("DOUBLE", "双精度浮点型", "Double", false),
    BINARY("BINARY", "单字节字符串", "Char", true),
    SMALLINT("SMALLINT", "短整型", "Integer", false),
    SMALLINT_UNSIGNED("SMALLINT UNSIGNED", "Integer", "Timestamp", false),
    TINYINT("TINYINT", "单字节整型", "Integer", false),
    TINYINT_UNSIGNED("TINYINT UNSIGNED", "Integer", "Timestamp", false),
    BOOL("BOOL", "布尔型", "Boolean", false),
    NCHAR("NCHAR", "多字节字符串", "String", true),
    JSON("JSON", "JSON", "String", false),
    VARCHAR("VARCHAR", "BINARY类型的别名", "Char", true),
    GEOMETRY("GEOMETRY", "几何类型", "未知", true);

    /**
     * TDEngine字段类型
     */
    private final String filedType;

    /**
     * 字段类型描述
     */
    private final String desc;

    /**
     * 对应的Java类型
     */
    private final String javaType;

    /**
     * 是否可以限制长度
     * 0: falase
     * 1: true
     */
    private final boolean needLengthLimit;

    TdFieldTypeEnum(String filedType, String desc, String javaType, boolean needLengthLimit) {
        this.filedType = filedType;
        this.desc = desc;
        this.javaType = javaType;
        this.needLengthLimit = needLengthLimit;
    }

    public static TdFieldTypeEnum matchByFieldType(Class<?> fieldType) {
        if (fieldType.equals(Timestamp.class)) {
            return TdFieldTypeEnum.TIMESTAMP;
        } else if (fieldType.equals(Integer.class)) {
            return TdFieldTypeEnum.INT;
        } else if (fieldType.equals(Double.class)) {
            return TdFieldTypeEnum.DOUBLE;
        } else if (fieldType.equals(Float.class)) {
            return TdFieldTypeEnum.FLOAT;
        } else if (fieldType.equals(Long.class)) {
            return TdFieldTypeEnum.BIGINT;
        } else if (fieldType.equals(Character.class)) {
            return TdFieldTypeEnum.BINARY;
        } else if (fieldType.equals(Boolean.class)) {
            return TdFieldTypeEnum.BOOL;
        } else if (fieldType.equals(String.class)) {
            return TdFieldTypeEnum.NCHAR;
        }
        return null;
    }
}