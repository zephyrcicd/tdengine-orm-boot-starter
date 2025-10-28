package com.zephyrcicd.tdengineorm.util;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 使用NamedParameterJdbcTemplate完成对实体类的相关操作封装
 *
 * @author Zephyr
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcTemplatePlus {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public <T> List<T> list(String sql, Map<String, Object> params, Class<T> clazz) {
        return namedParameterJdbcTemplate.query(sql, params, BeanPropertyRowMapper.newInstance(clazz));
    }


    public <T> T get(String sql, Map<String, Object> params, Class<T> clazz) {
        List<T> list = list(sql, params, clazz);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 批量保存列表数据
     *
     * @param entityClass   实体类Class对象
     * @param defaultTbName 表名称（若传 null，则使用@TableName注解内的表名）
     * @param entityList    实体类列表
     * @param <T>           实体类泛型
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> void saveBatch(Class<T> entityClass, String defaultTbName, List<T> entityList) {
        List<Field> allFields = ClassUtil.getAllFields(entityClass);
        StringBuilder insertIntoSql = TdSqlUtil.getInsertIntoSql(CollectionUtils.isEmpty(entityList), defaultTbName, entityClass, allFields, false);
        if (StrUtil.isBlank(insertIntoSql)) {
            log.warn("JdbcTemplateUtil#saveBatch fail, build sql fail, entityList or fieldList is empty! class:{}", entityClass.getSimpleName());
            return;
        }

        // 将列表数据进行分批处理，默认是 500 一批次
        doBatchUpdate(entityList, insertIntoSql.toString(), 500);
    }

    /**
     * 批量保存或更新列表数据
     *
     * @param entityClass   实体类Class对象
     * @param defaultTbName 表名称（若传 null，则使用@TableName注解内的表名）
     * @param entityList    实体类列表
     * @param <T>           实体类泛型
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> void saveOrUpdateBatch(Class<T> entityClass, String defaultTbName, List<T> entityList) {
        List<Field> fields = ClassUtil.getAllFields(entityClass);

        StringBuilder initSql = TdSqlUtil.getInsertIntoSql(CollectionUtils.isEmpty(entityList), defaultTbName, entityClass, fields, true);
        if (StrUtil.isBlank(initSql)) {
            log.warn("JdbcTemplateUtil#saveOrUpdateBatch fail, build sql fail, entityList or fieldList is empty! class:{}", entityClass.getSimpleName());
            return;
        }

        // 将列表数据进行分批处理，默认是 500 一批次
        doBatchUpdate(entityList, initSql.toString(), 300);
    }

    /**
     * 批量保存
     *
     * @param tbName  结核病名字
     * @param mapList 地图列表
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> void saveBatch(String tbName, List<Map<String, Object>> mapList) {
        Map<String, Object> standMap = getStandMap(mapList);
        if (standMap == null) {
            log.warn("JdbcTemplateUtil#saveBatch fail, map is empty! ");
            return;
        }
        StringBuilder insertIntoSql = TdSqlUtil.getInsertIntoSql(tbName, standMap);

        // 将列表数据进行分批处理，默认是 500 一批次
        doBatchUpdateByMap(mapList, insertIntoSql.toString(), 500, standMap);
    }


    /**
     * 保存或更新批处理
     *
     * @param tbName    结核病名字
     * @param mapList   地图列表
     * @param idKeyName Id键名
     */
    @Transactional(rollbackFor = Exception.class)
    public <T> void saveOrUpdateBatch(String tbName, List<Map<String, Object>> mapList, String idKeyName) {
        Map<String, Object> standMap = getStandMap(mapList);
        if (standMap == null) {
            log.warn("JdbcTemplateUtil#saveOrUpdateBatch fail, map is empty! ");
            return;
        }

        StringBuilder insertIntoSql = TdSqlUtil.getInsertIntoSql(tbName, standMap, idKeyName, true);
        if (StrUtil.isEmpty(insertIntoSql)) {
            log.warn("JdbcTemplateUtil#saveOrUpdateBatch fail, build sql fail, map is empty! ");
            return;
        }

        // 将列表数据进行分批处理，默认是 500 一批次
        doBatchUpdateByMap(mapList, insertIntoSql.toString(), 500, standMap);
    }

    /**
     * 检查db是否存在
     * (需要先选定一个已存在的数据库)
     *
     * @param dbName 数据库名字
     * @return boolean
     */
    public boolean checkDbExist(String dbName) {
        List<String> databaseName = namedParameterJdbcTemplate.getJdbcTemplate().query("SHOW DATABASES", (rs, row) -> rs.getString(1));
        if (CollectionUtils.isEmpty(databaseName)) {
            return false;
        }

        return databaseName.contains(dbName);
    }

    private <T> void doBatchUpdate(List<T> entityList, String insertIntoSql, int pageSize) {
        List<List<T>> partition = ListUtil.partition(entityList, pageSize);
        LocalDateTime now = LocalDateTime.now();
        for (List<T> subList : partition) {
            List<Map<String, Object>> paramsMapList = new ArrayList<>(subList.size());
            for (T entity : subList) {
                List<Field> fields = ClassUtil.getAllFields(entity.getClass());
                Map<String, Object> paramsMap = new HashMap<>(fields.size());
                for (Field field : fields) {
                    String fieldName = field.getName();
                    // 针对创建和更新时间, 使用当前时间
                    if ("createTime".equals(fieldName) || "updateTime".equals(fieldName)) {
                        paramsMap.put(fieldName, now);
                        continue;
                    }
                    paramsMap.put(fieldName, ReflectUtil.getFieldValue(entity, field));
                }
                paramsMapList.add(paramsMap);
            }

            int[] results = namedParameterJdbcTemplate.batchUpdate(insertIntoSql, paramsMapList.toArray(new Map[0]));
            if (log.isDebugEnabled()) {
                log.debug("JdbcTemplateUtil#doBatchUpdate result:{}, sql: {}", Arrays.stream(results).filter(item -> 1 == item).count(), insertIntoSql);
            }
        }
    }


    private void doBatchUpdateByMap(List<Map<String, Object>> entityList, String insertIntoSql, int pageSize, Map<String, Object> standMap) {
        List<List<Map<String, Object>>> partition = ListUtil.partition(entityList, pageSize);
        for (List<Map<String, Object>> subList : partition) {
            List<Map<String, Object>> paramsMapList = new ArrayList<>(subList.size());
            for (Map<String, Object> map : subList) {
                Set<? extends Map.Entry<String, ?>> entries = map.entrySet();
                Map<String, Object> paramsMap = new HashMap<>(standMap);
                for (Map.Entry<String, ?> entry : entries) {
                    paramsMap.put(entry.getKey(), entry.getValue());
                }
                paramsMapList.add(paramsMap);
            }

            int[] results = namedParameterJdbcTemplate.batchUpdate(insertIntoSql, paramsMapList.toArray(new Map[0]));
            if (log.isDebugEnabled()) {
                log.debug("JdbcTemplateUtil#doBatchUpdateByMap result:{}, sql: {}", Arrays.stream(results).filter(item -> 1 == item).count(), insertIntoSql);
            }
        }
    }


    private static Map<String, Object> getStandMap(List<Map<String, Object>> mapList) {
        if (CollectionUtils.isEmpty(mapList)) {
            return null;
        }
        // 获取Key最全的map
        Map<String, Object> standMap = mapList.stream().max(Comparator.comparingInt(map -> map.keySet().size())).get();
        // 将值都初始化为null
        Map<String, Object> stringObjectMap = new HashMap<>(standMap);
        stringObjectMap.entrySet().forEach(entry -> entry.setValue(null));
        return stringObjectMap;
    }
}
