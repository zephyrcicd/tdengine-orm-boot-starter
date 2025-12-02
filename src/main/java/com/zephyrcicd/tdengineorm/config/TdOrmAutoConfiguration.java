package com.zephyrcicd.tdengineorm.config;

import com.zephyrcicd.tdengineorm.cache.TagOrderCacheManager;
import com.zephyrcicd.tdengineorm.interceptor.LoggingSqlInterceptor;
import com.zephyrcicd.tdengineorm.interceptor.TdSqlInterceptor;
import com.zephyrcicd.tdengineorm.interceptor.TdSqlInterceptorChain;
import com.zephyrcicd.tdengineorm.template.MetaObjectHandler;
import com.zephyrcicd.tdengineorm.template.TdTemplate;
import com.zephyrcicd.tdengineorm.template.TsMetaObjectHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

/**
 * TDengine ORM 自动配置类
 * <p>提供对 TDengine 数据库的自动配置支持</p>
 *
 * @author Zephyr
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TdOrmConfig.class)
public class TdOrmAutoConfiguration {

    /**
     * 默认的 MetaObjectHandler（ts 字段自动填充）
     */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    @ConditionalOnProperty(prefix = TdOrmConfig.PREFIX, name = "enable-ts-auto-fill", havingValue = "true", matchIfMissing = true)
    public MetaObjectHandler tsMetaObjectHandler() {
        return new TsMetaObjectHandler();
    }

    /**
     * SQL 拦截器链
     * <p>
     * 自动收集所有 {@link TdSqlInterceptor} bean 并组装成拦截器链。
     * 内置的 {@link LoggingSqlInterceptor} 会自动添加到链中。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(TdSqlInterceptorChain.class)
    @ConditionalOnProperty(prefix = TdOrmConfig.PREFIX, name = "enable-sql-interceptor", havingValue = "true", matchIfMissing = true)
    public TdSqlInterceptorChain tdSqlInterceptorChain(
            ObjectProvider<List<TdSqlInterceptor>> interceptorsProvider,
            TdOrmConfig tdOrmConfig) {

        TdSqlInterceptorChain chain = new TdSqlInterceptorChain();

        // 1. 添加内置日志拦截器（替代原 tdLog 方法）
        chain.addInterceptor(new LoggingSqlInterceptor(tdOrmConfig));

        // 2. 收集所有用户自定义的 TdSqlInterceptor bean
        List<TdSqlInterceptor> customInterceptors = interceptorsProvider.getIfAvailable();
        if (customInterceptors != null) {
            chain.addInterceptors(customInterceptors);
        }

        return chain;
    }

    /**
     * 创建 TdTemplate
     * <p>
     * 需要用户自行配置数据源，如果开启了td-orm，并且默认使用主数据源
     * <br/>
     * 若用户有多个数据源，并且需要传递给td-orm的非主数据源，则需要自行创建TdTemplate对象
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(TdTemplate.class)
    @ConditionalOnProperty(prefix = TdOrmConfig.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
    public TdTemplate tdTemplate(DataSource dataSource,
                                  TdOrmConfig tdOrmConfig,
                                  ObjectProvider<MetaObjectHandler> metaObjectHandlerProvider,
                                  ObjectProvider<TdSqlInterceptorChain> sqlInterceptorChainProvider) {
        return TdTemplate.getInstance(
                new NamedParameterJdbcTemplate(dataSource),
                tdOrmConfig,
                metaObjectHandlerProvider.getIfAvailable(),
                sqlInterceptorChainProvider.getIfAvailable()
        );
    }


    /**
     * 创建 TagOrderCacheManager
     * 自动从 URL 中提取数据库名称，或使用配置的 databaseName
     */
    @Bean
    @ConditionalOnBean(TdTemplate.class)
    @ConditionalOnMissingBean(TagOrderCacheManager.class)
    public TagOrderCacheManager tagOrderCacheManager(TdTemplate tdTemplate) {
        return new TagOrderCacheManager(tdTemplate);
    }
}
