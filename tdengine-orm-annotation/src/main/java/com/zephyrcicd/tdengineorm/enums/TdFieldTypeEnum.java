package com.zephyrcicd.tdengineorm.enums;

import lombok.Getter;

import java.math.BigInteger;
import java.sql.Timestamp;

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
     * 根据Java字段类型自动匹配TDengine字段类型
     *
     * <p>匹配优先级策略：
     * <ul>
     *   <li>1. 排除仅WebSocket支持的类型（UNSIGNED类型、DECIMAL）</li>
     *   <li>2. 对于byte[]优先匹配VARBINARY（官方推荐替代BINARY）</li>
     *   <li>3. 对于String优先匹配NCHAR（通用字符串类型）</li>
     *   <li>4. tag和普通列使用相同的匹配逻辑，特殊类型（JSON/BLOB）需显式通过注解指定</li>
     * </ul>
     *
     * @param fieldType Java字段类型
     * @return 匹配的TDengine字段类型，未匹配则返回null
     */
    public static TdFieldTypeEnum matchByFieldType(Class<?> fieldType) {
        if (fieldType == null) {
            return null;
        }

        // 精确匹配 - 避免流式操作，提升性能
        if (Timestamp.class.isAssignableFrom(fieldType)) {
            return TIMESTAMP;
        }
        if (Boolean.class.isAssignableFrom(fieldType) || boolean.class == fieldType) {
            return BOOL;
        }
        if (Byte.class.isAssignableFrom(fieldType) || byte.class == fieldType) {
            return TINYINT;
        }
        if (Short.class.isAssignableFrom(fieldType) || short.class == fieldType) {
            return SMALLINT;  // 优先基础类型，排除TINYINT_UNSIGNED（仅WebSocket）
        }
        if (Integer.class.isAssignableFrom(fieldType) || int.class == fieldType) {
            return INT;  // 优先基础类型，排除SMALLINT_UNSIGNED（仅WebSocket）
        }
        if (Long.class.isAssignableFrom(fieldType) || long.class == fieldType) {
            return BIGINT;  // 优先基础类型，排除INT_UNSIGNED（仅WebSocket）
        }
        if (BigInteger.class.isAssignableFrom(fieldType)) {
            return BIGINT_UNSIGNED;  // 注意：BIGINT_UNSIGNED是WebSocket专用，但这里是唯一选择
        }
        if (Float.class.isAssignableFrom(fieldType) || float.class == fieldType) {
            return FLOAT;
        }
        if (Double.class.isAssignableFrom(fieldType) || double.class == fieldType) {
            return DOUBLE;
        }
        if (String.class.isAssignableFrom(fieldType)) {
            return NCHAR;  // 优先通用字符串类型，JSON需要显式指定
        }
        if (byte[].class.isAssignableFrom(fieldType)) {
            return VARBINARY;  // 优先官方推荐类型，替代BINARY，排除BLOB（仅列支持）
        }
        // DECIMAL仅WebSocket支持，BigDecimal无匹配

        return null;
    }
}
