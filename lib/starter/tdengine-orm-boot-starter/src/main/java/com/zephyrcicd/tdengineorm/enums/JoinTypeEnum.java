package com.zephyrcicd.tdengineorm.enums;

import lombok.Getter;

/**
 * @author Zephyr
 */
@Getter
public enum JoinTypeEnum {
    /**
     * 内连接
     */
    INNER_JOIN(" JOIN "),
    /**
     * 左连接
     */
    LEFT_JOIN(" LEFT JOIN "),
    /**
     * 右连接
     */
    RIGHT_JOIN(" RIGHT JOIN ");

    private final String sql;

    JoinTypeEnum(String sql) {
        this.sql = sql;
    }
}
