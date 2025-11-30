package com.zephyrcicd.tdengineorm.cache;

import com.zephyrcicd.tdengineorm.template.TdTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    private final TdTemplate tdTemplate;
    private final Map<String, List<String>> tagOrderCache = new ConcurrentHashMap<>();

    public TagOrderCacheManager(TdTemplate tdTemplate) {
        this.tdTemplate = tdTemplate;
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
     * 查询超级表的 TAG 字段顺序
     * <p>
     * 使用 DESCRIBE 命令直接查询表结构，无需指定数据库名（连接时已指定）
     * </p>
     */
    private List<String> queryTagOrderFromDatabase(String superTableName) {
        try {
            NamedParameterJdbcTemplate npJdbc = tdTemplate.getNamedParameterJdbcTemplate();

            // DESCRIBE 命令可以直接使用表名，连接时已指定数据库
            String sql = "DESCRIBE `" + superTableName + "`";

            List<String> tagList = npJdbc.query(
                    sql,
                    Collections.emptyMap(),
                    (rs, rowNum) -> {
                        String note = rs.getString("Note");
                        if ("TAG".equalsIgnoreCase(note)) {
                            return rs.getString("Field");
                        }
                        return null;
                    }
            ).stream().filter(Objects::nonNull).collect(Collectors.toList());

            if (log.isDebugEnabled()) {
                log.debug("Loaded tag order for stable '{}': {}", superTableName, tagList);
            }

            return tagList;

        } catch (Exception e) {
            log.warn("查询 TDengine TAG 列失败（stable={}）：{}", superTableName, e.getMessage());
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
