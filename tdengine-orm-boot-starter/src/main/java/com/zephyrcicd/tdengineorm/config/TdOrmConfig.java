package com.zephyrcicd.tdengineorm.config;

import com.zephyrcicd.tdengineorm.enums.NamingStyleEnum;
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

    /**
     * 是否启用 SQL 拦截器功能，默认开启
     * <p>
     * 开启后，所有 SQL 执行都会经过拦截器链处理。
     * 可以通过实现 {@link com.zephyrcicd.tdengineorm.interceptor.TdSqlInterceptor} 接口
     * 并注册为 Spring Bean 来添加自定义拦截器。
     * </p>
     */
    private boolean enableSqlInterceptor = true;

    /**
     * 命名风格
     * <p>
     * DEFAULT: 使用超级表名作为表名
     * TAG_JOIN: 使用超级表名 + Tag字段值拼接（需要 TagOrderCacheManager 支持）
     * </p>
     */
    private NamingStyleEnum namingStyle = NamingStyleEnum.TAG_JOIN;
}
