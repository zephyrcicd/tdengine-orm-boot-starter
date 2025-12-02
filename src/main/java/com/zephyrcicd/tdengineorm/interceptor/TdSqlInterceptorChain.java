package com.zephyrcicd.tdengineorm.interceptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SQL 拦截器链管理
 * <p>
 * 管理多个 {@link TdSqlInterceptor} 的注册、排序和获取。
 * </p>
 *
 * @author Zephyr
 * @since 2.2.0
 */
public class TdSqlInterceptorChain {

    private final List<TdSqlInterceptor> interceptors = new ArrayList<>();
    private volatile boolean sorted = false;

    /**
     * 添加单个拦截器
     *
     * @param interceptor 拦截器实例
     * @return 当前链实例（支持链式调用）
     */
    public TdSqlInterceptorChain addInterceptor(TdSqlInterceptor interceptor) {
        if (interceptor != null) {
            interceptors.add(interceptor);
            sorted = false;
        }
        return this;
    }

    /**
     * 添加多个拦截器
     *
     * @param interceptorList 拦截器列表
     * @return 当前链实例（支持链式调用）
     */
    public TdSqlInterceptorChain addInterceptors(List<TdSqlInterceptor> interceptorList) {
        if (interceptorList != null && !interceptorList.isEmpty()) {
            interceptors.addAll(interceptorList);
            sorted = false;
        }
        return this;
    }

    /**
     * 获取排序后的拦截器列表
     * <p>
     * 按 {@link TdSqlInterceptor#getOrder()} 从小到大排序。
     * </p>
     *
     * @return 排序后的拦截器列表（不可修改）
     */
    public List<TdSqlInterceptor> getInterceptors() {
        if (!sorted) {
            synchronized (this) {
                if (!sorted) {
                    interceptors.sort(Comparator.comparingInt(TdSqlInterceptor::getOrder));
                    sorted = true;
                }
            }
        }
        return interceptors;
    }

    /**
     * 判断是否有拦截器
     *
     * @return true 如果有至少一个拦截器
     */
    public boolean hasInterceptors() {
        return !interceptors.isEmpty();
    }

    /**
     * 获取拦截器数量
     *
     * @return 拦截器数量
     */
    public int size() {
        return interceptors.size();
    }

    /**
     * 清空所有拦截器
     *
     * @return 当前链实例（支持链式调用）
     */
    public TdSqlInterceptorChain clear() {
        interceptors.clear();
        sorted = false;
        return this;
    }
}
