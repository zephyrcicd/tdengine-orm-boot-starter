package com.zephyrcicd.tdengineorm.strategy;

import com.zephyrcicd.tdengineorm.cache.TagOrderCacheManager;
import com.zephyrcicd.tdengineorm.util.FieldUtil;
import com.zephyrcicd.tdengineorm.util.TdSqlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 默认表名策略 - 根据实体类上的标签字段值生成表名
 * <p>
 * 通过拼接超级表名和标签字段值来生成子表名，按照 TDengine DDL 中定义的 tag 顺序排列
 * </p>
 *
 * @author zjarlin
 */
@Component
@RequiredArgsConstructor
public class DefaultTagNameStrategy<T> implements DynamicNameStrategy<T> {
    private final TagOrderCacheManager tagOrderCacheManager;
    /**
     * 按照 DDL 定义的顺序对 tag 字段进行排序
     */
    private List<Pair<String, String>> sortTagsByDdlOrder(
            List<Pair<String, String>> tagPairs,
            List<String> ddlTagOrder) {

        if (ddlTagOrder == null || ddlTagOrder.isEmpty()) {
            // 如果查询失败或没有顺序信息，返回原始顺序
            return tagPairs;
        }

        // 创建字段名到下划线格式的映射
        Map<String, Pair<String, String>> tagMap = tagPairs.stream()
                .collect(Collectors.toMap(
                        pair -> FieldUtil.toUnderlineCase(pair.getFirst()),
                        pair -> pair,
                        (a, b) -> a
                ));

        // 按照 DDL 顺序重新排列
        return ddlTagOrder.stream()
                .map(tagMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public String getTableName(T entity) {
        String superTableName = TdSqlUtil.getTbName(entity.getClass());
        List<Pair<String, String>> allTagFieldsPair = TdSqlUtil.getAllTagFieldsPairOrdered(entity);

        // 如果没有配置 TagOrderCacheManager，使用原始顺序
        if (tagOrderCacheManager == null) {
            return Stream.concat(Stream.of(superTableName),
                            allTagFieldsPair.stream().map(Pair::getSecond))
                    .collect(Collectors.joining("_"));
        }

        // 获取 TDengine 中定义的 tag 顺序
        List<String> tagOrder = tagOrderCacheManager.getTagOrder(superTableName);

        // 按照 DDL 定义顺序排序 tag 字段
        List<Pair<String, String>> sortedTags = sortTagsByDdlOrder(allTagFieldsPair, tagOrder);

        return Stream.concat(Stream.of(superTableName),
                        sortedTags.stream().map(Pair::getSecond))
                .collect(Collectors.joining("_"));
    }
}
