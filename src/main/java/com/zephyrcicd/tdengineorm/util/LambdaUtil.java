package com.zephyrcicd.tdengineorm.util;

import com.zephyrcicd.tdengineorm.func.GetterFunction;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * @author Zephyr
 */
@Slf4j
public class LambdaUtil {

    public static <T> String getUnderlineFieldNameByGetter(GetterFunction<T, ?> getterFunc) {
        String methodName = getMethodName(getSerializedLambda(getterFunc));
        return FieldUtil.toUnderlineCase(methodName);
    }

    public static <T> String getFiledNameByGetter(GetterFunction<T, ?> getterFunc) {
        String methodName = getMethodName(getSerializedLambda(getterFunc));
        return FieldUtil.lowerFirst(methodName);
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> getEntityClass(GetterFunction<T, ?> getterFunc) {
        String instantiatedMethodType = getSerializedLambda(getterFunc).getInstantiatedMethodType();
        String instantiatedType = instantiatedMethodType.substring(2, instantiatedMethodType.indexOf(";")).replace("/", ".");
        return (Class<T>) ClassUtil.toClassConfident(instantiatedType, getterFunc.getClass().getClassLoader());
    }

    private static <T> SerializedLambda getSerializedLambda(GetterFunction<T, ?> getterFunc) {
        try {
            Method method = getterFunc.getClass().getDeclaredMethod("writeReplace");
            method.setAccessible(true);
            return (SerializedLambda) method.invoke(getterFunc);
        } catch (Exception e) {
            log.error("LambdaUtil#getFiledNameByGetterMethod error:{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static <T> @NotNull String getMethodName(SerializedLambda getterFunc) {
        String methodName = getterFunc.getImplMethodName();
        return FieldUtil.getFieldNameByMethod(methodName);
    }
}
