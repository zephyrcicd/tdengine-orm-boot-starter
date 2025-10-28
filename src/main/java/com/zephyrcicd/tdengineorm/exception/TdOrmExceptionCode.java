package com.zephyrcicd.tdengineorm.exception;


/**
 * @author Zephyr
 */
public enum TdOrmExceptionCode implements ExceptionCode {

    /**
     * 查询语句未提供查询的字段
     */
    NO_SELECT(70001, "No select column found"),
    /**
     * 找不到字段
     */
    NO_FILED(70002, "No filed found"),

    /**
     * 字段无长度
     */
    FIELD_NO_LENGTH(70003, "Filed must has length!"),

    /**
     * 未找到普通字段
     */
    NO_COMM_FIELD(70004, "No comm field found!"),

    /**
     * 未找到被@PrimaryTs标记的字段
     */
    NO_PRIMARY_TS(70005, "No @PrimaryTs field found!"),

    /**
     * sql嵌套层数超过1层
     */
    SQL_LAYER_OUT_LIMITED(70006, "The number of layers exceeds the limit!"),

    /**
     * 未设置最终字段别名
     */
    COLUMN_NO_ALIAS_NAME(70007, "Select joiner must set finalColumnAliasName!"),
    /**
     * 未匹配到合适的字段类型
     */
    CANT_NOT_MATCH_FIELD_TYPE(7008, "Not matched to the appropriate field type!"),

    /**
     * 参数值不能为空
     */
    PARAM_VALUE_CANT_NOT_BE_NULL(7009, "Parameter value cannot be null!"),

    /**
     * 多次调用GroupBy
     */
    MULTI_GROUP_BY(70010, "Call GroupBy multiple times!"),
    /**
     * 缺少TAG字段
     */
    NO_TAG_FIELD(70011, "The super table must have a tag field!"),

    TABLE_NAME_BLANK(70012, "Table name cannot be blank!"),
    ;

    private final Integer code;
    private final String message;

    TdOrmExceptionCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getMsg() {
        return this.message;
    }
}
