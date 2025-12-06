package com.zephyrcicd.tdengineorm.util;

import com.zephyrcicd.tdengineorm.constant.SqlConstant;
import com.zephyrcicd.tdengineorm.constant.TdSqlConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TDengine 表迁移工具类
 * 提供表结构修改、列交换等迁移功能
 *
 * @author Zephyr
 */
@Slf4j
public class TdMigrateUtil {

    /**
     * 交换表中两列的数据位置
     * 通过创建新表、插入数据、删除旧表、重命名的方式实现列交换
     *
     * @param jdbcTemplate NamedParameterJdbcTemplate 实例
     * @param tableName    表名
     * @param col1         第一列名
     * @param col2         第二列名
     * @throws RuntimeException 如果交换失败
     */
    public static void swapColumn(NamedParameterJdbcTemplate jdbcTemplate, String tableName, String col1, String col2) {
        List<String> sqls = generateSwapSqls(jdbcTemplate, tableName, col1, col2);
        try {
            for (String sql : sqls) {
                jdbcTemplate.update(sql, Collections.emptyMap());
            }
            log.info("Successfully swapped columns '{}' and '{}' in table '{}'", col1, col2, tableName);
        } catch (Exception e) {
            log.error("Failed to swap columns '{}' and '{}' in table '{}': {}", col1, col2, tableName, e.getMessage());
            throw new RuntimeException("Column swap failed: " + e.getMessage(), e);
        }
    }

    /**
     * 重命名表中的列
     * 通过创建新表、插入数据、删除旧表、重命名的方式实现列重命名
     *
     * @param jdbcTemplate NamedParameterJdbcTemplate 实例
     * @param tableName    表名
     * @param oldColumn    旧列名
     * @param newColumn    新列名
     * @throws RuntimeException 如果重命名失败
     */
    public static void renameColumn(NamedParameterJdbcTemplate jdbcTemplate, String tableName, String oldColumn, String newColumn) {
        List<String> sqls = generateRenameSqls(jdbcTemplate, tableName, oldColumn, newColumn);
        try {
            for (String sql : sqls) {
                jdbcTemplate.update(sql, Collections.emptyMap());
            }
            log.info("Successfully renamed column '{}' to '{}' in table '{}'", oldColumn, newColumn, tableName);
        } catch (Exception e) {
            log.error("Failed to rename column '{}' to '{}' in table '{}': {}", oldColumn, newColumn, tableName, e.getMessage());
            throw new RuntimeException("Column rename failed: " + e.getMessage(), e);
        }
    }

    /**
     * 生成交换列的 SQL 语句列表（用于测试或预览）
     *
     * @param jdbcTemplate NamedParameterJdbcTemplate 实例
     * @param tableName    表名
     * @param col1         第一列名
     * @param col2         第二列名
     * @return SQL 语句列表
     */
    public static List<String> generateSwapSqls(NamedParameterJdbcTemplate jdbcTemplate, String tableName, String col1, String col2) {
        // 1. 获取表结构信息
        TableStructure tableStructure = getTableStructure(jdbcTemplate, tableName);

        // 2. 验证列存在性
        if (!tableStructure.columns.containsKey(col1)) {
            throw new RuntimeException("Column '" + col1 + "' does not exist in table '" + tableName + "'");
        }
        if (!tableStructure.columns.containsKey(col2)) {
            throw new RuntimeException("Column '" + col2 + "' does not exist in table '" + tableName + "'");
        }

        // 3. 检查是否为超级表
        if (tableStructure.isStable) {
            throw new RuntimeException("超级表列交换暂不支持自动迁移，请参考测试输出中的手动迁移流程。TDengine 超级表结构变更需要手动处理每个子表。");
        }

        // 4. 生成新表结构（保持原结构）
        String newTableName = tableName + "_temp_swap_" + System.currentTimeMillis();
        String createNewTableSql = buildCreateTableSql(newTableName, tableStructure);

        // 5. 生成插入数据的SQL（交换列顺序）
        String insertDataSql = buildInsertDataSql(newTableName, tableName, tableStructure, col1, col2);

        // 6. 生成迁移SQL列表（TDengine 普通表不支持 RENAME，需要：临时表->删原表->重建原表->复制数据->删临时表）
        String finalTableSql = buildCreateTableSql(tableName + "_final", tableStructure);
        String finalInsertSql = buildFinalInsertSql(tableName, newTableName, tableStructure, col1, col2);

        List<String> sqls = new ArrayList<>();
        sqls.add(createNewTableSql);                                    // 1. 创建临时表（交换数据）
        sqls.add(insertDataSql);                                        // 2. 复制数据到临时表（SELECT 时交换 col1/col2）
        sqls.add("DROP TABLE " + tableName);                            // 3. 删除原表
        sqls.add(finalTableSql.replace("_final", ""));                  // 4. 重建原表（结构不变）
        sqls.add(finalInsertSql);                                       // 5. 从临时表复制到原表
        sqls.add("DROP TABLE " + newTableName);                         // 6. 删除临时表

        return sqls;
    }

    /**
     * 构建从临时表复制到最终表的SQL
     */
    private static String buildFinalInsertSql(String targetTable, String sourceTable, TableStructure structure, String col1, String col2) {
        List<String> columnOrder = getOrderedColumns(structure);
        return "INSERT INTO " + targetTable + " SELECT " + String.join(", ", columnOrder) + " FROM " + sourceTable;
    }

    /**
     * 生成重命名列的 SQL 语句列表（用于测试或预览）
     *
     * @param jdbcTemplate NamedParameterJdbcTemplate 实例
     * @param tableName    表名
     * @param oldColumn    旧列名
     * @param newColumn    新列名
     * @return SQL 语句列表
     */
    public static List<String> generateRenameSqls(NamedParameterJdbcTemplate jdbcTemplate, String tableName, String oldColumn, String newColumn) {
        // 1. 获取表结构信息
        TableStructure tableStructure = getTableStructure(jdbcTemplate, tableName);

        // 2. 验证列存在性和新列名不冲突
        if (!tableStructure.columns.containsKey(oldColumn)) {
            throw new RuntimeException("Column '" + oldColumn + "' does not exist in table '" + tableName + "'");
        }
        if (tableStructure.columns.containsKey(newColumn)) {
            throw new RuntimeException("Column '" + newColumn + "' already exists in table '" + tableName + "'");
        }
        if (tableStructure.tags.containsKey(newColumn)) {
            throw new RuntimeException("Column '" + newColumn + "' conflicts with existing TAG in table '" + tableName + "'");
        }

        // 3. 生成新表结构（重命名列）
        String newTableName = tableName + "_temp_rename_" + System.currentTimeMillis();
        String createNewTableSql = buildCreateTableSqlForRename(newTableName, tableStructure, oldColumn, newColumn);

        // 4. 生成插入数据的SQL（重命名列）
        String insertDataSql = buildInsertDataSqlForRename(newTableName, tableName, tableStructure, oldColumn, newColumn);

        // 5. 生成迁移SQL列表（TDengine 普通表不支持 RENAME）
        String finalTableSql = buildCreateTableSqlForRename(tableName + "_final", tableStructure, oldColumn, newColumn);
        String finalInsertSql = buildFinalInsertSqlForRename(tableName, newTableName, tableStructure, oldColumn, newColumn);

        List<String> sqls = new ArrayList<>();
        sqls.add(createNewTableSql);                                    // 1. 创建临时表（新列名）
        sqls.add(insertDataSql);                                        // 2. 复制数据到临时表
        sqls.add("DROP TABLE " + tableName);                            // 3. 删除原表
        sqls.add(finalTableSql.replace("_final", ""));                  // 4. 重建原表（新列名）
        sqls.add(finalInsertSql);                                       // 5. 从临时表复制到原表
        sqls.add("DROP TABLE " + newTableName);                         // 6. 删除临时表

        return sqls;
    }

    private static String buildFinalInsertSqlForRename(String targetTable, String sourceTable, TableStructure structure, String oldColumn, String newColumn) {
        List<String> targetColumns = getOrderedColumns(structure).stream()
                .map(col -> col.equals(oldColumn) ? newColumn : col)
                .collect(Collectors.toList());
        return "INSERT INTO " + targetTable + " SELECT " + String.join(", ", targetColumns) + " FROM " + sourceTable;
    }

    /**
     * 获取表结构信息
     */
    private static TableStructure getTableStructure(NamedParameterJdbcTemplate jdbcTemplate, String tableName) {
        // DESCRIBE table_name 返回：Field, Type, Length, Note
        String describeSql = "DESCRIBE " + tableName;
        List<Map<String, Object>> describeResult = jdbcTemplate.queryForList(describeSql, Collections.emptyMap());

        TableStructure structure = new TableStructure();
        structure.isStable = false;

        for (Map<String, Object> row : describeResult) {
            String field = (String) row.get("Field");
            String type = (String) row.get("Type");
            Object lengthObj = row.get("Length");
            String note = (String) row.get("Note");

            // 构建完整的类型字符串（只有 NCHAR/VARCHAR/BINARY/VARBINARY/GEOMETRY 需要长度）
            String fullType = buildFullType(type, lengthObj);

            if ("TAG".equals(note)) {
                structure.tags.put(field, fullType);
                structure.isStable = true;
            } else {
                structure.columns.put(field, fullType);
                if ("TIMESTAMP".equals(type) && structure.tsColumn == null) {
                    structure.tsColumn = field;
                }
            }
        }

        return structure;
    }

    private static final Set<String> TYPES_WITH_LENGTH = new HashSet<>(Arrays.asList("NCHAR", "VARCHAR", "BINARY", "VARBINARY", "GEOMETRY"));

    private static String buildFullType(String type, Object lengthObj) {
        String upperType = type.toUpperCase();
        if (TYPES_WITH_LENGTH.contains(upperType) && lengthObj != null) {
            int length = (Integer) lengthObj;
            if (length > 0) {
                return type + "(" + length + ")";
            }
        }
        return type;
    }

    /**
     * 构建创建新表的SQL（保持原表结构不变）
     */
    private static String buildCreateTableSql(String newTableName, TableStructure structure) {
        StringBuilder sql = new StringBuilder(structure.isStable ? TdSqlConstant.CREATE_STABLE_IF_NOT_EXIST : SqlConstant.CREATE_TABLE + SqlConstant.IF_NOT_EXISTS);
        sql.append(newTableName).append(" (");

        List<String> columnOrder = getOrderedColumns(structure);
        for (String column : columnOrder) {
            sql.append(column).append(" ").append(structure.columns.get(column)).append(", ");
        }

        sql.setLength(sql.length() - 2);
        sql.append(")");

        if (structure.isStable && !structure.tags.isEmpty()) {
            sql.append(SqlConstant.BLANK).append(TdSqlConstant.TAGS).append(" (");
            for (Map.Entry<String, String> tag : structure.tags.entrySet()) {
                sql.append(tag.getKey()).append(" ").append(tag.getValue()).append(", ");
            }
            sql.setLength(sql.length() - 2);
            sql.append(")");
        }

        return sql.toString();
    }

    /**
     * 获取有序的列列表（ts 列在第一位）
     */
    private static List<String> getOrderedColumns(TableStructure structure) {
        List<String> columnOrder = new ArrayList<>(structure.columns.keySet());
        if (structure.tsColumn != null && columnOrder.contains(structure.tsColumn)) {
            columnOrder.remove(structure.tsColumn);
            columnOrder.add(0, structure.tsColumn);
        }
        return columnOrder;
    }

    /**
     * 构建插入数据的SQL（交换 col1 和 col2 的数据）
     * 新表结构不变，但 SELECT 时交换 col1 和 col2 的位置，使数据交换
     */
    private static String buildInsertDataSql(String newTableName, String originalTableName, TableStructure structure, String col1, String col2) {
        if (structure.isStable) {
            return "-- 注意：超级表数据迁移需要手动处理每个子表，请参考测试输出中的完整迁移流程";
        }

        List<String> columnOrder = getOrderedColumns(structure);

        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(newTableName).append(" (");
        sql.append(String.join(", ", columnOrder));
        sql.append(") SELECT ");

        // SELECT 时交换 col1 和 col2 的位置以实现数据交换
        List<String> selectColumns = columnOrder.stream()
                .map(col -> {
                    if (col.equals(col1)) return col2;
                    if (col.equals(col2)) return col1;
                    return col;
                })
                .collect(Collectors.toList());

        sql.append(String.join(", ", selectColumns));
        sql.append(" FROM ").append(originalTableName);

        return sql.toString();
    }

    /**
     * 构建创建新表的SQL（用于重命名列）
     */
    private static String buildCreateTableSqlForRename(String newTableName, TableStructure structure, String oldColumn, String newColumn) {
        StringBuilder sql = new StringBuilder(structure.isStable ? TdSqlConstant.CREATE_STABLE_IF_NOT_EXIST : SqlConstant.CREATE_TABLE + SqlConstant.IF_NOT_EXISTS);
        sql.append(newTableName).append(" (");

        List<String> columnOrder = getOrderedColumns(structure);
        for (String column : columnOrder) {
            String columnName = column.equals(oldColumn) ? newColumn : column;
            sql.append(columnName).append(" ").append(structure.columns.get(column)).append(", ");
        }

        sql.setLength(sql.length() - 2);
        sql.append(")");

        if (structure.isStable && !structure.tags.isEmpty()) {
            sql.append(SqlConstant.BLANK).append(TdSqlConstant.TAGS).append(" (");
            for (Map.Entry<String, String> tag : structure.tags.entrySet()) {
                sql.append(tag.getKey()).append(" ").append(tag.getValue()).append(", ");
            }
            sql.setLength(sql.length() - 2);
            sql.append(")");
        }

        return sql.toString();
    }

    /**
     * 构建插入数据的SQL（用于重命名列）
     */
    private static String buildInsertDataSqlForRename(String newTableName, String originalTableName, TableStructure structure, String oldColumn, String newColumn) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(newTableName).append(" SELECT ");

        // 构建 SELECT 列列表，将 oldColumn 映射到新列名
        for (String column : structure.columns.keySet()) {
            sql.append(column).append(", ");
        }

        sql.setLength(sql.length() - 2);
        sql.append(" FROM ").append(originalTableName);

        return sql.toString();
    }

    /**
     * 执行迁移操作
     */
    private static void executeMigration(NamedParameterJdbcTemplate jdbcTemplate, String originalTableName,
                                       String newTableName, String createSql, String insertSql) {
        try {
            // 1. 创建新表
            jdbcTemplate.update(createSql, Collections.emptyMap());

            // 2. 插入数据
            jdbcTemplate.update(insertSql, Collections.emptyMap());

            // 3. 删除旧表
            String dropSql = "DROP TABLE " + originalTableName;
            jdbcTemplate.update(dropSql, Collections.emptyMap());

            // 4. 重命名新表
            String renameSql = "ALTER TABLE " + newTableName + " RENAME TO " + originalTableName;
            jdbcTemplate.update(renameSql, Collections.emptyMap());

        } catch (Exception e) {
            // 如果迁移失败，尝试清理临时表
            try {
                jdbcTemplate.update("DROP TABLE IF EXISTS " + newTableName, Collections.emptyMap());
            } catch (Exception cleanupEx) {
                log.warn("Failed to cleanup temporary table '{}': {}", newTableName, cleanupEx.getMessage());
            }
            throw e;
        }
    }

    /**
     * 表结构信息内部类
     */
    private static class TableStructure {
        boolean isStable;
        String tsColumn; // 时间戳列名
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        LinkedHashMap<String, String> tags = new LinkedHashMap<>();
    }
}
