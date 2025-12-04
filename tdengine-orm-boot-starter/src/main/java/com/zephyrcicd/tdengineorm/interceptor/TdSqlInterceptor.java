package com.zephyrcicd.tdengineorm.interceptor;

/**
 * TdTemplate SQL 执行拦截器接口
 * <p>
 * 用于在 SQL 执行前后添加自定义处理逻辑，如日志记录、性能监控、审计等。
 * 类似于 MyBatis 的 Interceptor 设计。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * &#64;Component
 * public class AuditSqlInterceptor implements TdSqlInterceptor {
 *     &#64;Override
 *     public boolean beforeExecute(TdSqlContext context) {
 *         log.info("Executing SQL: {}", context.getSql());
 *         return true;
 *     }
 *
 *     &#64;Override
 *     public void afterExecute(TdSqlContext context, Object result, Throwable ex) {
 *         long duration = System.currentTimeMillis() - context.getStartTime();
 *         log.info("SQL completed in {}ms", duration);
 *     }
 *
 *     &#64;Override
 *     public int getOrder() {
 *         return 100;
 *     }
 * }
 * </pre>
 *
 * @author Zephyr
 * @since 2.2.0
 */
public interface TdSqlInterceptor {

    /**
     * SQL 执行前拦截
     * <p>
     * 在 SQL 实际执行之前调用。可用于：
     * <ul>
     *     <li>记录 SQL 日志</li>
     *     <li>参数校验</li>
     *     <li>安全检查</li>
     *     <li>性能计时开始</li>
     * </ul>
     * </p>
     *
     * @param context SQL 执行上下文，包含 SQL、参数、类型等信息
     * @return true 继续执行 SQL，false 中断执行（此时方法将返回默认值）
     */
    default boolean beforeExecute(TdSqlContext context) {
        return true;
    }

    /**
     * SQL 执行后拦截
     * <p>
     * 在 SQL 执行完成后调用（无论成功或失败都会调用）。可用于：
     * <ul>
     *     <li>记录执行结果</li>
     *     <li>性能监控（记录执行时间）</li>
     *     <li>异常处理/告警</li>
     *     <li>审计日志</li>
     * </ul>
     * </p>
     *
     * @param context SQL 执行上下文
     * @param result  执行结果（可能为 null）
     * @param ex      执行过程中的异常（如果有，否则为 null）
     */
    default void afterExecute(TdSqlContext context, Object result, Throwable ex) {
    }

    /**
     * 获取拦截器的执行顺序
     * <p>
     * 数值越小，优先级越高。
     * <ul>
     *     <li>beforeExecute: 按 order 从小到大顺序执行</li>
     *     <li>afterExecute: 按 order 从大到小逆序执行</li>
     * </ul>
     * </p>
     *
     * @return 顺序值，默认为 0
     */
    default int getOrder() {
        return 0;
    }
}
