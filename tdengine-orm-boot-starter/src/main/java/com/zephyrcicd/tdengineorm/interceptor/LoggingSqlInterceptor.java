package com.zephyrcicd.tdengineorm.interceptor;

import com.zephyrcicd.tdengineorm.config.TdOrmConfig;
import com.zephyrcicd.tdengineorm.enums.TdLogLevelEnum;
import com.zephyrcicd.tdengineorm.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 内置日志拦截器
 * <p>
 * 替换原有的 {@code tdLog} 方法，在 SQL 执行前记录日志。
 * 保持与原有日志格式的兼容性。
 * </p>
 *
 * @author Zephyr
 * @since 2.2.0
 */
@Slf4j
public class LoggingSqlInterceptor implements TdSqlInterceptor {

    private static final String LOG_FORMAT = "【TD-Orm】 \n【SQL】 : {} \n【Params】: {}";

    private final TdOrmConfig config;

    public LoggingSqlInterceptor(TdOrmConfig config) {
        this.config = config;
    }

    @Override
    public boolean beforeExecute(TdSqlContext context) {
        TdLogLevelEnum logLevel = config == null ? null : config.getLogLevel();
        if (logLevel == null) {
            return true;
        }

        switch (logLevel) {
            case DEBUG:
                if (log.isDebugEnabled()) {
                    String paramsJson = JsonUtil.toJson(context.getParams());
                    log.debug(LOG_FORMAT, context.getSql(), paramsJson);
                }
                break;
            case INFO:
                String paramsJson = JsonUtil.toJson(context.getParams());
                log.info(LOG_FORMAT, context.getSql(), paramsJson);
                break;
            default:
                // WARN 和 ERROR 级别不输出常规日志
                break;
        }

        return true;
    }

    @Override
    public void afterExecute(TdSqlContext context, Object result, Throwable ex) {
        // 如果有异常且日志级别允许，记录错误日志
        if (ex != null && config != null) {
            TdLogLevelEnum logLevel = config.getLogLevel();
            if (logLevel == TdLogLevelEnum.ERROR || logLevel == TdLogLevelEnum.WARN) {
                long duration = System.currentTimeMillis() - context.getStartTime();
                log.error("【TDengineMapperLog】SQL execution failed in {}ms \n【SQL】: {} \n【Error】: {}",
                        duration, context.getSql(), ex.getMessage());
            }
        }
    }

    @Override
    public int getOrder() {
        // 最低优先级，确保日志在其他拦截器之后记录（beforeExecute 最后执行）
        // 这样其他拦截器可以修改上下文后再记录日志
        return Integer.MAX_VALUE;
    }
}
