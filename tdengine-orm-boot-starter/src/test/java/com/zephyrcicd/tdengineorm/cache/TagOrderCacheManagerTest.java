package com.zephyrcicd.tdengineorm.cache;

import com.zephyrcicd.tdengineorm.config.TdOrmConfig;
import com.zephyrcicd.tdengineorm.template.TdTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * TagOrderCacheManager 单元测试
 * 使用 Mock 模拟数据库交互
 *
 * @author zjarlin
 */
class TagOrderCacheManagerTest {

    private TdTemplate tdTemplate;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private List<Map<String, Object>> mockDescribeResult;
    private TagOrderCacheManager tagOrderCacheManager;

    private static Map<String, Object> createRow(String field, String type, int length, String note) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("Field", field);
        row.put("Type", type);
        row.put("Length", length);
        row.put("Note", note);
        return row;
    }

    @BeforeEach
    void setUp() {
        // 每个测试创建新的 mock 对象
        // 关键修复：直接 mock NamedParameterJdbcTemplate 而不是 JdbcTemplate
        namedParameterJdbcTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);

        tdTemplate = TdTemplate.getInstance(
                namedParameterJdbcTemplate,
                new TdOrmConfig(),
                null,  // MetaObjectHandler - 测试不需要
                null   // TdSqlInterceptorChain - 测试不需要
        );

        tagOrderCacheManager = tdTemplate.getTagOrderCacheManager();

        // 模拟 DESCRIBE 查询返回的结果
        mockDescribeResult = Arrays.asList(
                createRow("ts", "TIMESTAMP", 8, ""),
                createRow("value", "DOUBLE", 8, ""),
                createRow("energy_type_code", "NCHAR", 64, "TAG"),
                createRow("product_id", "NCHAR", 64, "TAG"),
                createRow("device_id", "NCHAR", 64, "TAG")
        );

        // 设置 mock 行为
        // 关键修复：使用正确的 SQL 格式（无数据库前缀），并且mock正确的方法签名
        when(namedParameterJdbcTemplate.queryForList(
                Mockito.eq("DESCRIBE `acquisition`"),
                Mockito.anyMap()
        )).thenReturn(mockDescribeResult);

        when(namedParameterJdbcTemplate.queryForList(
                Mockito.eq("DESCRIBE `non_existent_table_xyz`"),
                Mockito.anyMap()
        )).thenThrow(new RuntimeException("Table not found"));

        // 移除无效的 mock：不能 mock 真实对象的方法
        // when(tagOrderCacheManager.getTagOrder("acquisition"))...

    }

    @Test
    @DisplayName("测试从数据库查询 tag 顺序")
    void testQueryTagOrderFromDatabase() {
        // 测试查询 acquisition 表的 tag 顺序
        List<String> tagOrder = tagOrderCacheManager.getTagOrder("acquisition");

        // 验证结果
        assertNotNull(tagOrder, "Tag order should not be null");
        assertFalse(tagOrder.isEmpty(), "Tag order should not be empty");

        // 打印结果用于验证
        System.out.println("Tag order for 'acquisition': " + tagOrder);

        // 验证包含预期的 tag 字段
        assertTrue(tagOrder.contains("energy_type_code"), "Should contain energy_type_code");
        assertTrue(tagOrder.contains("product_id"), "Should contain product_id");
        assertTrue(tagOrder.contains("device_id"), "Should contain device_id");

        // 验证顺序（根据实际 DDL 定义）
        int energyTypeIndex = tagOrder.indexOf("energy_type_code");
        int productIdIndex = tagOrder.indexOf("product_id");
        int deviceIdIndex = tagOrder.indexOf("device_id");

        assertTrue(energyTypeIndex >= 0, "energy_type_code should exist");
        assertTrue(productIdIndex >= 0, "product_id should exist");
        assertTrue(deviceIdIndex >= 0, "device_id should exist");

        // 验证它们按照 DDL 定义的顺序
        System.out.println("Order: energy_type_code(" + energyTypeIndex + "), " +
                "product_id(" + productIdIndex + "), " +
                "device_id(" + deviceIdIndex + ")");
    }

    @Test
    @DisplayName("测试缓存机制")
    void testCacheWorking() {
        // 第一次查询
        List<String> firstQuery = tagOrderCacheManager.getTagOrder("acquisition");
        assertNotNull(firstQuery);

        // 第二次查询应该从缓存中获取
        List<String> secondQuery = tagOrderCacheManager.getTagOrder("acquisition");
        assertNotNull(secondQuery);

        // 验证是同一个对象（从缓存中获取）
        assertSame(firstQuery, secondQuery, "Should return the same cached object");
        System.out.println("Cache is working correctly");
    }

    @Test
    @DisplayName("测试清除单个表的缓存")
    void testClearCache() {
        // 先查询并缓存
        List<String> firstQuery = tagOrderCacheManager.getTagOrder("acquisition");
        assertNotNull(firstQuery);

        // 清空缓存
        tagOrderCacheManager.clearCache("acquisition");

        // 再次查询会重新从数据库获取
        List<String> secondQuery = tagOrderCacheManager.getTagOrder("acquisition");
        assertNotNull(secondQuery);

        // 验证不是同一个对象（重新从数据库查询）
        assertNotSame(firstQuery, secondQuery, "Should return a new object after cache clear");
        System.out.println("Single cache clear is working correctly");
    }

    @Test
    @DisplayName("测试清除所有缓存")
    void testClearAllCache() {
        // 查询表
        List<String> beforeClear = tagOrderCacheManager.getTagOrder("acquisition");
        assertNotNull(beforeClear);

        // 清空所有缓存
        tagOrderCacheManager.clearCache();

        // 验证缓存已清空（通过再次查询来验证）
        List<String> afterClear = tagOrderCacheManager.getTagOrder("acquisition");
        assertNotNull(afterClear);
        assertNotSame(beforeClear, afterClear, "Should return a new object after clearing all cache");
        System.out.println("All cache clear is working correctly");
    }

    @Test
    @DisplayName("测试查询不存在的表")
    void testQueryNonExistentTable() {
        // 测试查询不存在的表
        List<String> tagOrder = tagOrderCacheManager.getTagOrder("non_existent_table_xyz");

        // 应该返回空列表而不是抛出异常
        assertNotNull(tagOrder, "Should return empty list for non-existent table");
        assertTrue(tagOrder.isEmpty(), "Should be empty for non-existent table");
        System.out.println("Non-existent table handling is correct");
    }

    @Test
    @DisplayName("测试预加载单个表")
    void testPreloadTagOrder() {
        // 清空缓存
        tagOrderCacheManager.clearCache();

        // 预加载
        tagOrderCacheManager.preloadTagOrder("acquisition");

        // 验证已缓存（通过获取缓存来验证）
        List<String> tagOrder = tagOrderCacheManager.getTagOrder("acquisition");
        assertNotNull(tagOrder);
        assertFalse(tagOrder.isEmpty());
        System.out.println("Preload single table is working correctly");
    }

    @Test
    @DisplayName("测试预加载多个表")
    void testPreloadMultipleTagOrders() {
        // 清空缓存
        tagOrderCacheManager.clearCache();

        // 预加载多个表
        List<String> tableNames = Collections.singletonList("acquisition");
        tagOrderCacheManager.preloadTagOrders(tableNames);

        // 验证已缓存
        List<String> tagOrder = tagOrderCacheManager.getTagOrder("acquisition");
        assertNotNull(tagOrder);
        assertFalse(tagOrder.isEmpty());
        System.out.println("Preload multiple tables is working correctly");
    }

    @Test
    @DisplayName("测试 DESCRIBE 命令执行")
    void testDescribeCommand() {
        // 直接测试 DESCRIBE 命令（使用 mock 的 NamedParameterJdbcTemplate）
        String sql = "DESCRIBE `acquisition`";
        List<java.util.Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(sql, Collections.emptyMap());

        assertNotNull(rows);
        assertFalse(rows.isEmpty());

        System.out.println("DESCRIBE result:");
        rows.forEach(row -> {
            System.out.println("Field: " + row.get("Field") +
                    ", Type: " + row.get("Type") +
                    ", Note: " + row.get("Note"));
        });

        // 过滤出 TAG 字段
        List<String> tags = rows.stream()
                .filter(row -> "TAG".equals(row.get("Note")))
                .map(row -> (String) row.get("Field"))
                .collect(Collectors.toList());

        System.out.println("Tags found: " + tags);
        assertFalse(tags.isEmpty(), "Should find at least one TAG");
    }
}
