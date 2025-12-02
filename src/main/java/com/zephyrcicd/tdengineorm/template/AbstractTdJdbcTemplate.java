package com.zephyrcicd.tdengineorm.template;

import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.exception.TdOrmExceptionCode;
import com.zephyrcicd.tdengineorm.interceptor.TdSqlContext;
import com.zephyrcicd.tdengineorm.interceptor.TdSqlInterceptor;
import com.zephyrcicd.tdengineorm.interceptor.TdSqlInterceptorChain;
import com.zephyrcicd.tdengineorm.mapper.TdColumnRowMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
}
