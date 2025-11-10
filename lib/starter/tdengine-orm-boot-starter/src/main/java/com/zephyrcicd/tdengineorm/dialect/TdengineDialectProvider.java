package com.zephyrcicd.tdengineorm.dialect;

import org.springframework.data.jdbc.repository.config.DialectResolver;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.jdbc.core.JdbcOperations;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Locale;
import java.util.Optional;

/**
 * TDengine 方言提供者
 *
 * <p>通过 Spring 的 SPI 机制（SpringFactoriesLoader）自动注册，
 * 让 Spring Data JDBC 的 DialectResolver 能够识别 TDengine 数据库。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *     <li>Spring Boot 启动时通过 {@code META-INF/spring.factories} 加载此类</li>
 *     <li>{@link DialectResolver} 遍历所有 Provider</li>
 *     <li>调用 {@link #getDialect(JdbcOperations)} 方法尝试识别数据库</li>
 *     <li>如果识别成功，返回 {@link TdengineDialect} 实例</li>
 * </ol>
 *
 * <p>识别规则：通过 {@code DatabaseMetaData.getDatabaseProductName()} 获取数据库产品名称，
 * 如果包含 "tdengine" 或 "taos" 关键字（不区分大小写），则认为是 TDengine 数据库。</p>
 *
 * @author Zephyr
 * @since 1.2.1
 */
public class TdengineDialectProvider implements DialectResolver.JdbcDialectProvider {

    /**
     * 尝试为给定的 JdbcOperations 返回合适的 Dialect
     *
     * @param operations JDBC 操作对象，用于获取数据库连接和元数据
     * @return 如果识别为 TDengine 数据库，返回 {@link TdengineDialect}；否则返回 {@link Optional#empty()}
     */
    @Override
    public Optional<Dialect> getDialect(JdbcOperations operations) {
        return Optional.ofNullable(operations.execute((Connection connection) -> {
            try {
                DatabaseMetaData metaData = connection.getMetaData();
                String productName = metaData.getDatabaseProductName().toLowerCase(Locale.ENGLISH);

                // TDengine JDBC 驱动返回的产品名称可能是 "TDengine" 或 "taos"
                if (productName.contains("tdengine") || productName.contains("taos")) {
                    return (Dialect) TdengineDialect.INSTANCE;
                }

                return null;
            } catch (Exception e) {
                // 如果获取元数据失败，返回 null，让其他 Provider 尝试
                return null;
            }
        }));
    }
}
