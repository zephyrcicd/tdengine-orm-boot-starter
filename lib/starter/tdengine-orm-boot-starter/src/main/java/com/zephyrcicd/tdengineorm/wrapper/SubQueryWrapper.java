package com.zephyrcicd.tdengineorm.wrapper;

import com.zephyrcicd.tdengineorm.func.GetterFunction;
import com.zephyrcicd.tdengineorm.util.TdSqlUtil;

/**
 * @author Zephyr
 */
public class SubQueryWrapper<L, R> {

    public SubQueryWrapper<L, R> eq(Class<L> leftClass, Class<R> rightClass, GetterFunction<L, ?> leftGetterFunction,
                                    GetterFunction<R, ?> rightGetterFunction) {
        String leftColumnName = TdSqlUtil.getColumnName(leftClass, leftGetterFunction);
        String rightColumnName = TdSqlUtil.getColumnName(rightClass, rightGetterFunction);


        return this;
    }

    public <T, X> R eq(GetterFunction<T, ?> left, GetterFunction<X, ?> right) {


        return null;
    }
}
