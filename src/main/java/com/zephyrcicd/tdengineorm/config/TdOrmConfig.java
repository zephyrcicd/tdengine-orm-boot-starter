package com.zephyrcicd.tdengineorm.config;

import com.zephyrcicd.tdengineorm.enums.TdLogLevelEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * TDengine ORM 配置类
 *
 * @author Zephyr
 */
@Data
@ConfigurationProperties(prefix = TdOrmConfig.PREFIX)
public class TdOrmConfig {

    public static final String PREFIX = "td-orm";

    /**
     * 是否启用 TDengine ORM 自动配置
     */
    private boolean enabled = true;

    /**
     * 日志级别
     */
    private TdLogLevelEnum logLevel = TdLogLevelEnum.ERROR;

    /**
     * 是否启用Ts字段自动填充功能，默认开启
     */
    private boolean enableTsAutoFill = true;

    /**
     * 分页大小
     * 默认500
     */
    private int pageSize = 500;
}
