package com.zephyrcicd.tdengineorm.enums;

/**
 * @author Zephyr
 */
public enum TdLogLevelEnum {
    DEBUG, INFO, WARN, ERROR;

    public static TdLogLevelEnum match(String logLevelStr) {
        for (TdLogLevelEnum tdLogLevelEnum : values()) {
            if (tdLogLevelEnum.name().equalsIgnoreCase(logLevelStr)) {
                return tdLogLevelEnum;
            }
        }
        return null;
    }
}
