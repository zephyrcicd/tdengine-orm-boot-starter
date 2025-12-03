package com.zephyrcicd.tdengineorm.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Arrays;

/**
 * TDengine字段类型枚举
 * 
 * @author Zephyr
 * @since 2024/05/15
 * @see <a href="https://docs.taosdata.com/reference/connector/java/#%E6%95%B0%E6%8D%AE%E7%B1%BB%E5%9E%8B%E6%98%A0%E5%B0%84">TDengine Java连接器数据类型映射</a>
 */
@Getter
public enum TdFieldTypeEnum {
    /**
     * TDengine唯一的时间类型
     */
    TIMESTAMP("TIMESTAMP", "时间戳", "Long", false, false, false, false),
    BOOL("BOOL", "布尔型", "Boolean", false, false, false, false),
    TINYINT("TINYINT", "单字节整型", "Byte", false, false, false, false),
    /**
     * 仅在 WebSocket 连接方式支持
     */
    TINYINT_UNSIGNED("TINYINT UNSIGNED", "无符号单字节整型", "Short", false, true, false, false),
    SMALLINT("SMALLINT", "短整型", "Short", false, false, false, false),
    /**
     * 仅在 WebSocket 连接方式支持
     */
    SMALLINT_UNSIGNED("SMALLINT UNSIGNED", "无符号短整型", "Integer", false, true, false, false),
    INT("INT", "整型", "Integer", false, false, false, false),
    /**
     * 仅在 WebSocket 连接方式支持
     */
    INT_UNSIGNED("INT UNSIGNED", "无符号整数", "Long", false, true, false, false),
    BIGINT("BIGINT", "长整型", "Long", false, false, false, false),
    /**
     * 仅在 WebSocket 连接方式支持
     */
    BIGINT_UNSIGNED("BIGINT UNSIGNED", "无符号长整型", "BigInteger", false, true, false, false),
    FLOAT("FLOAT", "浮点型", "Float", false, false, false, false),
    DOUBLE("DOUBLE", "双精度浮点型", "Double", false, false, false, false),
    VARCHAR("VARCHAR", "变长字符串", "byte[]", true, false, false, false),
    BINARY("BINARY", "单字节字符串 (官方不建议使用，请用 VARBINARY 类型代替)", "byte[]", true, false, false, false),
    NCHAR("NCHAR", "多字节字符串", "String", true, false, false, false),
    /**
     * 仅在 tag 中支持
     */
    JSON("JSON", "JSON", "String", false, false, true, false),
    VARBINARY("VARBINARY", "可变长度二进制", "byte[]", true, false, false, false),
    GEOMETRY("GEOMETRY", "几何类型", "byte[]", true, false, false, false),
    /**
     * 仅在列中支持
     */
    BLOB("BLOB", "二进制大对象", "byte[]", true, false, false, true),
    /**
     * 仅在 WebSocket 连接方式支持
     */
    DECIMAL("DECIMAL", "精确数值", "BigDecimal", false, true, false, false);

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
     */
    private final boolean needLengthLimit;

    /**
     * 是否仅在WebSocket连接方式支持
     */
    private final boolean webSocketOnly;

    /**
     * 是否仅在tag中支持
     */
    private final boolean tagOnly;

    /**
     * 是否仅在列中支持
     */
    private final boolean columnOnly;

    TdFieldTypeEnum(String filedType, String desc, String javaType, boolean needLengthLimit, 
                    boolean webSocketOnly, boolean tagOnly, boolean columnOnly) {
        this.filedType = filedType;
        this.desc = desc;
        this.javaType = javaType;
        this.needLengthLimit = needLengthLimit;
        this.webSocketOnly = webSocketOnly;
        this.tagOnly = tagOnly;
        this.columnOnly = columnOnly;
    }

    /**
     * 根据Java字段类型自动匹配TDengine字段类型（用于普通列）
     * 排除：仅WebSocket支持的类型、仅tag支持的类型
     */
    public static TdFieldTypeEnum matchByFieldType(Class<?> fieldType) {
        return Arrays.stream(TdFieldTypeEnum.values())
                .filter(type -> !type.webSocketOnly && !type.tagOnly)
                .filter(type -> type.matches(fieldType))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据Java字段类型自动匹配TDengine字段类型（用于tag）
     * 排除：仅WebSocket支持的类型、仅列支持的类型
     */
    public static TdFieldTypeEnum matchByFieldTypeForTag(Class<?> fieldType) {
        return Arrays.stream(TdFieldTypeEnum.values())
                .filter(type -> !type.webSocketOnly && !type.columnOnly)
                .filter(type -> type.matches(fieldType))
                .findFirst()
                .orElse(null);
    }

    private boolean matches(Class<?> fieldType) {
        switch (this) {
            case TIMESTAMP:
                return Timestamp.class.isAssignableFrom(fieldType);
            case BOOL:
                return Boolean.class.isAssignableFrom(fieldType) || boolean.class.isAssignableFrom(fieldType);
            case TINYINT:
                return Byte.class.isAssignableFrom(fieldType) || byte.class.isAssignableFrom(fieldType);
            case SMALLINT:
                return Short.class.isAssignableFrom(fieldType) || short.class.isAssignableFrom(fieldType);
            case INT:
                return Integer.class.isAssignableFrom(fieldType) || int.class.isAssignableFrom(fieldType);
            case BIGINT:
                return Long.class.isAssignableFrom(fieldType) || long.class.isAssignableFrom(fieldType);
            case FLOAT:
                return Float.class.isAssignableFrom(fieldType) || float.class.isAssignableFrom(fieldType);
            case DOUBLE:
                return Double.class.isAssignableFrom(fieldType) || double.class.isAssignableFrom(fieldType);
            case VARCHAR:
            case BINARY:
            case VARBINARY:
            case GEOMETRY:
            case BLOB:
                return byte[].class.isAssignableFrom(fieldType);
            case NCHAR:
            case JSON:
                return String.class.isAssignableFrom(fieldType);
            // WebSocket专用类型的匹配（仅通过注解显式指定时使用）
            case TINYINT_UNSIGNED:
                return Short.class.isAssignableFrom(fieldType) || short.class.isAssignableFrom(fieldType);
            case SMALLINT_UNSIGNED:
                return Integer.class.isAssignableFrom(fieldType) || int.class.isAssignableFrom(fieldType);
            case INT_UNSIGNED:
                return Long.class.isAssignableFrom(fieldType) || long.class.isAssignableFrom(fieldType);
            case BIGINT_UNSIGNED:
                return BigInteger.class.isAssignableFrom(fieldType);
            case DECIMAL:
                return BigDecimal.class.isAssignableFrom(fieldType);
            default:
                return false;
        }
    }
}
