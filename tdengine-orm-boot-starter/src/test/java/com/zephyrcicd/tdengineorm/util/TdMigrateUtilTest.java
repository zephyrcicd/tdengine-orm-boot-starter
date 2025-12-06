package com.zephyrcicd.tdengineorm.util;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TdMigrateUtil 单元测试
 * 测试列交换和重命名功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TdMigrateUtilTest {

    private static final String URL = "jdbc:TAOS-RS://192.168.1.148:6041/iot_data?useSSL=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "taosdata";

    private static final String TEST_TABLE = "migrate_test_" + System.currentTimeMillis();

    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static Connection connection;

    @BeforeAll
    static void setUp() throws Exception {
        Class.forName("com.taosdata.jdbc.rs.RestfulDriver");
        connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(connection, true);
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        createTestTable();
        insertTestData();
    }

    @AfterAll
    static void tearDown() {
        try {
            jdbcTemplate.update("DROP TABLE IF EXISTS " + TEST_TABLE, Collections.emptyMap());
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createTestTable() {
        String createSql = "CREATE TABLE IF NOT EXISTS " + TEST_TABLE +
                " (ts TIMESTAMP, col1 INT, col2 INT, col3 NCHAR(50))";
        jdbcTemplate.update(createSql, Collections.emptyMap());
        System.out.println("Created test table: " + TEST_TABLE);
    }

    private static void insertTestData() {
        long now = System.currentTimeMillis();
        String insertSql = "INSERT INTO " + TEST_TABLE + " VALUES " +
                "('" + now + "', 100, 200, 'test1'), " +
                "('" + (now + 1000) + "', 300, 400, 'test2')";
        jdbcTemplate.update(insertSql, Collections.emptyMap());
        System.out.println("Inserted test data");
    }

    @Test
    @Order(1)
    @DisplayName("测试生成交换列SQL - 验证SQL语法正确")
    void testGenerateSwapSqls() {
        List<String> sqls = TdMigrateUtil.generateSwapSqls(jdbcTemplate, TEST_TABLE, "col1", "col2");

        assertNotNull(sqls);
        assertEquals(6, sqls.size());

        System.out.println("=== Generated Swap SQLs ===");
        for (int i = 0; i < sqls.size(); i++) {
            System.out.println((i + 1) + ". " + sqls.get(i));
        }

        // 验证 1. CREATE TABLE SQL（临时表）
        String createSql = sqls.get(0);
        assertTrue(createSql.contains("CREATE TABLE"));
        assertTrue(createSql.contains("_temp_swap_"));

        // 验证 2. INSERT SQL - 核心：SELECT 时 col1 和 col2 位置交换
        String insertSql = sqls.get(1);
        assertTrue(insertSql.contains("INSERT INTO"));
        assertTrue(insertSql.contains("SELECT"));
        // 验证 SELECT 中 col2 在 col1 前面（数据交换的关键）
        String selectPart = insertSql.substring(insertSql.indexOf("SELECT"));
        int col1Pos = selectPart.indexOf("col1");
        int col2Pos = selectPart.indexOf("col2");
        assertTrue(col2Pos < col1Pos, "SELECT 中 col2 应该在 col1 前面以实现数据交换");

        // 验证 3. DROP TABLE SQL（删除原表）
        assertTrue(sqls.get(2).contains("DROP TABLE"));
        assertTrue(sqls.get(2).contains(TEST_TABLE));

        // 验证 4. CREATE TABLE SQL（重建原表）
        assertTrue(sqls.get(3).contains("CREATE TABLE"));
        assertTrue(sqls.get(3).contains(TEST_TABLE));

        // 验证 5. INSERT SQL（从临时表复制到原表）
        assertTrue(sqls.get(4).contains("INSERT INTO"));
        assertTrue(sqls.get(4).contains(TEST_TABLE));

        // 验证 6. DROP TABLE SQL（删除临时表）
        assertTrue(sqls.get(5).contains("DROP TABLE"));
        assertTrue(sqls.get(5).contains("_temp_swap_"));
    }

    @Test
    @Order(2)
    @DisplayName("测试交换列 - 验证数据确实交换")
    void testSwapColumnData() {
        // 查询交换前的数据
        String querySql = "SELECT col1, col2 FROM " + TEST_TABLE + " ORDER BY ts LIMIT 1";
        List<?> beforeData = jdbcTemplate.queryForList(querySql, Collections.emptyMap());
        System.out.println("Before swap: " + beforeData);

        Integer beforeCol1 = (Integer) ((java.util.Map<?, ?>) beforeData.get(0)).get("col1");
        Integer beforeCol2 = (Integer) ((java.util.Map<?, ?>) beforeData.get(0)).get("col2");

        // 执行交换
        TdMigrateUtil.swapColumn(jdbcTemplate, TEST_TABLE, "col1", "col2");

        // 查询交换后的数据
        List<?> afterData = jdbcTemplate.queryForList(querySql, Collections.emptyMap());
        System.out.println("After swap: " + afterData);

        Integer afterCol1 = (Integer) ((java.util.Map<?, ?>) afterData.get(0)).get("col1");
        Integer afterCol2 = (Integer) ((java.util.Map<?, ?>) afterData.get(0)).get("col2");

        // 验证数据交换成功
        assertEquals(beforeCol2, afterCol1, "col1 应该变成原来 col2 的值");
        assertEquals(beforeCol1, afterCol2, "col2 应该变成原来 col1 的值");

        System.out.println("✓ Data swap verified: col1=" + beforeCol1 + "->" + afterCol1 + ", col2=" + beforeCol2 + "->" + afterCol2);
    }

    @Test
    @Order(3)
    @DisplayName("测试不存在的列抛异常")
    void testSwapNonExistentColumn() {
        Exception exception = assertThrows(RuntimeException.class, () ->
                TdMigrateUtil.generateSwapSqls(jdbcTemplate, TEST_TABLE, "col1", "non_existent_col")
        );
        assertTrue(exception.getMessage().contains("does not exist"));
        System.out.println("✓ Exception for non-existent column: " + exception.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("测试生成重命名列SQL")
    void testGenerateRenameSqls() {
        // 需要重新创建测试表（因为前面的测试可能已删除）
        try {
            createTestTable();
        } catch (Exception ignored) {}

        List<String> sqls = TdMigrateUtil.generateRenameSqls(jdbcTemplate, TEST_TABLE, "col3", "col3_renamed");

        assertNotNull(sqls);
        assertEquals(6, sqls.size());

        System.out.println("=== Generated Rename SQLs ===");
        for (int i = 0; i < sqls.size(); i++) {
            System.out.println((i + 1) + ". " + sqls.get(i));
        }

        // 验证 1. CREATE TABLE SQL（临时表，包含新列名）
        String createSql = sqls.get(0);
        assertTrue(createSql.contains("col3_renamed"));
        assertTrue(createSql.contains("_temp_rename_"));

        // 验证 2. INSERT SQL
        assertTrue(sqls.get(1).contains("INSERT INTO"));
        assertTrue(sqls.get(1).contains("SELECT"));

        // 验证 4. 重建原表（包含新列名）
        assertTrue(sqls.get(3).contains("col3_renamed"));
        assertTrue(sqls.get(3).contains(TEST_TABLE));
    }
}
