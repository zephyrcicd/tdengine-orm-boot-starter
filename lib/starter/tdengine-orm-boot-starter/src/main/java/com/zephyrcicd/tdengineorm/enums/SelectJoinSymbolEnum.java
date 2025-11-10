package com.zephyrcicd.tdengineorm.enums;

import lombok.Getter;

/**
 * @author Zephyr
 */
@Getter
public enum SelectJoinSymbolEnum {
    /**
     * 加法
     */
    PLUS(" + "),
    /**
     * 减法
     */
    MINUS(" - "),
    /**
     * 乘法
     */
    MULTIPLICATION(" * "),
    /**
     * 除法
     */
    DIVISION(" / "),
    /**
     * 左括号
     */
    LEFT_BRACKETS("("),
    /**
     * 右括号
     */
    RIGHT_BRACKETS(")"),
    ;

    private final String key;

    SelectJoinSymbolEnum(String key) {
        this.key = key;
    }
}
