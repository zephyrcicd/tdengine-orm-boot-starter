package com.zephyrcicd.tdengineorm.enums;

import lombok.Getter;

/**
 * TDengine常用聚合函数
 *
 * @author Zephyr
 */
@Getter
public enum TdSelectFuncEnum {

    /**
     * 计数
     */
    COUNT("COUNT({}) {}"),
    /**
     * 获取平均值
     */
    AVG("AVG({}) {}"),
    /**
     * 取区间结果的第一条
     */
    FIRST("FIRST({}) {}"),
    /**
     * 取区间最后一条
     */
    LAST("LAST({}) {}"),
    /**
     * 总和
     */
    SUM("SUM({}) {}"),
    /**
     * 取绝对值
     */
    ABS("ABS({}) {}"),
    /**
     * 向上取整
     */
    CEIL("CEIL({}) {}"),
    /**
     * 向下取整
     */
    FLOOR("FLOOR({}) {}"),
    /**
     * 字符串拼接
     */
    CONCAT("CONCAT({}) {}"),
    /**
     * 字符串转小写
     */
    LOWER("LOWER({}) {}"),
    /**
     * 转为大写
     */
    UPPER("UPPER({}) {}"),
    /**
     * 字符串切割
     */
    SUBSTR("SUBSTR({}) {}");

    private final String func;

    TdSelectFuncEnum(String func) {
        this.func = func;
    }
}
