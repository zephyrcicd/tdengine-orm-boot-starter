package com.zephyrcicd.tdengineorm.typehandler;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型处理器注册表
 * <p>
 * 管理Java类型与TypeHandler的映射关系，支持自定义类型处理器注册。
 * 使用单例模式，线程安全。
 * </p>
 *
 * @author zjarlin
 * @since 2.4.0
 */
@Slf4j
public final class TypeHandlerRegistry {

    private static final TypeHandlerRegistry INSTANCE = new TypeHandlerRegistry();

    private final Map<Class<?>, TypeHandler<?>> typeHandlerMap = new ConcurrentHashMap<>();
    private final Map<String, TypeHandler<?>> namedHandlerMap = new ConcurrentHashMap<>();

    private TypeHandlerRegistry() {
        registerDefaultHandlers();
    }

    public static TypeHandlerRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 注册默认类型处理器
     */
    private void registerDefaultHandlers() {
        register(new StringTypeHandler());
        register(new IntegerTypeHandler());
        register(new LongTypeHandler());
        register(new DoubleTypeHandler());
        register(new FloatTypeHandler());
        register(new BooleanTypeHandler());
        register(new ByteArrayTypeHandler());
        register(new TimestampTypeHandler());
        register(new JsonMapTypeHandler());
        register(new ObjectTypeHandler());
    }

    /**
     * 注册类型处理器（按类型自动识别）
     */
    public <T> void register(BaseTypeHandler<T> handler) {
        Class<T> rawType = handler.getRawType();
        typeHandlerMap.put(rawType, handler);
        if (log.isDebugEnabled()) {
            log.debug("Registered TypeHandler for type: {}", rawType.getName());
        }
    }

    /**
     * 注册类型处理器（按名称）
     */
    public void register(String name, TypeHandler<?> handler) {
        namedHandlerMap.put(name, handler);
        if (log.isDebugEnabled()) {
            log.debug("Registered named TypeHandler: {}", name);
        }
    }

    /**
     * 注册类型处理器（按类型）
     */
    public <T> void register(Class<T> javaType, TypeHandler<T> handler) {
        typeHandlerMap.put(javaType, handler);
        if (log.isDebugEnabled()) {
            log.debug("Registered TypeHandler for type: {}", javaType.getName());
        }
    }

    /**
     * 获取类型处理器
     */
    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> getHandler(Class<T> javaType) {
        return (TypeHandler<T>) typeHandlerMap.get(javaType);
    }

    /**
     * 按名称获取处理器
     */
    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> getHandler(String name) {
        return (TypeHandler<T>) namedHandlerMap.get(name);
    }

    /**
     * 检查是否存在处理器
     */
    public boolean hasHandler(Class<?> javaType) {
        return typeHandlerMap.containsKey(javaType);
    }

    /**
     * 清除所有已注册的处理器
     */
    public void clear() {
        typeHandlerMap.clear();
        namedHandlerMap.clear();
        registerDefaultHandlers();
    }

    /**
     * 从MyBatis TypeHandler批量注册（可变参数）
     * <p>
     * 将已有的MyBatis TypeHandler适配并注册到本框架中。
     * </p>
     *
     * <pre>
     * TypeHandlerRegistry.getInstance().fromMybatis(
     *     new JsonTypeHandler(),
     *     new EnumTypeHandler(Status.class),
     *     new CustomTypeHandler()
     * );
     * </pre>
     *
     * @param mybatisHandlers MyBatis TypeHandler数组
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void fromMybatis(org.apache.ibatis.type.TypeHandler<?>... mybatisHandlers) {
        if (mybatisHandlers == null) {
            return;
        }
        Arrays.stream(mybatisHandlers)
                .forEach(handler -> {
                    MybatisTypeHandlerAdapter adapter = new MybatisTypeHandlerAdapter(handler);
                    Class javaType = adapter.getJavaType();
                    if (javaType != null && javaType != Object.class) {
                        typeHandlerMap.put(javaType, adapter);
                        if (log.isDebugEnabled()) {
                            log.debug("Registered MyBatis TypeHandler for type: {}", javaType.getName());
                        }
                    }
                });
    }

    /**
     * 从MyBatis TypeHandler批量注册（Collection）
     *
     * @param mybatisHandlers MyBatis TypeHandler集合
     */
    @SuppressWarnings("rawtypes")
    public void fromMybatis(Collection<org.apache.ibatis.type.TypeHandler<?>> mybatisHandlers) {
        if (mybatisHandlers == null) {
            return;
        }
        fromMybatis(mybatisHandlers.toArray(new org.apache.ibatis.type.TypeHandler[0]));
    }

    /**
     * 从MyBatis TypeHandler注册并指定Java类型
     *
     * @param javaType       Java类型
     * @param mybatisHandler MyBatis TypeHandler
     */
    public <T> void fromMybatis(Class<T> javaType, org.apache.ibatis.type.TypeHandler<T> mybatisHandler) {
        TypeHandler<T> adapter = MybatisTypeHandlerAdapter.wrap(mybatisHandler, javaType);
        typeHandlerMap.put(javaType, adapter);
        if (log.isDebugEnabled()) {
            log.debug("Registered MyBatis TypeHandler for type: {}", javaType.getName());
        }
    }

    /**
     * 从MyBatis TypeHandler注册并指定名称
     *
     * @param name           处理器名称
     * @param mybatisHandler MyBatis TypeHandler
     */
    public void fromMybatis(String name, org.apache.ibatis.type.TypeHandler<?> mybatisHandler) {
        TypeHandler<?> adapter = MybatisTypeHandlerAdapter.wrap(mybatisHandler);
        namedHandlerMap.put(name, adapter);
        if (log.isDebugEnabled()) {
            log.debug("Registered named MyBatis TypeHandler: {}", name);
        }
    }
}
