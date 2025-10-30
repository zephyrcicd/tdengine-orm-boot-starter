package com.zephyrcicd.tdengineorm.strategy;

import com.zephyrcicd.tdengineorm.util.TdSqlUtil;
import org.springframework.data.util.Pair;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 默认表名策略 - 根据实体类上的标签字段值生成表名
 * <p>
 * 通过拼接超级表名和标签字段值来生成子表名
 * </p>
 *
 * @author zjarlin
 */
public class DefaultTagNameStrategy<T> implements DynamicNameStrategy<T> {

    @Override
    public String getTableName(T entity) {
        String superTableName = TdSqlUtil.getTbName(entity.getClass());
        Set<Pair<String, String>> allTagFieldsPair = TdSqlUtil.getAllTagFieldsPair(entity);

        return Stream.concat(Stream.of(superTableName),
                           allTagFieldsPair.stream().map(Pair::getSecond))
                     .collect(Collectors.joining("_"));
    }

}
