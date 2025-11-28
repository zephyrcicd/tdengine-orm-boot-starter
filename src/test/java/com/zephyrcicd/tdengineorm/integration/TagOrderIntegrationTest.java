package com.zephyrcicd.tdengineorm.integration;

import com.zephyrcicd.tdengineorm.cache.TagOrderCacheManager;
import com.zephyrcicd.tdengineorm.config.TdOrmConfig;
import com.zephyrcicd.tdengineorm.entity.Acquisition;
import com.zephyrcicd.tdengineorm.strategy.DefaultTagNameStrategy;
import com.zephyrcicd.tdengineorm.template.TdTemplate;
import com.zephyrcicd.tdengineorm.util.TdSqlUtil;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tag 顺序集成测试
 * 测试完整流程：TdTemplate + TagOrderCacheManager + DefaultTagNameStrategy
 * 使用 Mock 模拟数据库交互
 *
 * @author zjarlin
 */
class TagOrderIntegrationTest {

    private static JdbcTemplate jdbcTemplate;
    private static NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static TdOrmConfig tdOrmConfig;
    private static TdTemplate tdTemplate;
    private static TagOrderCacheManager tagOrderCacheManager;

    @BeforeAll
    static void setUpAll() {
        // 使用 Mock 模拟 JdbcTemplate
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        namedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);

        // 模拟 DESCRIBE 查询返回的 tag 顺序
        List<Map<String, Object>> describeResult = Arrays.asList(
                createRow("ts", "TIMESTAMP", 8, ""),
                createRow("value", "DOUBLE", 8, ""),
                createRow("energy_type_code", "NCHAR", 64, "TAG"),
                createRow("product_id", "NCHAR", 64, "TAG"),
                createRow("device_id", "NCHAR", 64, "TAG")
        );
        when(jdbcTemplate.queryForList(anyString())).thenReturn(describeResult);

        // 创建配置对象
        tdOrmConfig = new TdOrmConfig();
        tdOrmConfig.setUrl("jdbc:TAOS-RS://localhost:6041/iot_data?useSSL=false");
        tdOrmConfig.setUsername("root");
        tdOrmConfig.setPassword("taosdata");
        tdOrmConfig.setDriverClassName("com.taosdata.jdbc.rs.RestfulDriver");

        // 创建 TdTemplate
        tdTemplate = new TdTemplate(namedParameterJdbcTemplate, tdOrmConfig);

        // 创建 TagOrderCacheManager
        String databaseName = tdOrmConfig.getDatabaseName();
        assertNotNull(databaseName, "Database name should be extracted from URL");
        assertEquals("iot_data", databaseName, "Database name should be iot_data");

        tagOrderCacheManager = new TagOrderCacheManager(jdbcTemplate, databaseName);
    }

    private static Map<String, Object> createRow(String field, String type, int length, String note) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Field", field);
        row.put("Type", type);
        row.put("Length", length);
        row.put("Note", note);
        return row;
    }

    @AfterAll
    static void tearDownAll() {
        // Mock 对象不需要关闭
    }

    @Test
    @DisplayName("测试从配置 URL 中自动提取数据库名称")
    void testExtractDatabaseNameFromConfig() {
        String databaseName = tdOrmConfig.getDatabaseName();
        assertNotNull(databaseName);
        assertEquals("iot_data", databaseName);
        System.out.println("✓ Database name extracted from URL: " + databaseName);
    }

    @Test
    @DisplayName("测试 TagOrderCacheManager 查询 tag 顺序")
    void testTagOrderCacheManager() {
        List<String> tagOrder = tagOrderCacheManager.getTagOrder("acquisition");

        assertNotNull(tagOrder);
        assertFalse(tagOrder.isEmpty());

        System.out.println("✓ Tag order from database: " + tagOrder);

        // 验证包含所有预期的 tag
        assertTrue(tagOrder.contains("energy_type_code"));
        assertTrue(tagOrder.contains("product_id"));
        assertTrue(tagOrder.contains("device_id"));
    }

    @Test
    @DisplayName("测试提取实体类的 tag 字段")
    void testExtractTagFieldsFromEntity() {
        Acquisition acquisition = Acquisition.builder()
                .ts(System.currentTimeMillis())
                .value(123.45)
                .energyTypeCode("ELEC")
                .productId("PROD001")
                .deviceId("DEV001")
                .build();

        // 测试获取有序的 tag 字段对
        List<Pair<String, String>> tagFieldsPair = TdSqlUtil.getAllTagFieldsPairOrdered(acquisition);

        assertNotNull(tagFieldsPair);
        assertEquals(3, tagFieldsPair.size());

        System.out.println("✓ Tag fields from entity:");
        tagFieldsPair.forEach(pair ->
                System.out.println("  - " + pair.getFirst() + " = " + pair.getSecond())
        );
    }

    @Test
    @DisplayName("测试 DefaultTagNameStrategy 生成表名")
    void testDefaultTagNameStrategy() {
        // 创建策略实例
        DefaultTagNameStrategy strategy = new DefaultTagNameStrategy(tagOrderCacheManager);

        // 创建实体
        Acquisition acquisition = Acquisition.builder()
                .ts(System.currentTimeMillis())
                .value(123.45)
                .energyTypeCode("ELEC")
                .productId("PROD001")
                .deviceId("DEV001")
                .build();

        // 生成表名
        String tableName = strategy.getTableName(acquisition);

        assertNotNull(tableName);
        System.out.println("✓ Generated table name: " + tableName);

        // 验证表名格式
        assertTrue(tableName.startsWith("acquisition_"));

        // 验证表名包含所有 tag 值
        assertTrue(tableName.contains("ELEC"));
        assertTrue(tableName.contains("PROD001"));
        assertTrue(tableName.contains("DEV001"));
    }

    @Test
    @DisplayName("测试完整流程：按 DDL 顺序生成表名")
    void testCompleteFlowWithCorrectOrder() {
        // 从数据库获取 tag 顺序
        List<String> tagOrder = tagOrderCacheManager.getTagOrder("acquisition");
        System.out.println("✓ Tag order from DDL: " + tagOrder);

        // 创建策略
        DefaultTagNameStrategy strategy = new DefaultTagNameStrategy(tagOrderCacheManager);

        // 创建实体
        Acquisition acquisition = Acquisition.builder()
                .ts(System.currentTimeMillis())
                .value(123.45)
                .energyTypeCode("ELEC")
                .productId("PROD001")
                .deviceId("DEV001")
                .build();

        // 生成表名
        String tableName = strategy.getTableName(acquisition);
        System.out.println("✓ Generated table name: " + tableName);

        // 根据 DDL 顺序验证表名
        // 假设 DDL 顺序是: energy_type_code, product_id, device_id
        String expectedTableName = buildExpectedTableName(tagOrder, acquisition);
        System.out.println("✓ Expected table name: " + expectedTableName);

        assertEquals(expectedTableName, tableName,
                "Table name should follow DDL tag order");
    }

    @Test
    @DisplayName("测试多个实体生成不同的表名")
    void testMultipleEntitiesDifferentTableNames() {
        DefaultTagNameStrategy strategy = new DefaultTagNameStrategy(tagOrderCacheManager);

        Acquisition acquisition1 = Acquisition.builder()
                .energyTypeCode("ELEC")
                .productId("PROD001")
                .deviceId("DEV001")
                .build();

        Acquisition acquisition2 = Acquisition.builder()
                .energyTypeCode("GAS")
                .productId("PROD002")
                .deviceId("DEV002")
                .build();

        String tableName1 = strategy.getTableName(acquisition1);
        String tableName2 = strategy.getTableName(acquisition2);

        System.out.println("✓ Table name 1: " + tableName1);
        System.out.println("✓ Table name 2: " + tableName2);

        assertNotEquals(tableName1, tableName2,
                "Different entities should generate different table names");
    }

    @Test
    @DisplayName("测试缓存生效避免重复查询")
    void testCachingAvoidsRepeatedQueries() {
        // 清空缓存
        tagOrderCacheManager.clearCache();

        // 第一次查询
        long startTime1 = System.currentTimeMillis();
        List<String> tagOrder1 = tagOrderCacheManager.getTagOrder("acquisition");
        long duration1 = System.currentTimeMillis() - startTime1;

        // 第二次查询（应该从缓存获取）
        long startTime2 = System.currentTimeMillis();
        List<String> tagOrder2 = tagOrderCacheManager.getTagOrder("acquisition");
        long duration2 = System.currentTimeMillis() - startTime2;

        System.out.println("✓ First query duration: " + duration1 + "ms");
        System.out.println("✓ Second query duration (cached): " + duration2 + "ms");

        // 验证返回相同的对象（从缓存获取）
        assertSame(tagOrder1, tagOrder2, "Should return cached object");

        // 缓存的查询应该更快（通常 < 1ms）
        assertTrue(duration2 < duration1 || duration2 < 5,
                "Cached query should be faster");
    }

    /**
     * 根据 DDL 顺序构建预期的表名
     */
    private String buildExpectedTableName(List<String> tagOrder, Acquisition acquisition) {
        StringBuilder sb = new StringBuilder("acquisition");

        for (String tagName : tagOrder) {
            String tagValue = getTagValue(tagName, acquisition);
            if (tagValue != null) {
                sb.append("_").append(tagValue);
            }
        }

        return sb.toString();
    }

    /**
     * 根据 tag 名称获取实体的 tag 值
     */
    private String getTagValue(String tagName, Acquisition acquisition) {
        switch (tagName) {
            case "energy_type_code":
                return acquisition.getEnergyTypeCode();
            case "product_id":
                return acquisition.getProductId();
            case "device_id":
                return acquisition.getDeviceId();
            default:
                return null;
        }
    }
}
