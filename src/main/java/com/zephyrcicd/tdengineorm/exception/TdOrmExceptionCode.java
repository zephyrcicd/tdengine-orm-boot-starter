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
     * 未找到`ts`字段（TDengine要求所有表都有ts字段，并且为Timestamp类型）
     */
    NO_TS_COLUMN_FOUND(70005, "No `ts` column found!"),

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
    CANT_NOT_MATCH_FIELD_TYPE(70008, "Not matched to the appropriate field type!"),

    /**
     * 参数值不能为空
     */
    PARAM_VALUE_CANT_NOT_BE_NULL(70009, "Parameter value cannot be null!"),

    /**
     * 多次调用GroupBy
     */
    MULTI_GROUP_BY(70010, "Call GroupBy multiple times!"),
    /**
     * 缺少TAG字段
     */
    NO_TAG_FIELD(70011, "The super table must have a tag field!"),

    TABLE_NAME_BLANK(70012, "Table name cannot be blank!"),

    /**
     * TAG字段不能是复合字段
     */
    TAG_FIELD_CAN_NOT_BE_COMPOSITE_FIELD(70013, "Tag field can't be composite key!"),

    /**
     * 不支持使用Map.class作为返回类型
     */
    MAP_TYPE_NOT_SUPPORTED(70014, "Map.class is not supported as result type! Please use listAsMap() or getOneAsMap() methods instead."),

    ENTITY_LIST_IS_EMPTY(70015, "Entity list is empty!"),
    PARTITION_SIZE_IS_ZERO(70016, "Partition size must be greater than zero"),
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
