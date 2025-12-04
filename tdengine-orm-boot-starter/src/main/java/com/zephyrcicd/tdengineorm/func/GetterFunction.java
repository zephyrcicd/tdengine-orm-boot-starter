package com.zephyrcicd.tdengineorm.func;

import java.io.Serializable;
import java.util.function.Function;

/**
 * @author Zephyr
 */
@FunctionalInterface
public interface GetterFunction<T, R> extends Function<T, R>, Serializable {

}
