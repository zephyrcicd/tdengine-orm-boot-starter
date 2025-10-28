package com.zephyrcicd.tdengineorm.wrapper;

/**
 * @author Zephyr
 */
public class TdWrappers {

    public static <T> TdQueryWrapper<T> queryWrapper(Class<T> targerClass) {
        return new TdQueryWrapper<>(targerClass);
    }
}
