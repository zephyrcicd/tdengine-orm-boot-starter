package com.zephyrcicd.tdengineorm.template;

import com.zephyrcicd.tdengineorm.constant.SqlConstant;
import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.exception.TdOrmExceptionCode;
import com.zephyrcicd.tdengineorm.interceptor.TdSqlContext;
import com.zephyrcicd.tdengineorm.interceptor.TdSqlInterceptor;
import com.zephyrcicd.tdengineorm.interceptor.TdSqlInterceptorChain;
import com.zephyrcicd.tdengineorm.mapper.TdColumnRowMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.zephyrcicd.tdengineorm.util.StringUtil.addSingleQuotes;

/**
 * TDengine JDBC 操作抽象基类
 * <p>
 * 提供带拦截器的底层 SQL 执行方法，子类可专注于业务逻辑实现。
 * </p>
 *
 * @author Zephyr
 * @since 2.2.0
 */
@Slf4j
public abstract class AbstractTdJdbcTemplate {

    @Getter
    protected final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * SQL 拦截器链
     */
    @Setter
    protected TdSqlInterceptorChain sqlInterceptorChain;

    protected AbstractTdJdbcTemplate(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    /**
     * 执行更新操作（INSERT/DELETE/CREATE 等）
     * <p>
     * 在执行前后触发 SQL 拦截器链。
     * </p>
     *
     * @param finalSql  SQL 语句
     * @param paramsMap 参数 Map
     * @return 受影响的行数
     */
    protected int updateWithInterceptor(String finalSql, Map<String, Object> paramsMap) {
        TdSqlContext context = TdSqlContext.builder()
                .sql(finalSql)
                .params(paramsMap)
                .sqlType(TdSqlContext.SqlType.UPDATE)
                .startTime(System.currentTimeMillis())
                .build();

        // 执行前拦截
        if (!executeBeforeInterceptors(context)) {
            return 0;
        }

        Integer result = null;
        Throwable ex = null;
        try {
            result = namedParameterJdbcTemplate.update(finalSql, paramsMap);
            return result;
        } catch (Throwable t) {
            ex = t;
            throw t;
        } finally {
            executeAfterInterceptors(context, result, ex);
        }
    }

    /**
     * 执行列表查询
     * <p>
     * 在执行前后触发 SQL 拦截器链。
     * </p>
     *
     * @param sql         SQL 语句
     * @param paramsMap   参数 Map
     * @param resultClass 结果类型
     * @param <R>         结果泛型
     * @return 查询结果列表
     */
    protected <R> List<R> listWithInterceptor(String sql, Map<String, Object> paramsMap, Class<R> resultClass) {
        // 不支持 Map.class，提示使用 listAsMap()
        if (Map.class.isAssignableFrom(resultClass)) {
            log.error("Map.class is not supported as result type! Please use listAsMap() method instead.");
            throw new TdOrmException(TdOrmExceptionCode.MAP_TYPE_NOT_SUPPORTED);
        }

        TdSqlContext context = TdSqlContext.builder()
                .sql(sql)
                .params(paramsMap)
                .sqlType(TdSqlContext.SqlType.QUERY)
                .resultClass(resultClass)
                .startTime(System.currentTimeMillis())
                .build();

        // 执行前拦截
        if (!executeBeforeInterceptors(context)) {
            return Collections.emptyList();
        }

        List<R> result = null;
        Throwable ex = null;
        try {
            result = namedParameterJdbcTemplate.query(sql, paramsMap, TdColumnRowMapper.getInstance(resultClass));
            return result;
        } catch (Throwable t) {
            ex = t;
            throw t;
        } finally {
            executeAfterInterceptors(context, result, ex);
        }
    }

    /**
     * 执行单条查询
     * <p>
     * 在执行前后触发 SQL 拦截器链。
     * </p>
     *
     * @param resultClass 结果类型
     * @param sql         SQL 语句
     * @param paramsMap   参数 Map
     * @param <R>         结果泛型
     * @return 查询结果（可能为 null）
     */
    protected <R> R getOneWithInterceptor(Class<R> resultClass, String sql, Map<String, Object> paramsMap) {
        // 不支持 Map.class，提示使用 getOneAsMap()
        if (Map.class.isAssignableFrom(resultClass)) {
            log.error("Map.class is not supported as result type! Please use getOneAsMap() method instead.");
            throw new TdOrmException(TdOrmExceptionCode.MAP_TYPE_NOT_SUPPORTED);
        }

        TdSqlContext context = TdSqlContext.builder()
                .sql(sql)
                .params(paramsMap)
                .sqlType(TdSqlContext.SqlType.QUERY_ONE)
                .resultClass(resultClass)
                .startTime(System.currentTimeMillis())
                .build();

        // 执行前拦截
        if (!executeBeforeInterceptors(context)) {
            return null;
        }

        R result = null;
        Throwable ex = null;
        try {
            List<R> list = namedParameterJdbcTemplate.query(sql, paramsMap, TdColumnRowMapper.getInstance(resultClass));
            if (!CollectionUtils.isEmpty(list)) {
                result = list.get(0);
            }
            return result;
        } catch (Throwable t) {
            ex = t;
            throw t;
        } finally {
            executeAfterInterceptors(context, result, ex);
        }
    }

    /**
     * 执行 Map 列表查询（支持拦截器）
     *
     * @param sql       SQL 语句
     * @param paramsMap 参数 Map
     * @return Map 列表
     */
    protected List<Map<String, Object>> listAsMapWithInterceptor(String sql, Map<String, Object> paramsMap) {
        TdSqlContext context = TdSqlContext.builder()
                .sql(sql)
                .params(paramsMap)
                .sqlType(TdSqlContext.SqlType.QUERY)
                .startTime(System.currentTimeMillis())
                .build();

        if (!executeBeforeInterceptors(context)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = null;
        Throwable ex = null;
        try {
            result = namedParameterJdbcTemplate.queryForList(sql, paramsMap);
            return result;
        } catch (Throwable t) {
            ex = t;
            throw t;
        } finally {
            executeAfterInterceptors(context, result, ex);
        }
    }

    /**
     * 执行单条 Map 查询（支持拦截器）
     *
     * @param sql       SQL 语句
     * @param paramsMap 参数 Map
     * @return Map 或 null
     */
    protected Map<String, Object> getOneAsMapWithInterceptor(String sql, Map<String, Object> paramsMap) {
        TdSqlContext context = TdSqlContext.builder()
                .sql(sql)
                .params(paramsMap)
                .sqlType(TdSqlContext.SqlType.QUERY_ONE)
                .startTime(System.currentTimeMillis())
                .build();

        if (!executeBeforeInterceptors(context)) {
            return null;
        }

        Map<String, Object> result = null;
        Throwable ex = null;
        try {
            List<Map<String, Object>> list = namedParameterJdbcTemplate.queryForList(sql, paramsMap);
            if (!CollectionUtils.isEmpty(list)) {
                result = list.get(0);
            }
            return result;
        } catch (Throwable t) {
            ex = t;
            throw t;
        } finally {
            executeAfterInterceptors(context, result, ex);
        }
    }

    /**
     * 执行 COUNT 查询（支持拦截器）
     *
     * @param countSql  COUNT SQL 语句
     * @param paramsMap 参数 Map
     * @return 计数结果
     */
    protected Long countWithInterceptor(String countSql, Map<String, Object> paramsMap) {
        TdSqlContext context = TdSqlContext.builder()
                .sql(countSql)
                .params(paramsMap)
                .sqlType(TdSqlContext.SqlType.QUERY_ONE)
                .resultClass(Long.class)
                .startTime(System.currentTimeMillis())
                .build();

        if (!executeBeforeInterceptors(context)) {
            return 0L;
        }

        Long result = null;
        Throwable ex = null;
        try {
            result = namedParameterJdbcTemplate.queryForObject(countSql, paramsMap, Long.class);
            return result != null ? result : 0L;
        } catch (Throwable t) {
            ex = t;
            throw t;
        } finally {
            executeAfterInterceptors(context, result, ex);
        }
    }

    /**
     * 执行前置拦截器
     *
     * @param context SQL 执行上下文
     * @return true 继续执行，false 中断执行
     */
    protected boolean executeBeforeInterceptors(TdSqlContext context) {
        if (sqlInterceptorChain == null || !sqlInterceptorChain.hasInterceptors()) {
            return true;
        }
        for (TdSqlInterceptor interceptor : sqlInterceptorChain.getInterceptors()) {
            try {
                if (!interceptor.beforeExecute(context)) {
                    return false;
                }
            } catch (Exception e) {
                log.warn("SQL interceptor beforeExecute error: {}", e.getMessage());
            }
        }
        return true;
    }

    /**
     * 执行后置拦截器（逆序）
     *
     * @param context SQL 执行上下文
     * @param result  执行结果
     * @param ex      异常
     */
    protected void executeAfterInterceptors(TdSqlContext context, Object result, Throwable ex) {
        if (sqlInterceptorChain == null || !sqlInterceptorChain.hasInterceptors()) {
            return;
        }
        List<TdSqlInterceptor> interceptors = sqlInterceptorChain.getInterceptors();
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            try {
                interceptors.get(i).afterExecute(context, result, ex);
            } catch (Exception e) {
                log.warn("SQL interceptor afterExecute error: {}", e.getMessage());
            }
        }
    }

    // ========== 通用 SQL 构建工具方法 ==========

    /**
     * 从 Map 列表中找出 key 最多的 Map
     * <p>
     * 适用于批量插入时确定列名模板，避免列名缺失。
     * </p>
     *
     * @param dataList Map 数据列表
     * @return key 数量最多的 Map
     * @throws TdOrmException 如果列表为空或所有 Map 都为空
     */
    protected Map<String, Object> findMapWithMaxKeys(List<Map<String, Object>> dataList) {
        return dataList.stream()
                .max(Comparator.comparingInt(Map::size))
                .orElseThrow(() -> new TdOrmException(TdOrmExceptionCode.NO_FILED));
    }

    /**
     * 构建 INSERT SQL 前缀部分
     * <p>
     * 格式：INSERT INTO table (col1, col2, col3) VALUES
     * </p>
     *
     * @param tableName   表名
     * @param columnNames 列名集合（顺序敏感）
     * @return SQL 前缀字符串
     */
    protected String buildInsertSqlPrefix(String tableName, Set<String> columnNames) {
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
     * 构建 VALUES 部分 SQL（支持 NULL 填充和全局索引）
     * <p>
     * 生成格式：(:col1-0, :col2-0, :col3-0), (:col1-1, :col2-1, :col3-1)
     * </p>
     * <p>
     * <b>关键特性：</b>
     * <ul>
     *     <li>全局索引：使用 startIndex 避免参数名冲突</li>
     *     <li>NULL 填充：缺失的 key 自动填充 null</li>
     *     <li>参数命名：格式为 columnName-index</li>
     * </ul>
     * </p>
     *
     * @param batch       当前批次的 Map 数据列表
     * @param columnNames 列名集合（来自 key 最多的 Map）
     * @param paramsMap   参数 Map（输出参数，会被填充）
     * @param startIndex  起始索引（用于参数命名，避免冲突）
     * @return VALUES 部分 SQL 字符串
     */
    protected String buildValuesSql(List<Map<String, Object>> batch, Set<String> columnNames,
                                    Map<String, Object> paramsMap, int startIndex) {
        StringBuilder sql = new StringBuilder();

        for (int i = 0; i < batch.size(); i++) {
            sql.append("(");
            Map<String, Object> dataMap = batch.get(i);

            for (String columnName : columnNames) {
                String paramName = columnName + "-" + (startIndex + i); // 全局索引避免冲突
                sql.append(SqlConstant.COLON).append(paramName).append(SqlConstant.COMMA);
                // 使用 getOrDefault，缺失的 key 使用 null
                paramsMap.put(paramName, dataMap.getOrDefault(columnName, null));
            }

            sql.deleteCharAt(sql.length() - 1); // 删除最后的逗号
            sql.append("), ");
        }

        sql.delete(sql.length() - 2, sql.length()); // 删除最后的 ", "
        return sql.toString();
    }

    /**
     * 批量插入 Map 数据的核心实现方法
     * <p>
     * 包含分批处理、NULL 填充、参数索引管理等完整逻辑。
     * </p>
     *
     * @param tableName 表名
     * @param dataList  数据列表
     * @param pageSize  批次大小
     * @return 每批插入影响的行数数组
     */
    protected int[] doBatchInsertMaps(String tableName, List<Map<String, Object>> dataList, int pageSize) {
        // 查找 key 最多的 Map 并构建 SQL 前缀（性能优化：只构建一次）
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

    @SuppressWarnings("unchecked")
    protected <T> Class<T> inferEntityClass(List<T> entityList) {
        return (Class<T>) entityList.get(0).getClass();
    }
}
