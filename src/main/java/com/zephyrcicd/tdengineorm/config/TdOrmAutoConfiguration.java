package com.zephyrcicd.tdengineorm.config;

import com.zephyrcicd.tdengineorm.template.TdTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * TDengine ORM 自动配置类
 *
 * @author Zephyr
 */
@Configuration
@EnableConfigurationProperties(TdOrmConfig.class)
@ConditionalOnProperty(prefix = TdOrmConfig.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class TdOrmAutoConfiguration {

    public static final String TDENGINE_DATA_SOURCE = "tdengineDataSource";
    public static final String TDENGINE_NAMED_PARAMETER_JDBC_TEMPLATE = "tdengineNamedParameterJdbcTemplate";

    /**
     * 当 Druid 连接池存在时，创建 Druid DataSource
     * 优先级最高
     */
    @Bean(TDENGINE_DATA_SOURCE)
    @ConditionalOnClass(name = "com.alibaba.druid.pool.DruidDataSource")
    @ConditionalOnMissingBean(name = TDENGINE_DATA_SOURCE)
    public DataSource druidDataSource(TdOrmConfig tdOrmConfig) {
        try {
            Class<?> druidDataSourceClass = Class.forName("com.alibaba.druid.pool.DruidDataSource");
            Object druidDataSource = druidDataSourceClass.getDeclaredConstructor().newInstance();

            // 设置基本连接信息
            if (StringUtils.hasText(tdOrmConfig.getUrl())) {
                druidDataSourceClass.getMethod("setUrl", String.class).invoke(druidDataSource, tdOrmConfig.getUrl());
            }
            druidDataSourceClass.getMethod("setUsername", String.class).invoke(druidDataSource, tdOrmConfig.getUsername());
            druidDataSourceClass.getMethod("setPassword", String.class).invoke(druidDataSource, tdOrmConfig.getPassword());
            druidDataSourceClass.getMethod("setDriverClassName", String.class).invoke(druidDataSource, tdOrmConfig.getDriverClassName());

            // 设置 Druid 特有配置
            druidDataSourceClass.getMethod("setInitialSize", int.class).invoke(druidDataSource, 5);
            druidDataSourceClass.getMethod("setMaxActive", int.class).invoke(druidDataSource, 20);
            druidDataSourceClass.getMethod("setMinIdle", int.class).invoke(druidDataSource, 5);
            druidDataSourceClass.getMethod("setMaxWait", long.class).invoke(druidDataSource, 60000L);
            druidDataSourceClass.getMethod("setValidationQuery", String.class).invoke(druidDataSource, "SELECT 1");
            druidDataSourceClass.getMethod("setTestOnBorrow", boolean.class).invoke(druidDataSource, false);
            druidDataSourceClass.getMethod("setTestOnReturn", boolean.class).invoke(druidDataSource, false);
            druidDataSourceClass.getMethod("setTestWhileIdle", boolean.class).invoke(druidDataSource, true);

            return (DataSource) druidDataSource;
        } catch (Exception e) {
            throw new RuntimeException("创建 Druid DataSource 失败", e);
        }
    }

    /**
     * 当 HikariCP 连接池存在时，创建 HikariCP DataSource
     * 优先级次之
     */
    @Bean(TDENGINE_DATA_SOURCE)
    @ConditionalOnClass(name = "com.zaxxer.hikari.HikariDataSource")
    @ConditionalOnMissingBean(name = TDENGINE_DATA_SOURCE)
    public DataSource hikariDataSource(TdOrmConfig tdOrmConfig) {
        try {
            Class<?> hikariConfigClass = Class.forName("com.zaxxer.hikari.HikariConfig");
            Class<?> hikariDataSourceClass = Class.forName("com.zaxxer.hikari.HikariDataSource");

            Object hikariConfig = hikariConfigClass.getDeclaredConstructor().newInstance();

            // 设置基本连接信息
            if (StringUtils.hasText(tdOrmConfig.getUrl())) {
                hikariConfigClass.getMethod("setJdbcUrl", String.class).invoke(hikariConfig, tdOrmConfig.getUrl());
            }
            hikariConfigClass.getMethod("setUsername", String.class).invoke(hikariConfig, tdOrmConfig.getUsername());
            hikariConfigClass.getMethod("setPassword", String.class).invoke(hikariConfig, tdOrmConfig.getPassword());
            hikariConfigClass.getMethod("setDriverClassName", String.class).invoke(hikariConfig, tdOrmConfig.getDriverClassName());

            // 设置 HikariCP 特有配置
            hikariConfigClass.getMethod("setMaximumPoolSize", int.class).invoke(hikariConfig, 20);
            hikariConfigClass.getMethod("setMinimumIdle", int.class).invoke(hikariConfig, 5);
            hikariConfigClass.getMethod("setConnectionTimeout", long.class).invoke(hikariConfig, 30000L);
            hikariConfigClass.getMethod("setIdleTimeout", long.class).invoke(hikariConfig, 600000L);
            hikariConfigClass.getMethod("setMaxLifetime", long.class).invoke(hikariConfig, 1800000L);

            return (DataSource) hikariDataSourceClass.getDeclaredConstructor(hikariConfigClass).newInstance(hikariConfig);
        } catch (Exception e) {
            throw new RuntimeException("创建 HikariCP DataSource 失败", e);
        }
    }

    /**
     * 当 Apache DBCP2 连接池存在时，创建 DBCP2 DataSource
     * 优先级再次之
     */
    @Bean(TDENGINE_DATA_SOURCE)
    @ConditionalOnClass(name = "org.apache.commons.dbcp2.BasicDataSource")
    @ConditionalOnMissingBean(name = TDENGINE_DATA_SOURCE)
    public DataSource dbcp2DataSource(TdOrmConfig tdOrmConfig) {
        try {
            Class<?> basicDataSourceClass = Class.forName("org.apache.commons.dbcp2.BasicDataSource");
            Object basicDataSource = basicDataSourceClass.getDeclaredConstructor().newInstance();

            // 设置基本连接信息
            if (StringUtils.hasText(tdOrmConfig.getUrl())) {
                basicDataSourceClass.getMethod("setUrl", String.class).invoke(basicDataSource, tdOrmConfig.getUrl());
            }
            basicDataSourceClass.getMethod("setUsername", String.class).invoke(basicDataSource, tdOrmConfig.getUsername());
            basicDataSourceClass.getMethod("setPassword", String.class).invoke(basicDataSource, tdOrmConfig.getPassword());
            basicDataSourceClass.getMethod("setDriverClassName", String.class).invoke(basicDataSource, tdOrmConfig.getDriverClassName());

            // 设置 DBCP2 特有配置
            basicDataSourceClass.getMethod("setInitialSize", int.class).invoke(basicDataSource, 5);
            basicDataSourceClass.getMethod("setMaxTotal", int.class).invoke(basicDataSource, 20);
            basicDataSourceClass.getMethod("setMinIdle", int.class).invoke(basicDataSource, 5);
            basicDataSourceClass.getMethod("setMaxIdle", int.class).invoke(basicDataSource, 10);
            basicDataSourceClass.getMethod("setMaxWaitMillis", long.class).invoke(basicDataSource, 60000L);
            basicDataSourceClass.getMethod("setValidationQuery", String.class).invoke(basicDataSource, "SELECT 1");
            basicDataSourceClass.getMethod("setTestOnBorrow", boolean.class).invoke(basicDataSource, false);
            basicDataSourceClass.getMethod("setTestOnReturn", boolean.class).invoke(basicDataSource, false);
            basicDataSourceClass.getMethod("setTestWhileIdle", boolean.class).invoke(basicDataSource, true);

            return (DataSource) basicDataSource;
        } catch (Exception e) {
            throw new RuntimeException("创建 DBCP2 DataSource 失败", e);
        }
    }

    /**
     * 兜底方案：使用 Spring Boot 默认的简单 DataSource
     * 优先级最低
     */
    @Bean(TDENGINE_DATA_SOURCE)
    @ConditionalOnMissingBean(name = TDENGINE_DATA_SOURCE)
    public DataSource defaultDataSource(TdOrmConfig tdOrmConfig) {
        try {
            // 使用 Spring 的 DriverManagerDataSource 作为兜底方案
            Class<?> driverManagerDataSourceClass = Class.forName("org.springframework.jdbc.datasource.DriverManagerDataSource");
            Object dataSource = driverManagerDataSourceClass.getDeclaredConstructor().newInstance();

            if (StringUtils.hasText(tdOrmConfig.getUrl())) {
                driverManagerDataSourceClass.getMethod("setUrl", String.class).invoke(dataSource, tdOrmConfig.getUrl());
            }
            driverManagerDataSourceClass.getMethod("setUsername", String.class).invoke(dataSource, tdOrmConfig.getUsername());
            driverManagerDataSourceClass.getMethod("setPassword", String.class).invoke(dataSource, tdOrmConfig.getPassword());
            driverManagerDataSourceClass.getMethod("setDriverClassName", String.class).invoke(dataSource, tdOrmConfig.getDriverClassName());

            return (DataSource) dataSource;
        } catch (Exception e) {
            throw new RuntimeException("创建默认 DataSource 失败", e);
        }
    }

    /**
     * 创建 TDengine 专用的 NamedParameterJdbcTemplate
     */
    @Bean(TDENGINE_NAMED_PARAMETER_JDBC_TEMPLATE)
    @ConditionalOnMissingBean(name = TDENGINE_NAMED_PARAMETER_JDBC_TEMPLATE)
    public NamedParameterJdbcTemplate tdengineNamedParameterJdbcTemplate(@Qualifier(TDENGINE_DATA_SOURCE) DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * 创建 TdTemplate
     */
    @Bean
    @ConditionalOnMissingBean(TdTemplate.class)
    public TdTemplate tdTemplate(@Qualifier(TDENGINE_NAMED_PARAMETER_JDBC_TEMPLATE) NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                 TdOrmConfig tdOrmConfig) {
        return new TdTemplate(namedParameterJdbcTemplate, tdOrmConfig);
    }
}
