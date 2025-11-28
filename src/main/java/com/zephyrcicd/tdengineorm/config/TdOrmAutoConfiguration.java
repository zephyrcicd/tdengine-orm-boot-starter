package com.zephyrcicd.tdengineorm.config;

import com.zephyrcicd.tdengineorm.cache.TagOrderCacheManager;
import com.zephyrcicd.tdengineorm.template.TdTemplate;
import com.zephyrcicd.tdengineorm.template.TsMetaObjectHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * TDengine ORM 自动配置类
 * <p>提供对 TDengine 数据库的自动配置支持</p>
 *
 * @author Zephyr
 */
@Configuration
@EnableConfigurationProperties(TdOrmConfig.class)
@ConditionalOnProperty(prefix = TdOrmConfig.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
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
            setBasicDataSourceProperties(druidDataSource, druidDataSourceClass, tdOrmConfig);

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
            setBasicHikariConfigProperties(hikariConfig, hikariConfigClass, tdOrmConfig);

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
            setBasicDataSourceProperties(basicDataSource, basicDataSourceClass, tdOrmConfig);

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

            setBasicDataSourceProperties(dataSource, driverManagerDataSourceClass, tdOrmConfig);

            return (DataSource) dataSource;
        } catch (Exception e) {
            throw new RuntimeException("创建默认 DataSource 失败", e);
        }
    }

    /**
     * 设置基本数据源属性
     */
    private void setBasicDataSourceProperties(Object dataSource, Class<?> dataSourceClass, TdOrmConfig tdOrmConfig) throws Exception {
        if (StringUtils.hasText(tdOrmConfig.getUrl())) {
            dataSourceClass.getMethod("setUrl", String.class).invoke(dataSource, tdOrmConfig.getUrl());
        }
        dataSourceClass.getMethod("setUsername", String.class).invoke(dataSource, tdOrmConfig.getUsername());
        dataSourceClass.getMethod("setPassword", String.class).invoke(dataSource, tdOrmConfig.getPassword());
        dataSourceClass.getMethod("setDriverClassName", String.class).invoke(dataSource, tdOrmConfig.getDriverClassName());
    }

    /**
     * 设置 HikariConfig 基本属性
     */
    private void setBasicHikariConfigProperties(Object hikariConfig, Class<?> hikariConfigClass, TdOrmConfig tdOrmConfig) throws Exception {
        if (StringUtils.hasText(tdOrmConfig.getUrl())) {
            hikariConfigClass.getMethod("setJdbcUrl", String.class).invoke(hikariConfig, tdOrmConfig.getUrl());
        }
        hikariConfigClass.getMethod("setUsername", String.class).invoke(hikariConfig, tdOrmConfig.getUsername());
        hikariConfigClass.getMethod("setPassword", String.class).invoke(hikariConfig, tdOrmConfig.getPassword());
        hikariConfigClass.getMethod("setDriverClassName", String.class).invoke(hikariConfig, tdOrmConfig.getDriverClassName());
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
     * 创建 TagOrderCacheManager
     * 自动从 URL 中提取数据库名称，或使用配置的 databaseName
     */
    @Bean
    @ConditionalOnMissingBean(TagOrderCacheManager.class)
    public TagOrderCacheManager tagOrderCacheManager(@Qualifier(TDENGINE_DATA_SOURCE) DataSource dataSource,
                                                     TdOrmConfig tdOrmConfig) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String databaseName = tdOrmConfig.getDatabaseName();
        if (databaseName == null) {
            throw new IllegalStateException(
                    "Cannot create TagOrderCacheManager: database name not found. " +
                    "Please configure 'td-orm.database-name' or ensure the database name is present in 'td-orm.url'");
        }
        return new TagOrderCacheManager(jdbcTemplate, databaseName);
    }

    /**
     * 创建 TdTemplate
     */
    @Bean
    @ConditionalOnMissingBean(TdTemplate.class)
    public TdTemplate tdTemplate(@Qualifier(TDENGINE_NAMED_PARAMETER_JDBC_TEMPLATE) NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                 TdOrmConfig tdOrmConfig) {
        TdTemplate tdTemplate = new TdTemplate(namedParameterJdbcTemplate, tdOrmConfig);
        // 根据配置决定是否启用TsMetaObjectHandler
        if (tdOrmConfig.isEnableTsAutoFill()) {
            tdTemplate.setMetaObjectHandler(new TsMetaObjectHandler());
        }
        // 返回代理增强的TdTemplate实例
        return tdTemplate.createProxy();
    }
}
