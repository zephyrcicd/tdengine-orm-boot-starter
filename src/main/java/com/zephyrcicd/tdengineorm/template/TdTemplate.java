package com.zephyrcicd.tdengineorm.template;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONUtil;
import com.zephyrcicd.tdengineorm.annotation.TdTag;
import com.zephyrcicd.tdengineorm.constant.SqlConstant;
import com.zephyrcicd.tdengineorm.constant.TdSqlConstant;
import com.zephyrcicd.tdengineorm.dto.Page;
import com.zephyrcicd.tdengineorm.enums.TdLogLevelEnum;
import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.exception.TdOrmExceptionCode;
import com.zephyrcicd.tdengineorm.mapper.TdColumnRowMapper;
import com.zephyrcicd.tdengineorm.strategy.DefaultEntityTableNameStrategy;
import com.zephyrcicd.tdengineorm.strategy.EntityTableNameStrategy;
import com.zephyrcicd.tdengineorm.strategy.MapTableNameStrategy;
import com.zephyrcicd.tdengineorm.util.AssertUtil;
import com.zephyrcicd.tdengineorm.util.ClassUtil;
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
import java.util.*;
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

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    public static final Integer DEFAULT_BATCH_SIZE = 500;


    /**
     * 创建超级表
     *
     * @param clazz clazz
     * @return int
     */
    public <T> int createStableTableIfNotExist(Class<T> clazz) {
        List<Field> fieldList = ClassUtil.getAllFields(clazz);
        // 区分普通字段和Tag字段
        Pair<List<Field>, List<Field>> fieldListPairByTag = TdSqlUtil.differentiateByTag(fieldList);

        List<Field> commFieldList = fieldListPairByTag.getValue();
        if (CollectionUtils.isEmpty(commFieldList)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_COMM_FIELD);
        }

        Field primaryTsField = TdSqlUtil.checkPrimaryTsField(commFieldList);

        String finalSql = TdSqlConstant.CREATE_STABLE_IF_NOT_EXIST + TdSqlUtil.getTbName(clazz) + TdSqlUtil.buildCreateColumn(commFieldList, primaryTsField);
        List<Field> tagFieldList = fieldListPairByTag.getKey();

        if (CollectionUtils.isEmpty(tagFieldList)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_TAG_FIELD);
        }
        String tagColumnSql = TdSqlUtil.buildCreateColumn(tagFieldList, null);
        finalSql += SqlConstant.BLANK + TdSqlConstant.TAGS + tagColumnSql;
        return updateWithTdLog(finalSql, new HashMap<>(0));
    }

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
     * 插入单条数据（使用默认表名）
     *
     * <p>
     * 使用实体类上的 @TdTable 注解值作为表名进行插入。
     * 如果实体类对应的是超级表，会使用超级表名称作为表名插入，故要求实体类内对Tag相关字段有准确的赋值。
     * </p>
     *
     * <p><b>适用场景：</b>普通表插入、向超级表插入数据（需包含 TAG 字段值）</p>
     *
     * @param entity 实体对象（必须有 @TdTable 注解）
     * @param <T>    实体类型
     * @return 影响的行数
     * @throws TdOrmException 如果实体类没有字段
     */
    public <T> int insert(T entity) {
        String tbName = TdSqlUtil.getTbName(entity.getClass());

        // 拿到所有字段进行赋值
        List<Field> fields = ClassUtil.getAllFields(entity.getClass());
        if (CollectionUtils.isEmpty(fields)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_FILED);
        }

        return doInsertEntity(entity, tbName, fields);
    }

    /**
     * 向指定子表插入数据（使用动态表名策略）
     *
     * <p>
     * 通过策略模式动态生成表名，适用于 TDengine 子表场景。
     * 由于已经通过策略指定了子表名称，会跳过 TAG 相关字段的赋值，只插入普通字段。
     * </p>
     *
     * <p><b>适用场景：</b>TDengine 子表插入（根据设备ID等生成子表名）、动态分表场景</p>
     *
     * @param entityTableNameStrategy 实体类表名称获取策略
     * @param object                  实体对象
     * @param <T>                     实体类型
     * @return 影响的行数
     * @throws TdOrmException 如果表名为空或实体类没有非TAG字段
     */
    public <T> int insert(EntityTableNameStrategy<T> entityTableNameStrategy, T object) {
        String tbName = entityTableNameStrategy.getTableName(object);
        AssertUtil.notBlank(tbName, new TdOrmException(TdOrmExceptionCode.TABLE_NAME_BLANK));

        // 获取非TAG字段
        List<Field> noTagFieldList = TdSqlUtil.getNoTagFieldList(object.getClass());
        if (CollectionUtils.isEmpty(noTagFieldList)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_COMM_FIELD);
        }

        return doInsertEntity(object, tbName, noTagFieldList);
    }


    private <T> int doInsertEntity(T object, String tbName, List<Field> noTagFieldList) {
        Map<String, Object> paramsMap = new HashMap<>(noTagFieldList.size());

        String sql = SqlConstant.INSERT_INTO + tbName + TdSqlUtil.joinColumnNamesAndValuesSql(object, noTagFieldList, paramsMap);
        return updateWithTdLog(sql, paramsMap);
    }


    /**
     * 使用 Map 作为数据载体插入指定表（使用动态表名策略）
     *
     * <p>
     * 适用于无需定义实体类的灵活场景，直接使用 Map 存储数据并通过策略动态生成表名。
     * Map 的 key 为列名，value 为列值。
     * </p>
     *
     * <p><b>适用场景：</b>动态字段场景、快速原型开发、基于 Map 的数据动态分表</p>
     *
     * @param mapTableNameStrategy Map 载体表名称策略
     * @param dataMap              数据 Map（key=列名, value=列值）
     * @return 影响的行数
     */
    public int insert(MapTableNameStrategy mapTableNameStrategy, Map<String, Object> dataMap) {
        return doInsertMap(mapTableNameStrategy.getTableName(dataMap), dataMap);
    }

    private int doInsertMap(String tableName, Map<String, Object> dataMap) {
        StringBuilder sql = new StringBuilder(SqlConstant.INSERT_INTO)
                .append(tableName)
                .append(SqlConstant.LEFT_BRACKET);

        StringBuilder valueSql = new StringBuilder(") VALUES (");
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            sql.append(entry.getKey()).append(SqlConstant.COMMA);
            valueSql.append(SqlConstant.COLON).append(entry.getKey()).append(SqlConstant.COMMA);
        }
        sql.deleteCharAt(sql.length() - 1);
        valueSql.deleteCharAt(valueSql.length() - 1).append(SqlConstant.RIGHT_BRACKET);
        sql.append(valueSql);
        return updateWithTdLog(sql.toString(), dataMap);
    }


    /**
     * 使用 USING 语法插入子表数据（自动创建子表）
     *
     * <p>
     * 使用 TDengine 的 INSERT INTO ... USING ... TAGS 语法自动创建子表并插入数据。
     * 如果子表不存在，会自动根据超级表和 TAG 值创建子表；如果已存在则直接插入。
     * </p>
     *
     * <p><b>适用场景：</b>TDengine 子表首次插入、动态设备接入场景、需要同时指定 TAG 和普通字段</p>
     *
     * @param object                实体对象（需包含 TAG 和普通字段）
     * @param dynamicTbNameStrategy 动态子表名称策略
     * @param <T>                   实体类型
     * @return 影响的行数
     */
    public <T> int insertUsing(T object, EntityTableNameStrategy<T> dynamicTbNameStrategy) {
        // 获取SQL&参数值
        Pair<String, Map<String, Object>> finalSqlAndParamsMapPair = TdSqlUtil.getFinalInsertUsingSql(object,
                ClassUtil.getAllFields(object.getClass()), dynamicTbNameStrategy);

        String finalSql = finalSqlAndParamsMapPair.getKey();
        Map<String, Object> paramsMap = finalSqlAndParamsMapPair.getValue();

        return updateWithTdLog(finalSql, paramsMap);
    }


    /**
     * 批量插入数据（使用动态表名策略，默认批次大小）
     *
     * <p>
     * 批量插入实体列表到不同的子表中。会根据表名策略对数据进行智能分组，
     * 相同表名的数据合并为一条批量 INSERT 语句，提高插入效率。
     * 使用默认批次大小（{@value DEFAULT_BATCH_SIZE}）进行分批插入。
     * </p>
     *
     * <p><b>适用场景：</b>大批量数据导入不同子表、多设备数据批量上报、分表场景的批量写入</p>
     *
     * @param clazz                 实体类 Class
     * @param entityList            实体列表
     * @param dynamicTbNameStrategy 动态表名策略
     * @param <T>                   实体类型
     * @return 每批插入影响的行数数组
     */
    public <T> int[] batchInsert(Class<T> clazz, List<T> entityList, EntityTableNameStrategy<T> dynamicTbNameStrategy) {
        return batchInsert(clazz, entityList, DEFAULT_BATCH_SIZE, dynamicTbNameStrategy);
    }

    /**
     * 批量插入数据（使用动态表名策略，自定义批次大小）
     *
     * <p>
     * 批量插入实体列表到不同的子表中，支持自定义每批次大小。
     * 会根据表名策略对数据进行智能分组，相同表名的数据合并为一条批量 INSERT 语句。
     * 每个分组内部会按照指定的 pageSize 进行分批插入，避免单次 SQL 过大。
     * </p>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *     <li>不使用 USING 语法，因此不能指定 TAG 字段的值</li>
     *     <li>适用于子表已存在的场景</li>
     *     <li>数据会先按表名分组，再按 pageSize 分批</li>
     * </ul>
     *
     * <p><b>适用场景：</b>大批量数据导入不同子表（自定义批次大小）、多设备数据批量上报、分表场景的批量写入</p>
     *
     * @param clazz                 实体类 Class
     * @param entityList            实体列表
     * @param pageSize              每批次大小
     * @param dynamicTbNameStrategy 动态表名策略
     * @param <T>                   实体类型
     * @return 每批插入影响的行数数组
     */
    public <T> int[] batchInsert(Class<T> clazz, List<T> entityList, int pageSize, EntityTableNameStrategy<T> dynamicTbNameStrategy) {
        // 不使用USING语法时, 不能指定TAG字段的值
        List<Field> fieldList = ClassUtil.getAllFields(clazz, field -> !field.isAnnotationPresent(TdTag.class));

        // 按照命名策略对数据进行分组,相同表名的数据放在一起
        Map<String, List<T>> tableGroupMap = new HashMap<>();

        for (T entity : entityList) {
            String tbName = dynamicTbNameStrategy.getTableName(entity);
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

    /**
     * 批量插入Map数据到指定表（使用默认批次大小）
     *
     * <p>
     * 将List中的所有Map插入到同一张指定的表中。
     * 适用于无需实体类的灵活批量插入场景，Map的key为列名，value为列值。
     * </p>
     *
     * <p><b>适用场景：</b>从JSON/API批量导入数据、无需实体类的批量插入、动态字段场景</p>
     *
     * @param tableName 表名
     * @param dataList  数据列表（每个Map代表一行数据）
     * @return 每批插入影响的行数数组
     */
    public int[] batchInsert(String tableName, List<Map<String, Object>> dataList) {
        return batchInsert(tableName, dataList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 批量插入Map数据到指定表（自定义批次大小）
     *
     * <p>
     * 将List中的所有Map插入到同一张指定的表中，支持自定义每批次大小。
     * 适用于大数据量场景，可通过调整pageSize控制每批SQL的大小。
     * </p>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *     <li>所有Map必须包含相同的key（列名）</li>
     *     <li>Map的key必须与表的列名一致</li>
     * </ul>
     *
     * <p><b>适用场景：</b>大批量Map数据导入、自定义批次控制、性能优化场景</p>
     *
     * @param tableName 表名
     * @param dataList  数据列表（每个Map代表一行数据）
     * @param pageSize  每批次大小
     * @return 每批插入影响的行数数组
     */
    public int[] batchInsert(String tableName, List<Map<String, Object>> dataList, int pageSize) {
        AssertUtil.notBlank(tableName, new TdOrmException(TdOrmExceptionCode.TABLE_NAME_BLANK));
        if (CollectionUtils.isEmpty(dataList)) {
            return new int[0];
        }

        // 查找key最多的Map并构建SQL前缀（性能优化：只构建一次）
        Map<String, Object> maxKeysMap = findMapWithMaxKeys(dataList);
        Set<String> columnNames = maxKeysMap.keySet();
        String sqlPrefix = buildInsertSqlPrefix(tableName, columnNames);

        // 分批进行插入
        List<List<Map<String, Object>>> partition = ListUtil.partition(dataList, pageSize);
        List<Integer> resultList = new ArrayList<>();
        int processedCount = 0; // 全局计数器，确保参数名唯一

        for (List<Map<String, Object>> batch : partition) {
            Map<String, Object> paramsMap = new HashMap<>(batch.size() * columnNames.size());
            String valuesSql = buildValuesSql(batch, columnNames, paramsMap, processedCount);
            String sql = sqlPrefix + valuesSql;

            int singleResult = namedParameterJdbcTemplate.update(sql, paramsMap);
            if (log.isDebugEnabled()) {
                log.debug("{} ===== execute result ====>{}", sql, singleResult);
            }
            resultList.add(singleResult);
            processedCount += batch.size(); // 更新已处理数量
        }

        return resultList.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 批量插入Map数据到不同表（使用策略动态生成表名，默认批次大小）
     *
     * <p>
     * List中的每个Map可能插入到不同的表中，通过策略动态生成表名。
     * 会自动按表名分组，相同表名的Map合并为一条批量INSERT语句。
     * </p>
     *
     * <p><b>适用场景：</b>多设备数据批量上报、基于Map的动态分表、灵活的批量数据处理</p>
     *
     * @param dataList 数据列表（每个Map代表一行数据）
     * @param strategy Map表名称策略
     * @return 每批插入影响的行数数组
     */
    public int[] batchInsert(List<Map<String, Object>> dataList, MapTableNameStrategy strategy) {
        return batchInsert(dataList, strategy, DEFAULT_BATCH_SIZE);
    }

    /**
     * 批量插入Map数据到不同表（使用策略动态生成表名，自定义批次大小）
     *
     * <p>
     * List中的每个Map可能插入到不同的表中，通过策略动态生成表名。
     * 会自动按表名分组，相同表名的Map合并为一条批量INSERT语句。
     * 每个分组内部会按照指定的pageSize进行分批插入，避免单次SQL过大。
     * </p>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *     <li>同一分组（同一表）的所有Map必须包含相同的key（列名）</li>
     *     <li>数据会先按表名分组，再按pageSize分批</li>
     * </ul>
     *
     * <p><b>适用场景：</b>大批量多设备数据上报、基于Map的动态分表、自定义批次控制</p>
     *
     * @param dataList 数据列表（每个Map代表一行数据）
     * @param strategy Map表名称策略
     * @param pageSize 每批次大小
     * @return 每批插入影响的行数数组
     */
    public int[] batchInsert(List<Map<String, Object>> dataList, MapTableNameStrategy strategy, int pageSize) {
        if (CollectionUtils.isEmpty(dataList)) {
            return new int[0];
        }

        // 按照命名策略对数据进行分组,相同表名的数据放在一起
        Map<String, List<Map<String, Object>>> tableGroupMap = new HashMap<>();

        for (Map<String, Object> dataMap : dataList) {
            String tbName = strategy.getTableName(dataMap);
            AssertUtil.notBlank(tbName, new TdOrmException(TdOrmExceptionCode.TABLE_NAME_BLANK));
            tableGroupMap.computeIfAbsent(tbName, k -> new ArrayList<>()).add(dataMap);
        }

        // 对每个分组分别进行批量插入
        List<Integer> resultList = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : tableGroupMap.entrySet()) {
            String tbName = entry.getKey();
            List<Map<String, Object>> groupDataList = entry.getValue();

            // 查找当前分组中key最多的Map并构建SQL前缀（每个表只构建一次）
            Map<String, Object> maxKeysMap = findMapWithMaxKeys(groupDataList);
            Set<String> columnNames = maxKeysMap.keySet();
            String sqlPrefix = buildInsertSqlPrefix(tbName, columnNames);

            // 以防数据量过大, 分批进行插入
            List<List<Map<String, Object>>> partition = ListUtil.partition(groupDataList, pageSize);
            int processedCount = 0; // 当前表的全局计数器

            for (List<Map<String, Object>> batch : partition) {
                Map<String, Object> paramsMap = new HashMap<>(batch.size() * columnNames.size());
                String valuesSql = buildValuesSql(batch, columnNames, paramsMap, processedCount);
                String sql = sqlPrefix + valuesSql;

                int singleResult = namedParameterJdbcTemplate.update(sql, paramsMap);
                if (log.isDebugEnabled()) {
                    log.debug("{} ===== execute result ====>{}", sql, singleResult);
                }
                resultList.add(singleResult);
                processedCount += batch.size(); // 更新已处理数量
            }
        }

        return resultList.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 使用 USING 语法批量插入子表数据（默认表名策略，默认批次大小）
     *
     * <p>
     * 使用 TDengine 的 INSERT INTO ... USING ... TAGS 语法批量插入数据到子表。
     * 如果子表不存在会自动创建。使用默认表名策略和默认批次大小。
     * </p>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *     <li>列表中的所有数据必须属于同一个子表（TAG 值相同）</li>
     *     <li>默认使用超级表名作为子表名</li>
     * </ul>
     *
     * <p><b>适用场景：</b>TDengine 子表首次批量插入、同设备多条数据批量上报</p>
     *
     * @param clazz      实体类 Class
     * @param entityList 实体列表（TAG 值必须相同）
     * @param <T>        实体类型
     * @return 每批插入影响的行数数组
     */
    public <T> int[] batchInsertUsing(Class<T> clazz, List<T> entityList) {
        return batchInsertUsing(clazz, entityList, DEFAULT_BATCH_SIZE, new DefaultEntityTableNameStrategy<>());
    }

    /**
     * 使用 USING 语法批量插入子表数据（自定义表名策略，默认批次大小）
     *
     * <p>
     * 使用 TDengine 的 INSERT INTO ... USING ... TAGS 语法批量插入数据到子表。
     * 通过策略模式动态生成子表名。如果子表不存在会自动创建。
     * </p>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *     <li>列表中的所有数据必须属于同一个子表（TAG 值相同）</li>
     *     <li>通过策略动态生成子表名</li>
     * </ul>
     *
     * <p><b>适用场景：</b>TDengine 子表首次批量插入（动态表名）、同设备多条数据批量上报</p>
     *
     * @param clazz                 实体类 Class
     * @param entityList            实体列表（TAG 值必须相同）
     * @param dynamicTbNameStrategy 动态表名策略
     * @param <T>                   实体类型
     * @return 每批插入影响的行数数组
     */
    public <T> int[] batchInsertUsing(Class<T> clazz, List<T> entityList, EntityTableNameStrategy<T> dynamicTbNameStrategy) {
        return batchInsertUsing(clazz, entityList, DEFAULT_BATCH_SIZE, dynamicTbNameStrategy);
    }

    /**
     * 使用 USING 语法批量插入子表数据（自定义表名策略和批次大小）
     *
     * <p>
     * 使用 TDengine 的 INSERT INTO ... USING ... TAGS 语法批量插入数据到子表。
     * 通过策略模式动态生成子表名，并支持自定义每批次大小。如果子表不存在会自动创建。
     * </p>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *     <li>列表中的所有数据必须属于同一个子表（TAG 值相同）</li>
     *     <li>会使用第一条数据的 TAG 值作为所有数据的 TAG</li>
     *     <li>按照 pageSize 分批执行 INSERT USING 语句</li>
     * </ul>
     *
     * <p><b>适用场景：</b>TDengine 子表首次大批量插入（自定义批次）、同设备大量历史数据导入</p>
     *
     * @param clazz                 实体类 Class
     * @param entityList            实体列表（TAG 值必须相同）
     * @param pageSize              每批次大小
     * @param dynamicTbNameStrategy 动态表名策略
     * @param <T>                   实体类型
     * @return 每批插入影响的行数数组
     */
    public <T> int[] batchInsertUsing(Class<T> clazz, List<T> entityList, int pageSize, EntityTableNameStrategy<T> dynamicTbNameStrategy) {

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
            StringBuilder finalSql = new StringBuilder(TdSqlUtil.getInsertUsingSqlPrefix(t,
                    ClassUtil.getAllFields(clazz), dynamicTbNameStrategy, paramsMap));
            joinInsetSqlSuffix(list, finalSql, paramsMap);
            int singleResult = namedParameterJdbcTemplate.update(finalSql.toString(), paramsMap);
            if (log.isDebugEnabled()) {
                log.debug("{} =====execute result====>{}", finalSql, result);
            }
            result[i] = singleResult;
        }
        return result;
    }

    public <T> int deleteByTs(Class<T> clazz, Long ts) {
        String tbName = TdSqlUtil.getTbName(clazz);
        String sql = "DELETE FROM " + tbName + " WHERE ts = :ts";
        Map<String, Object> paramsMap = new HashMap<>(1);
        paramsMap.put("ts", ts);
        return namedParameterJdbcTemplate.update(sql, paramsMap);
    }

    public <T> int batchDeleteByTs(Class<T> clazz, List<Long> tsList) {
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
        return namedParameterJdbcTemplate.query(sql, paramsMap, TdColumnRowMapper.getInstance(resultClass));
    }

    private <R> R getOneWithTdLog(Class<R> resultClass, String sql, Map<String, Object> paramsMap) {
        tdLog(sql, paramsMap);
        List<R> list = namedParameterJdbcTemplate.query(sql, paramsMap, TdColumnRowMapper.getInstance(resultClass));
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 查找List中key最多的Map
     *
     * @param dataList Map数据列表
     * @return key数量最多的Map
     */
    private Map<String, Object> findMapWithMaxKeys(List<Map<String, Object>> dataList) {
        return dataList.stream()
                .max(Comparator.comparingInt(Map::size))
                .orElseThrow(() -> new TdOrmException(TdOrmExceptionCode.NO_FILED));
    }

    /**
     * 构建INSERT SQL前缀部分（INSERT INTO table (col1, col2) VALUES）
     *
     * @param tableName   表名
     * @param columnNames 列名集合
     * @return SQL前缀字符串
     */
    private String buildInsertSqlPrefix(String tableName, Set<String> columnNames) {
        StringBuilder sql = new StringBuilder(SqlConstant.INSERT_INTO)
                .append(tableName)
                .append(SqlConstant.LEFT_BRACKET);

        for (String columnName : columnNames) {
            sql.append(columnName).append(SqlConstant.COMMA);
        }
        sql.deleteCharAt(sql.length() - 1); // 删除最后的逗号
        sql.append(") VALUES ");

        return sql.toString();
    }

    /**
     * 构建VALUES部分SQL（支持NULL填充和全局索引）
     *
     * @param batch       当前批次的Map数据列表
     * @param columnNames 列名集合（来自key最多的Map）
     * @param paramsMap   参数Map（输出参数）
     * @param startIndex  起始索引（用于参数命名，避免冲突）
     * @return VALUES部分SQL字符串
     */
    private String buildValuesSql(List<Map<String, Object>> batch, Set<String> columnNames,
                                  Map<String, Object> paramsMap, int startIndex) {
        StringBuilder sql = new StringBuilder();

        for (int i = 0; i < batch.size(); i++) {
            sql.append("(");
            Map<String, Object> dataMap = batch.get(i);

            for (String columnName : columnNames) {
                String paramName = columnName + "-" + (startIndex + i); // 全局索引避免冲突
                sql.append(SqlConstant.COLON).append(paramName).append(SqlConstant.COMMA);
                // 使用getOrDefault，缺失的key使用null（性能优先，用户保证数据正确性）
                paramsMap.put(paramName, dataMap.getOrDefault(columnName, null));
            }

            sql.deleteCharAt(sql.length() - 1); // 删除最后的逗号
            sql.append("), ");
        }

        sql.delete(sql.length() - 2, sql.length()); // 删除最后的", "
        return sql.toString();
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
