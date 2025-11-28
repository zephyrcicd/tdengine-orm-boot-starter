package com.zephyrcicd.tdengineorm.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tag 顺序缓存管理器
 * <p>
 * 缓存所有超级表的 tag 定义顺序，避免重复查询数据库
 * </p>
 *
 * @author zjarlin
 */
@Slf4j
public class TagOrderCacheManager {

    private final JdbcTemplate jdbcTemplate;
    private final String databaseName;
    private final Map<String, List<String>> tagOrderCache = new ConcurrentHashMap<>();

    public TagOrderCacheManager(JdbcTemplate jdbcTemplate, String databaseName) {
        this.jdbcTemplate = jdbcTemplate;
        this.databaseName = databaseName;
    }

    /**
     * 获取指定超级表的 tag 定义顺序
     *
     * @param superTableName 超级表名
     * @return tag 列名列表，按 DDL 定义顺序排序
     */
    public List<String> getTagOrder(String superTableName) {
        return tagOrderCache.computeIfAbsent(superTableName, this::queryTagOrderFromDatabase);
    }

    /**
     * 从 TDengine 查询超级表的 tag 定义顺序
     * 使用 DESCRIBE 命令获取表结构，然后提取 TAG 字段
     */
    private List<String> queryTagOrderFromDatabase(String superTableName) {
        try {
            String sql = String.format("DESCRIBE `%s`.`%s`", databaseName, superTableName);

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            // 过滤出 Note 列为 "TAG" 的行，并提取 Field 列
            List<String> tagOrder = rows.stream()
                    .filter(row -> "TAG".equals(row.get("Note")))
                    .map(row -> (String) row.get("Field"))
                    .collect(java.util.stream.Collectors.toList());

            if (log.isDebugEnabled()) {
                log.debug("Loaded tag order for table {}: {}", superTableName, tagOrder);
            }
            return tagOrder;
        } catch (Exception e) {
            log.warn("Failed to query tag order from TDengine for table: {}, error: {}",
                    superTableName, e.getMessage());
            // 查询失败时返回空列表
            return Collections.emptyList();
        }
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        tagOrderCache.clear();
        log.info("Tag order cache cleared");
    }

    /**
     * 清空指定表的缓存
     */
    public void clearCache(String superTableName) {
        tagOrderCache.remove(superTableName);
        if (log.isDebugEnabled()) {
            log.debug("Tag order cache cleared for table: {}", superTableName);
        }
    }

    /**
     * 预加载指定表的 tag 顺序
     */
    public void preloadTagOrder(String superTableName) {
        getTagOrder(superTableName);
    }

    /**
     * 预加载多个表的 tag 顺序
     */
    public void preloadTagOrders(List<String> superTableNames) {
        superTableNames.forEach(this::preloadTagOrder);
    }
}
