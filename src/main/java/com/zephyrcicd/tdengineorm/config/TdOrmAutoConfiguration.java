package com.zephyrcicd.tdengineorm.config;

import com.zephyrcicd.tdengineorm.cache.TagOrderCacheManager;
import com.zephyrcicd.tdengineorm.template.TdTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

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
    public TdTemplate tdTemplate(DataSource dataSource, TdOrmConfig tdOrmConfig) {
        return TdTemplate.getInstance(new NamedParameterJdbcTemplate(dataSource), tdOrmConfig);
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
