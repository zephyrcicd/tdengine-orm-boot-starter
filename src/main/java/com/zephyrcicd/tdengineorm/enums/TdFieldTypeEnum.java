package com.zephyrcicd.tdengineorm.enums;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.function.Predicate;

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
    BOOL("BOOL", "布尔型", "Boolean", false),
    TINYINT("TINYINT", "单字节整型", "Byte", false),
    TINYINT_UNSIGNED("TINYINT UNSIGNED", "无符号单字节整型", "Short", false),
    SMALLINT("SMALLINT", "短整型", "Short", false),
    SMALLINT_UNSIGNED("SMALLINT UNSIGNED", "无符号短整型", "Integer", false),
    INT("INT", "整型", "Integer", false),
    INT_UNSIGNED("INT UNSIGNED", "无符号整数", "Long", false),
    BIGINT("BIGINT", "长整型", "Long", false),
    BIGINT_UNSIGNED("BIGINT UNSIGNED", "无符号长整型", "BigInteger", false),
    FLOAT("FLOAT", "浮点型", "Float", false),
    DOUBLE("DOUBLE", "双精度浮点型", "Double", false),
    VARCHAR("VARCHAR", "变长字符串", "byte[]", true),
    BINARY("BINARY", "单字节字符串", "byte[]", true),
    NCHAR("NCHAR", "多字节字符串", "String", true),
    JSON("JSON", "JSON", "String", false),
    VARBINARY("VARBINARY", "可变长度二进制", "byte[]", true),
    GEOMETRY("GEOMETRY", "几何类型", "byte[]", true),
    BLOB("BLOB", "二进制大对象", "byte[]", true),
    DECIMAL("DECIMAL", "精确数值", "BigDecimal", false);

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
     * 0: false
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
        return Arrays.stream(TdFieldTypeEnum.values())
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
            case TINYINT_UNSIGNED:
            case SMALLINT:
                return Short.class.isAssignableFrom(fieldType) || short.class.isAssignableFrom(fieldType);
            case SMALLINT_UNSIGNED:
            case INT:
                return Integer.class.isAssignableFrom(fieldType) || int.class.isAssignableFrom(fieldType);
            case INT_UNSIGNED:
            case BIGINT:
                return Long.class.isAssignableFrom(fieldType) || long.class.isAssignableFrom(fieldType);
            case BIGINT_UNSIGNED:
                return BigInteger.class.isAssignableFrom(fieldType);
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
            case DECIMAL:
                return BigDecimal.class.isAssignableFrom(fieldType);
            default:
                return false;
        }
    }
}
