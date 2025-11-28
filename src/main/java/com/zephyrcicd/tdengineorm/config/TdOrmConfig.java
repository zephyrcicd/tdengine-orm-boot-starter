package com.zephyrcicd.tdengineorm.config;

import com.zephyrcicd.tdengineorm.enums.TdLogLevelEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;


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
     * 连接地址
     */
    private String url;

    /**
     * 用户名
     */
    private String username = "root";

    /**
     * 密码
     */
    private String password = "taosdata";

    /**
     * 驱动类名
     */
    private String driverClassName = "com.taosdata.jdbc.TSDBDriver";

    /**
     * 日志级别
     */
    private TdLogLevelEnum logLevel = TdLogLevelEnum.ERROR;

    /**
     * 是否启用Ts字段自动填充功能，默认开启
     */
    private boolean enableTsAutoFill = true;


    /**
     * 获取数据库名称
     * 如果配置了 databaseName 则直接返回，否则从 URL 中提取
     *
     * @return 数据库名称
     */
    public String getDatabaseName() {
        return extractDatabaseNameFromUrl(url);
    }

    /**
     * 从 JDBC URL 中提取数据库名称
     * 支持的格式：
     * - jdbc:TAOS://host:port/database
     * - jdbc:TAOS-RS://host:port/database?params
     *
     * @param jdbcUrl JDBC URL
     * @return 数据库名称，提取失败返回 null
     */
    private String extractDatabaseNameFromUrl(String jdbcUrl) {
        if (!StringUtils.hasText(jdbcUrl)) {
            return null;
        }

        try {
            // 跳过协议部分 (jdbc:TAOS:// 或 jdbc:TAOS-RS://)
            int protocolEnd = jdbcUrl.indexOf("://");
            if (protocolEnd == -1) {
                return null;
            }

            // 从协议后开始查找数据库路径的 '/'
            int dbPathStart = jdbcUrl.indexOf('/', protocolEnd + 3);
            if (dbPathStart == -1) {
                return null;
            }

            int questionMarkIndex = jdbcUrl.indexOf('?', dbPathStart);
            String dbName;
            if (questionMarkIndex == -1) {
                // 没有查询参数
                dbName = jdbcUrl.substring(dbPathStart + 1);
            } else {
                // 有查询参数
                dbName = jdbcUrl.substring(dbPathStart + 1, questionMarkIndex);
            }

            return StringUtils.hasText(dbName) ? dbName : null;
        } catch (Exception e) {
            return null;
        }
    }

}
