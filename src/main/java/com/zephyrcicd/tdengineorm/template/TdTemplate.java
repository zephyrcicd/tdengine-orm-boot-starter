package com.zephyrcicd.tdengineorm.template;

import com.zephyrcicd.tdengineorm.config.TdOrmConfig;
import com.zephyrcicd.tdengineorm.constant.SqlConstant;
import com.zephyrcicd.tdengineorm.constant.TdSqlConstant;
import com.zephyrcicd.tdengineorm.dto.Page;
import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.exception.TdOrmExceptionCode;
import com.zephyrcicd.tdengineorm.interceptor.TdSqlInterceptorChain;
import com.zephyrcicd.tdengineorm.strategy.DefaultDynamicNameStrategy;
import com.zephyrcicd.tdengineorm.strategy.DynamicNameStrategy;
import com.zephyrcicd.tdengineorm.util.AssertUtil;
import com.zephyrcicd.tdengineorm.util.TdSqlUtil;
import com.zephyrcicd.tdengineorm.wrapper.AbstractTdQueryWrapper;
import com.zephyrcicd.tdengineorm.wrapper.TdQueryWrapper;
import com.zephyrcicd.tdengineorm.wrapper.TdWrappers;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.collections4.ListUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.*;

import static com.zephyrcicd.tdengineorm.util.StringUtil.addSingleQuotes;


/**
 * TDengine 数据访问模板类
 * 提供对 TDengine 数据库的 CRUD 操作，支持动态表名、批量插入等功能
 *
 * @author Zephyr
 */
@Slf4j
@Setter
public class TdTemplate extends AbstractTdJdbcTemplate {

    @Getter
    private final TdOrmConfig tdOrmConfig;

    /**
     * 构造函数
     *
     * @param namedParameterJdbcTemplate JDBC 模板
     * @param tdOrmConfig                配置
     */
    public TdTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate, TdOrmConfig tdOrmConfig) {
        super(namedParameterJdbcTemplate);
        this.tdOrmConfig = tdOrmConfig;
    }

    /**
     * 创建 TdTemplate 实例（新版工厂方法）
     * <p>
     * 支持 SQL 拦截器链和 MetaObjectHandler。
     * </p>
     *
     * @param namedParameterJdbcTemplate JDBC 模板
     * @param tdOrmConfig                配置
     * @param metaObjectHandler          元对象处理器（可为 null）
     * @param sqlInterceptorChain        SQL 拦截器链（可为 null）
     * @return TdTemplate 实例
     */
    public static TdTemplate getInstance(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                          TdOrmConfig tdOrmConfig,
                                          MetaObjectHandler metaObjectHandler,
                                          TdSqlInterceptorChain sqlInterceptorChain) {
        TdTemplate tdTemplate = new TdTemplate(namedParameterJdbcTemplate, tdOrmConfig);
        tdTemplate.setSqlInterceptorChain(sqlInterceptorChain);

        // 如果有 MetaObjectHandler，创建代理进行实体填充
        if (metaObjectHandler != null) {
            return createDefaultProxy(tdTemplate, new TdTemplateMethodInterceptor(metaObjectHandler));
        }
        return tdTemplate;
    }


    /**
     * 创建代理增强的TdTemplate实例
     * 使用Spring的ProxyFactory创建基于CGLIB的类代理
     *
     * @return 代理增强的TdTemplate实例
     */
    private static TdTemplate createDefaultProxy(TdTemplate tdTemplate, TdTemplateMethodInterceptor... tdTemplateMethodInterceptors) {
        ProxyFactory proxyFactory = new ProxyFactory(tdTemplate);
        proxyFactory.setProxyTargetClass(true);
        if (tdTemplateMethodInterceptors != null) {
            for (TdTemplateMethodInterceptor tdTemplateMethodInterceptor : tdTemplateMethodInterceptors) {
                proxyFactory.addAdvice(tdTemplateMethodInterceptor);
            }
        }
        return (TdTemplate) proxyFactory.getProxy();
    }

    /**
     * 创建超级表
     *
     * @param clazz clazz
     * @return int
     */
    public <T> int createStableTableIfNotExist(Class<T> clazz) {
        List<Field> fieldList = TdSqlUtil.getExistFields(clazz);
        // 区分普通字段和Tag字段
        Pair<List<Field>, List<Field>> fieldListPairByTag = TdSqlUtil.differentiateByTag(fieldList);

        List<Field> commFieldList = fieldListPairByTag.getSecond();
        if (CollectionUtils.isEmpty(commFieldList)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_COMM_FIELD);
        }

        Field primaryTsField = TdSqlUtil.checkPrimaryTsField(commFieldList);

        String finalSql = TdSqlConstant.CREATE_STABLE_IF_NOT_EXIST + TdSqlUtil.getTbName(clazz)
                + TdSqlUtil.buildCreateColumn(commFieldList, primaryTsField);
        List<Field> tagFieldList = fieldListPairByTag.getFirst();

        if (CollectionUtils.isEmpty(tagFieldList)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_TAG_FIELD);
        }
        String tagColumnSql = TdSqlUtil.buildCreateTagColumn(tagFieldList);
        finalSql += SqlConstant.BLANK + TdSqlConstant.TAGS + tagColumnSql;
        return updateWithInterceptor(finalSql, new HashMap<>(0));
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
        return getOneWithInterceptor(resultClass, sql, paramsMap);
    }

    /**
     * 查询单条数据并返回 Map（用于聚合查询等场景）
     *
     * <p>此方法专门用于返回 {@code Map<String, Object>} 类型的单条结果，
     * 适用于以下场景：</p>
     * <ul>
     *     <li>单行聚合统计查询（AVG、SUM、COUNT、MAX、MIN 等）</li>
     *     <li>自定义单列查询（不映射到实体类）</li>
     *     <li>获取分组后的单个结果</li>
     * </ul>
     *
     * <p>相比使用 {@code getOne(wrapper, Map.class)}，此方法：</p>
     * <ul>
     *     <li>✅ 类型安全，无 IDE 警告</li>
     *     <li>✅ 语义清晰，明确表示返回 Map 数据</li>
     *     <li>✅ 性能更好，直接使用 Spring JDBC 的 queryForList 方法</li>
     * </ul>
     *
     * @param wrapper 查询包装器
     * @param <T>     实体类型（用于构建查询条件）
     * @return Map，key 为列名（列别名），value 为列值；如果没有数据则返回 null
     * @see #listAsMap(AbstractTdQueryWrapper) 查询 Map 列表
     * @see #getOne(AbstractTdQueryWrapper, Class) 查询并映射到实体类
     */
    public <T> Map<String, Object> getOneAsMap(AbstractTdQueryWrapper<T> wrapper) {
        String sql = wrapper.getSql();
        Map<String, Object> paramsMap = wrapper.getParamsMap();
        return getOneAsMapWithInterceptor(sql, paramsMap);
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
        return listWithInterceptor(wrapper.getSql(), wrapper.getParamsMap(), resultClass);
    }

    /**
     * 查询并返回 Map 列表
     *
     * <p>此方法专门用于返回 {@code List<Map<String, Object>>} 类型的结果，
     *
     * <p>相比使用 {@code list(wrapper, Map.class)}，此方法：</p>
     * <ul>
     *     <li>✅ 类型安全，无 IDE 警告</li>
     *     <li>✅ 语义清晰，明确表示返回 Map 列表</li>
     *     <li>✅ 性能更好，直接使用 Spring JDBC 的 queryForList 方法</li>
     * </ul>
     *
     * @param wrapper 查询包装器
     * @param <T>     实体类型（用于构建查询条件）
     * @return Map 列表，每个 Map 的 key 为列名（列别名），value 为列值
     * @see #getOneAsMap(AbstractTdQueryWrapper) 查询单条 Map 数据
     * @see #list(AbstractTdQueryWrapper, Class) 查询并映射到实体类
     */
    public <T> List<Map<String, Object>> listAsMap(AbstractTdQueryWrapper<T> wrapper) {
        String sql = wrapper.getSql();
        Map<String, Object> paramsMap = wrapper.getParamsMap();
        return listAsMapWithInterceptor(sql, paramsMap);
    }

    public <T> Page<T> page(long pageNo, long pageSize, TdQueryWrapper<T> wrapper) {
        return page(pageNo, pageSize, wrapper, wrapper.getEntityClass());
    }

    public <T, R> Page<R> page(long pageNo, long pageSize, TdQueryWrapper<T> wrapper, Class<R> resultClass) {
        // 构建安全的计数查询SQL
        String innerSql = wrapper.getSql();
        String countSql = "SELECT COUNT(*) FROM (" + innerSql + ") t";
        Long count = countWithInterceptor(countSql, wrapper.getParamsMap());
        Page<R> page = Page.<R>builder()
                .pageNo(pageNo)
                .pageSize(pageSize)
                .total(count).build();
        if (count != null && count > 0) {
            List<R> list = listWithInterceptor(wrapper.limit(pageNo, pageSize).getSql(), wrapper.getParamsMap(), resultClass);
            page.setDataList(list);
        }
        return page;
    }

    /**
     * 统计数据量
     *
     * @param wrapper 查询包装器
     * @return 数据条数
     */
    public <T> Long count(AbstractTdQueryWrapper<T> wrapper) {
        // 构建安全的计数查询SQL
        String innerSql = wrapper.getSql();
        String countSql = "SELECT COUNT(*) FROM (" + innerSql + ") t";
        Map<String, Object> paramsMap = wrapper.getParamsMap();

        return countWithInterceptor(countSql, paramsMap);
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
        List<Field> fields = TdSqlUtil.getExistFields(entity.getClass());
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
     * @param dynamicNameStrategy 实体类表名称获取策略
     * @param object              实体对象
     * @param <T>                 实体类型
     * @return 影响的行数
     * @throws TdOrmException 如果表名为空或实体类没有非TAG字段
     */
    public <T> int insert(DynamicNameStrategy<T> dynamicNameStrategy, T object) {
        String tbName = dynamicNameStrategy.getTableName(object);
        AssertUtil.notBlank(tbName, new TdOrmException(TdOrmExceptionCode.TABLE_NAME_BLANK));

        // 获取非TAG字段
        List<Field> noTagFieldList = TdSqlUtil.getExistNonTagFields(object.getClass());
        if (CollectionUtils.isEmpty(noTagFieldList)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_COMM_FIELD);
        }

        return doInsertEntity(object, tbName, noTagFieldList);
    }


    private <T> int doInsertEntity(T object, String tbName, List<Field> noTagFieldList) {
        Map<String, Object> paramsMap = new HashMap<>(noTagFieldList.size());

        String sql = SqlConstant.INSERT_INTO + addSingleQuotes(tbName) + TdSqlUtil.joinColumnNamesAndValuesSql(object, noTagFieldList, paramsMap);
        return updateWithInterceptor(sql, paramsMap);
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
    public int insert(DynamicNameStrategy<Map<String, Object>> mapTableNameStrategy, Map<String, Object> dataMap) {
        return doInsertMap(mapTableNameStrategy.getTableName(dataMap), dataMap);
    }

    private int doInsertMap(String tableName, Map<String, Object> dataMap) {
        StringBuilder sql = new StringBuilder(SqlConstant.INSERT_INTO)
                .append(addSingleQuotes(tableName))
                .append(SqlConstant.LEFT_BRACKET);

        StringBuilder valueSql = new StringBuilder(") VALUES (");
        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            sql.append(entry.getKey()).append(SqlConstant.COMMA);
            valueSql.append(SqlConstant.COLON).append(entry.getKey()).append(SqlConstant.COMMA);
        }
        sql.deleteCharAt(sql.length() - 1);
        valueSql.deleteCharAt(valueSql.length() - 1).append(SqlConstant.RIGHT_BRACKET);
        sql.append(valueSql);
        return updateWithInterceptor(sql.toString(), dataMap);
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
    public <T> int insertUsing(T object, DynamicNameStrategy<T> dynamicTbNameStrategy) {
        // 获取SQL&参数值
        Pair<String, Map<String, Object>> finalSqlAndParamsMapPair = TdSqlUtil.getFinalInsertUsingSql(object,
                TdSqlUtil.getExistFields(object.getClass()), dynamicTbNameStrategy);

        String finalSql = finalSqlAndParamsMapPair.getFirst();
        Map<String, Object> paramsMap = finalSqlAndParamsMapPair.getSecond();

        return updateWithInterceptor(finalSql, paramsMap);
    }

    public <T> int[] batchInsert(Class<T> clazz, List<T> entityList) {
        return batchInsert(clazz, entityList, tdOrmConfig.getPageSize(), new DefaultDynamicNameStrategy<>());
    }


    /**
     * 批量插入数据（使用动态表名策略，默认批次大小）
     *
     * <p>
     * 批量插入实体列表到不同的子表中。会根据表名策略对数据进行智能分组，
     * 相同表名的数据放在一起合并为一条批量 INSERT 语句，提高插入效率。
     * 使用指定批次大小（tdOrmConfig.getPageSize()）进行分批插入。
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
    public <T> int[] batchInsert(Class<T> clazz, List<T> entityList, DynamicNameStrategy<T> dynamicTbNameStrategy) {
        return batchInsert(clazz, entityList, tdOrmConfig.getPageSize(), dynamicTbNameStrategy);
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
    public <T> int[] batchInsert(Class<T> clazz, List<T> entityList, int pageSize, DynamicNameStrategy<T> dynamicTbNameStrategy) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Partition size must be greater than zero");
        }

        // 不使用USING语法时, 不能指定TAG字段的值
        List<Field> fieldList = TdSqlUtil.getExistNonTagFields(clazz);

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
            List<List<T>> partition = ListUtils.partition(groupEntityList, pageSize);

            for (List<T> list : partition) {
                Map<String, Object> paramsMap = new HashMap<>(list.size());
                StringBuilder insertIntoSql = TdSqlUtil.getInsertIntoSqlPrefix(tbName, fieldList);
                StringBuilder finalSql = new StringBuilder(insertIntoSql);
                joinInsetSqlSuffix(list, finalSql, paramsMap);
                int singleResult = updateWithInterceptor(finalSql.toString(), paramsMap);
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
        return batchInsert(tableName, dataList, tdOrmConfig.getPageSize());
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
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Partition size must be greater than zero");
        }
        AssertUtil.notBlank(tableName, new TdOrmException(TdOrmExceptionCode.TABLE_NAME_BLANK));
        if (CollectionUtils.isEmpty(dataList)) {
            return new int[0];
        }

        return doBatchInsertMaps(tableName, dataList, pageSize);
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
    public int[] batchInsert(List<Map<String, Object>> dataList, DynamicNameStrategy<Map<String, Object>> strategy) {
        return batchInsert(dataList, strategy, tdOrmConfig.getPageSize());
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
    public int[] batchInsert(List<Map<String, Object>> dataList, DynamicNameStrategy<Map<String, Object>> strategy,
                             int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Partition size must be greater than zero");
        }
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

            int[] batchResults = doBatchInsertMaps(tbName, groupDataList, pageSize);
            for (int result : batchResults) {
                resultList.add(result);
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
        return batchInsertUsing(clazz, entityList, tdOrmConfig.getPageSize(), new DefaultDynamicNameStrategy<>());
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
    public <T> int[] batchInsertUsing(Class<T> clazz, List<T> entityList, DynamicNameStrategy<T> dynamicTbNameStrategy) {
        return batchInsertUsing(clazz, entityList, tdOrmConfig.getPageSize(), dynamicTbNameStrategy);
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
    public <T> int[] batchInsertUsing(Class<T> clazz, List<T> entityList, int pageSize, DynamicNameStrategy<T> dynamicTbNameStrategy) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Partition size must be greater than zero");
        }

        if (CollectionUtils.isEmpty(entityList)) {
            return new int[0];
        }

        // 获取TAG字段列表
        List<Field> tagFields = TdSqlUtil.getExistTagFields(clazz);

        // 按照子表名对数据进行分组，相同子表的数据（相同TAG值）放在一起
        Map<String, List<T>> tagGroupMap = new LinkedHashMap<>();

        for (T entity : entityList) {
            // 使用动态表名策略生成子表名作为分组key
            // 相同子表名意味着相同的TAG值组合
            String subTableName = dynamicTbNameStrategy.getTableName(entity);
            tagGroupMap.computeIfAbsent(subTableName, k -> new ArrayList<>()).add(entity);
        }

        // 对每个TAG组（子表）分别进行批量插入
        List<Integer> resultList = new ArrayList<>();

        for (Map.Entry<String, List<T>> entry : tagGroupMap.entrySet()) {
            List<T> groupEntityList = entry.getValue();

            // 对当前TAG组进行分页
            List<List<T>> partition = ListUtils.partition(groupEntityList, pageSize);

            // 使用该组第一个实体获取TAG值（同一组内TAG值相同）
            T firstEntity = groupEntityList.get(0);
            Map<String, Object> tagValueMap = TdSqlUtil.getFiledValueMap(tagFields, firstEntity);

            // 对每个分页批次进行插入
            for (List<T> list : partition) {
                Map<String, Object> paramsMap = new HashMap<>(list.size());
                paramsMap.putAll(tagValueMap);

                StringBuilder finalSql = new StringBuilder(TdSqlUtil.getInsertUsingSqlPrefix(firstEntity,
                        TdSqlUtil.getExistFields(clazz), dynamicTbNameStrategy, paramsMap));
                joinInsetSqlSuffix(list, finalSql, paramsMap);
                int singleResult = updateWithInterceptor(finalSql.toString(), paramsMap);
                if (log.isDebugEnabled()) {
                    log.debug("{} =====execute result====>{}", finalSql, singleResult);
                }
                resultList.add(singleResult);
            }
        }

        return resultList.stream().mapToInt(Integer::intValue).toArray();
    }

    public <T> int deleteByTs(Class<T> clazz, Long ts) {
        String tbName = TdSqlUtil.getTbName(clazz);
        String sql = "DELETE FROM " + tbName + " WHERE ts = :ts";
        Map<String, Object> paramsMap = new HashMap<>(1);
        paramsMap.put("ts", ts);
        return updateWithInterceptor(sql, paramsMap);
    }

    public <T> int batchDeleteByTs(Class<T> clazz, List<Long> tsList) {
        String tbName = TdSqlUtil.getTbName(clazz);
        String sql = "DELETE FROM " + tbName + " WHERE ts IN (:tsList)";
        Map<String, Object> paramsMap = new HashMap<>(1);
        paramsMap.put("tsList", tsList);
        return updateWithInterceptor(sql, paramsMap);
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
                .append(addSingleQuotes(tableName))
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

    /**
     * 批量插入Map数据的核心实现方法
     *
     * @param tableName 表名
     * @param dataList  数据列表
     * @param pageSize  批次大小
     * @return 每批插入影响的行数数组
     */
    private int[] doBatchInsertMaps(String tableName, List<Map<String, Object>> dataList, int pageSize) {
        // 查找key最多的Map并构建SQL前缀（性能优化：只构建一次）
        Map<String, Object> maxKeysMap = findMapWithMaxKeys(dataList);
        Set<String> columnNames = maxKeysMap.keySet();
        String sqlPrefix = buildInsertSqlPrefix(tableName, columnNames);

        // 分批进行插入
        List<List<Map<String, Object>>> partition = ListUtils.partition(dataList, pageSize);
        List<Integer> resultList = new ArrayList<>();
        int processedCount = 0; // 全局计数器，确保参数名唯一

        for (List<Map<String, Object>> batch : partition) {
            Map<String, Object> paramsMap = new HashMap<>(batch.size() * columnNames.size());
            String valuesSql = buildValuesSql(batch, columnNames, paramsMap, processedCount);
            String sql = sqlPrefix + valuesSql;

            int singleResult = updateWithInterceptor(sql, paramsMap);
            resultList.add(singleResult);
            processedCount += batch.size(); // 更新已处理数量
        }

        return resultList.stream().mapToInt(Integer::intValue).toArray();
    }

    private static <T> void joinInsetSqlSuffix(List<T> list, StringBuilder finalSql, Map<String, Object> paramsMap) {
        for (int i = 0; i < list.size(); i++) {
            T entity = list.get(i);
            List<Field> fields = TdSqlUtil.getExistNonTagFields(entity.getClass());
            Pair<String, Map<String, Object>> insertSqlSuffix = TdSqlUtil.getInsertSqlSuffix(entity, fields, i);
            finalSql.append(insertSqlSuffix.getFirst());
            paramsMap.putAll(insertSqlSuffix.getSecond());
        }
    }

    /**
     * TdTemplate方法拦截器（基于Spring AOP的CGLIB代理）
     * 用于在方法调用前后添加元对象处理逻辑
     */
    private static class TdTemplateMethodInterceptor implements MethodInterceptor {
        private final MetaObjectHandler metaObjectHandler;

        public TdTemplateMethodInterceptor(MetaObjectHandler metaObjectHandler) {
            this.metaObjectHandler = metaObjectHandler;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            // 只有在设置了MetaObjectHandler时才执行处理逻辑
            if (metaObjectHandler != null) {
                // 对于插入相关方法，在调用前进行元对象处理
                String methodName = invocation.getMethod().getName();
                if (methodName.equals("insert") || methodName.equals("insertUsing") ||
                        methodName.startsWith("batchInsert")) {

                    // 处理参数中的实体对象或Map
                    processInsertParams(invocation.getArguments());
                }
            }

            // 调用原始方法
            return invocation.proceed();
        }

        /**
         * 处理插入方法的参数，执行元对象填充
         *
         * @param args 方法参数
         */
        private void processInsertParams(Object[] args) {
            if (args == null || args.length == 0) {
                return;
            }

            // 使用函数式编程风格处理参数
            java.util.Arrays.stream(args)
                    .filter(arg -> arg != null && !(arg instanceof DynamicNameStrategy))
                    .forEach(this::processObject);
        }

        /**
         * 处理单个对象或列表对象
         *
         * @param obj 待处理的对象
         */
        private void processObject(Object obj) {
            if (obj == null) {
                return;
            }

            // 使用函数式编程风格处理对象
            if (obj instanceof List) {
                ((List<?>) obj).forEach(this::processObject);
            } else {
                metaObjectHandler.insertFill(obj);
            }
        }
    }
}
