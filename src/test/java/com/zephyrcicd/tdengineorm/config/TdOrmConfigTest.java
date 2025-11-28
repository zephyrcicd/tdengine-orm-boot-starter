package com.zephyrcicd.tdengineorm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TdOrmConfig 单元测试
 *
 * @author zjarlin
 */
class TdOrmConfigTest {

    @Test
    @DisplayName("测试从 JDBC URL 中提取数据库名称 - TAOS 协议")
    void testExtractDatabaseNameFromTaosUrl() {
        TdOrmConfig config = new TdOrmConfig();
        config.setUrl("jdbc:TAOS://localhost:6030/test");

        String databaseName = config.getDatabaseName();
        assertEquals("test", databaseName);
    }

    @Test
    @DisplayName("测试从 JDBC URL 中提取数据库名称 - TAOS-RS 协议")
    void testExtractDatabaseNameFromTaosRsUrl() {
        TdOrmConfig config = new TdOrmConfig();
        config.setUrl("jdbc:TAOS-RS://192.168.1.148:6041/iot_data");

        String databaseName = config.getDatabaseName();
        assertEquals("iot_data", databaseName);
    }

    @Test
    @DisplayName("测试从带查询参数的 URL 中提取数据库名称")
    void testExtractDatabaseNameFromUrlWithParams() {
        TdOrmConfig config = new TdOrmConfig();
        config.setUrl("jdbc:TAOS-RS://192.168.1.148:6041/iot_data?useSSL=false");

        String databaseName = config.getDatabaseName();
        assertEquals("iot_data", databaseName);
    }

    @Test
    @DisplayName("测试从带多个查询参数的 URL 中提取数据库名称")
    void testExtractDatabaseNameFromUrlWithMultipleParams() {
        TdOrmConfig config = new TdOrmConfig();
        config.setUrl("jdbc:TAOS-RS://192.168.1.148:6041/iot_data?useSSL=false&charset=UTF-8");

        String databaseName = config.getDatabaseName();
        assertEquals("iot_data", databaseName);
    }


    @Test
    @DisplayName("测试 URL 为空时返回 null")
    void testEmptyUrl() {
        TdOrmConfig config = new TdOrmConfig();
        config.setUrl("");

        String databaseName = config.getDatabaseName();
        assertNull(databaseName);
    }

    @Test
    @DisplayName("测试 URL 为 null 时返回 null")
    void testNullUrl() {
        TdOrmConfig config = new TdOrmConfig();
        config.setUrl(null);

        String databaseName = config.getDatabaseName();
        assertNull(databaseName);
    }

    @Test
    @DisplayName("测试 URL 格式错误时返回 null")
    void testInvalidUrlFormat() {
        TdOrmConfig config = new TdOrmConfig();
        config.setUrl("jdbc:TAOS://localhost:6030");

        String databaseName = config.getDatabaseName();
        assertNull(databaseName);
    }

    @Test
    @DisplayName("测试 URL 中没有数据库名时返回 null")
    void testUrlWithoutDatabaseName() {
        TdOrmConfig config = new TdOrmConfig();
        config.setUrl("jdbc:TAOS://localhost:6030/");

        String databaseName = config.getDatabaseName();
        assertNull(databaseName);
    }

    @Test
    @DisplayName("测试带变量占位符的 URL")
    void testUrlWithPlaceholder() {
        TdOrmConfig config = new TdOrmConfig();
        config.setUrl("jdbc:TAOS-RS://${host}:6041/iot_data?useSSL=false");

        String databaseName = config.getDatabaseName();
        assertEquals("iot_data", databaseName);
    }
}
