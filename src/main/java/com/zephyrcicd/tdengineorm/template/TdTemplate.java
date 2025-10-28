package com.zephyrcicd.tdengineorm.template;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.zephyrcicd.tdengineorm.annotation.TdTag;
import com.zephyrcicd.tdengineorm.constant.SqlConstant;
import com.zephyrcicd.tdengineorm.dto.Page;
import com.zephyrcicd.tdengineorm.enums.TdLogLevelEnum;
import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.exception.TdOrmExceptionCode;
import com.zephyrcicd.tdengineorm.strategy.DefaultDynamicNameStrategy;
import com.zephyrcicd.tdengineorm.strategy.DynamicNameStrategy;
import com.zephyrcicd.tdengineorm.util.ClassUtil;
import com.zephyrcicd.tdengineorm.util.JdbcTemplatePlus;
import com.zephyrcicd.tdengineorm.util.TdOrmUtil;
import com.zephyrcicd.tdengineorm.util.TdSqlUtil;
import com.zephyrcicd.tdengineorm.wrapper.AbstractTdQueryWrapper;
import com.zephyrcicd.tdengineorm.wrapper.TdQueryWrapper;
import com.zephyrcicd.tdengineorm.wrapper.TdWrappers;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TDengine 数据访问模板类
 * 提供对 TDengine 数据库的 CRUD 操作，支持动态表名、批量插入等功能
 *
 * @author Zephyr
 */
@Slf4j
@Setter
@RequiredArgsConstructor
public class TdTemplate {

    private final JdbcTemplatePlus jdbcTemplatePlus;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    public static final Integer DEFAULT_BATCH_SIZE = 1000;

    /**
     * 按ts字段倒叙, 获取最新的一条数据
     *
     * @param clazz clazz
     * @return {@link T }
     */
    public <T> T getLastOneByTs(Class<T> clazz) {
        TdQueryWrapper<T> wrapper = TdWrappers.queryWrapper(clazz)
                .selectAll()
                .orderByDesc("ts")
                .limit(1);
        return getOne(wrapper);
    }


    /**
     * 查询单条数据
     *
     * @param wrapper 包装器
     * @return {@link T }
     */
    public <T> T getOne(AbstractTdQueryWrapper<T> wrapper) {
        return getOne(wrapper, wrapper.getEntityClass());
    }


    /**
     * 查询单条数据, 可以响应和实体类不一样的对象
     *
     * @param wrapper     包装器
     * @param resultClass 结果类
     * @return {@link R }
     */
    public <T, R> R getOne(AbstractTdQueryWrapper<T> wrapper, Class<R> resultClass) {
        String sql = wrapper.getSql();
        Map<String, Object> paramsMap = wrapper.getParamsMap();
        return getOneWithTdLog(resultClass, sql, paramsMap);
    }


    /**
     * 列表
     *
     * @param wrapper 包装器
     * @return {@link List }<{@link T }>
     */
    public <T> List<T> list(AbstractTdQueryWrapper<T> wrapper) {
        return list(wrapper, wrapper.getEntityClass());
    }


    public <T, R> List<R> list(AbstractTdQueryWrapper<T> wrapper, Class<R> resultClass) {
        return listWithTdLog(wrapper.getSql(), wrapper.getParamsMap(), resultClass);
    }

    public <T> Page<T> page(long pageNo, long pageSize, TdQueryWrapper<T> wrapper) {
        return page(pageNo, pageSize, wrapper, wrapper.getEntityClass());
    }

    public <T, R> Page<R> page(long pageNo, long pageSize, TdQueryWrapper<T> wrapper, Class<R> resultClass) {
        String countSql = "select count(*) from (" + wrapper.getSql() + ") t";
        Long count = namedParameterJdbcTemplate.queryForObject(countSql, wrapper.getParamsMap(), Long.class);
        Page<R> page = Page.<R>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(count).build();
        if (count != null && count > 0) {
            List<R> list = listWithTdLog(wrapper.limit(pageNo, pageSize).getSql(), wrapper.getParamsMap(), resultClass);
            page.setDataList(list);
        }
        return page;
    }

    /**
     * 插入单条数据
     *
     * @param object 对象
     * @return int
     */
    public <T> int insert(T object) {
        return insert(object, new DefaultDynamicNameStrategy<>());
    }

    /**
     * 向指定子表插入数据, 无需指定TAG相关字段
     *
     * @param object              对象
     * @param dynamicNameStrategy 动态命名策略
     * @return int
     */
    public <T> int insert(T object, DynamicNameStrategy<T> dynamicNameStrategy) {
        // 获取超级表表名&所有Field
        List<Field> noTagFieldList = TdSqlUtil.getNoTagFieldList(object.getClass());
        if (CollectionUtils.isEmpty(noTagFieldList)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_COMM_FIELD);
        }

        Map<String, Object> paramsMap = new HashMap<>(noTagFieldList.size());
        String tbName = TdSqlUtil.getTbName(object.getClass());
        if (null != dynamicNameStrategy) {
            tbName = dynamicNameStrategy.dynamicTableName(object, tbName);
        }
        String sql = SqlConstant.INSERT_INTO + tbName + TdSqlUtil.joinColumnNamesAndValuesSql(object, noTagFieldList, paramsMap);
        return updateWithTdLog(sql, paramsMap);
    }


    public <T> int insertUsing(T object) {
        return insertUsing(object, new DefaultDynamicNameStrategy<>());
    }

    public <T> int insertUsing(T object, DynamicNameStrategy<T> dynamicTbNameStrategy) {
        // 获取表表名&所有Field
        Pair<String, List<Field>> tbNameAndFieldsPair = TdSqlUtil.getTbNameAndFieldListPair(object.getClass());
        // 获取SQL&参数值
        Pair<String, Map<String, Object>> finalSqlAndParamsMapPair = TdSqlUtil.getFinalInsertUsingSql(object, tbNameAndFieldsPair.getValue(), tbNameAndFieldsPair.getKey(), dynamicTbNameStrategy);

        String finalSql = finalSqlAndParamsMapPair.getKey();
        Map<String, Object> paramsMap = finalSqlAndParamsMapPair.getValue();

        return updateWithTdLog(finalSql, paramsMap);
    }


    public <T> int[] batchInsert(Class<T> clazz, List<T> entityList, DynamicNameStrategy<T> dynamicTbNameStrategy) {
        return batchInsert(clazz, entityList, DEFAULT_BATCH_SIZE, dynamicTbNameStrategy);
    }

    public <T> int[] batchInsert(Class<T> clazz, List<T> entityList, int pageSize, DynamicNameStrategy<T> dynamicTbNameStrategy) {
        // 不使用USING语法时, 不能指定TAG字段的值
        List<Field> fieldList = ClassUtil.getAllFields(clazz, field -> !field.isAnnotationPresent(TdTag.class));

        // 按照命名策略对数据进行分组,相同表名的数据放在一起
        Map<String, List<T>> tableGroupMap = new HashMap<>();
        String defaultTbName = TdSqlUtil.getTbName(clazz);

        for (T entity : entityList) {
            String tbName = dynamicTbNameStrategy.dynamicTableName(entity, defaultTbName);
            tableGroupMap.computeIfAbsent(tbName, k -> new ArrayList<>()).add(entity);
        }

        // 对每个分组分别进行批量插入
        List<Integer> resultList = new ArrayList<>();
        for (Map.Entry<String, List<T>> entry : tableGroupMap.entrySet()) {
            String tbName = entry.getKey();
            List<T> groupEntityList = entry.getValue();

            // 以防数据量过大, 分批进行插入
            List<List<T>> partition = ListUtil.partition(groupEntityList, pageSize);

            for (List<T> list : partition) {
                Map<String, Object> paramsMap = new HashMap<>(list.size());
                StringBuilder insertIntoSql = TdSqlUtil.getInsertIntoSqlPrefix(tbName, fieldList);
                StringBuilder finalSql = new StringBuilder(insertIntoSql);
                joinInsetSqlSuffix(list, finalSql, paramsMap);
                int singleResult = namedParameterJdbcTemplate.update(finalSql.toString(), paramsMap);
                if (log.isDebugEnabled()) {
                    log.debug("{} ===== execute result ====>{}", finalSql, singleResult);
                }
                resultList.add(singleResult);
            }
        }

        return resultList.stream().mapToInt(Integer::intValue).toArray();
    }

    public <T> int[] batchInsertUsing(Class<T> clazz, List<T> entityList) {
        return batchInsertUsing(clazz, entityList, DEFAULT_BATCH_SIZE, new DefaultDynamicNameStrategy<>());
    }

    public <T> int[] batchInsertUsing(Class<T> clazz, List<T> entityList, DynamicNameStrategy<T> dynamicTbNameStrategy) {
        return batchInsertUsing(clazz, entityList, DEFAULT_BATCH_SIZE, dynamicTbNameStrategy);
    }

    public <T> int[] batchInsertUsing(Class<T> clazz, List<T> entityList, int pageSize, DynamicNameStrategy dynamicTbNameStrategy) {
        // 获取超级表表名&所有字段
        Pair<String, List<Field>> tbNameAndFieldsPair = TdSqlUtil.getTbNameAndFieldListPair(clazz);

        // 目前仅支持同子表的数据批量插入, 所以随意取一个对象的tag的值都是一样的
        List<List<T>> partition = ListUtil.partition(entityList, pageSize);
        T t = partition.get(0).get(0);

        List<Field> tagFields = ClassUtil.getAllFields(t.getClass()).stream()
                .filter(field -> field.isAnnotationPresent(TdTag.class))
                .collect(Collectors.toList());

        Map<String, Object> tagValueMap = TdSqlUtil.getFiledValueMap(tagFields, t);
        int[] result = new int[partition.size()];
        for (int i = 0; i < partition.size(); i++) {
            List<T> list = partition.get(i);
            Map<String, Object> paramsMap = new HashMap<>(list.size());
            paramsMap.putAll(tagValueMap);
            StringBuilder finalSql = new StringBuilder(TdSqlUtil.getInsertUsingSqlPrefix(t, tbNameAndFieldsPair.getKey(),
                    tbNameAndFieldsPair.getValue(), dynamicTbNameStrategy, paramsMap));
            joinInsetSqlSuffix(list, finalSql, paramsMap);
            int singleResult = namedParameterJdbcTemplate.update(finalSql.toString(), paramsMap);
            if (log.isDebugEnabled()) {
                log.debug("{} =====execute result====>{}", finalSql, result);
            }
            result[i] = singleResult;
        }
        return result;
    }

    public <T> int deleteByTs(Class<T> clazz, Timestamp ts) {
        String tbName = TdSqlUtil.getTbName(clazz);
        String sql = "DELETE FROM " + tbName + " WHERE ts = :ts";
        Map<String, Object> paramsMap = new HashMap<>(1);
        paramsMap.put("ts", ts);
        return namedParameterJdbcTemplate.update(sql, paramsMap);
    }

    public <T> int batchDeleteByTs(Class<T> clazz, List<Timestamp> tsList) {
        String tbName = TdSqlUtil.getTbName(clazz);
        String sql = "DELETE FROM " + tbName + " WHERE ts IN (:tsList)";
        Map<String, Object> paramsMap = new HashMap<>(1);
        paramsMap.put("tsList", tsList);
        return namedParameterJdbcTemplate.update(sql, paramsMap);
    }


    private static void tdLog(String sql, Map<String, Object> paramsMap) {
        TdLogLevelEnum tdLogLevelEnum = TdOrmUtil.getLogLevel();
        if (null != tdLogLevelEnum) {
            String logFormat = "【TDengineMapperLog】 \n【SQL】 : {} \n【Params】: {}";
            switch (tdLogLevelEnum) {
                case DEBUG:
                    if (log.isDebugEnabled()) {
                        log.debug(logFormat, sql, JSONUtil.toJsonStr(paramsMap));
                    }
                    break;
                case INFO:
                    log.info(logFormat, sql, JSONUtil.toJsonStr(paramsMap));
                    break;
                default:
            }
        }
    }

    private int updateWithTdLog(String finalSql, Map<String, Object> paramsMap) {
        tdLog(finalSql, paramsMap);
        return namedParameterJdbcTemplate.update(finalSql, paramsMap);
    }

    private <R> List<R> listWithTdLog(String sql, Map<String, Object> paramsMap, Class<R> resultClass) {
        tdLog(sql, paramsMap);
        return jdbcTemplatePlus.list(sql, paramsMap, resultClass);
    }

    private <R> R getOneWithTdLog(Class<R> resultClass, String sql, Map<String, Object> paramsMap) {
        tdLog(sql, paramsMap);
        return jdbcTemplatePlus.get(sql, paramsMap, resultClass);
    }

    private static <T> void joinInsetSqlSuffix(List<T> list, StringBuilder finalSql, Map<String, Object> paramsMap) {
        for (int i = 0; i < list.size(); i++) {
            T entity = list.get(i);
            List<Field> fields = ClassUtil.getAllFields(entity.getClass(), field -> !field.isAnnotationPresent(TdTag.class));
            Pair<String, Map<String, Object>> insertSqlSuffix = TdSqlUtil.getInsertSqlSuffix(entity, fields, i);
            finalSql.append(insertSqlSuffix.getKey());
            paramsMap.putAll(insertSqlSuffix.getValue());
        }
    }

}
