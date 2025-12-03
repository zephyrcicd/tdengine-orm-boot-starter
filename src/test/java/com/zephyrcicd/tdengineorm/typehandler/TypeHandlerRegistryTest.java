package com.zephyrcicd.tdengineorm.typehandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TypeHandlerRegistry 单元测试
 *
 * @author zjarlin
 * @since 2.4.0
 */
class TypeHandlerRegistryTest {

    private TypeHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = TypeHandlerRegistry.getInstance();
    }

    @Test
    void testGetInstance() {
        TypeHandlerRegistry instance1 = TypeHandlerRegistry.getInstance();
        TypeHandlerRegistry instance2 = TypeHandlerRegistry.getInstance();
        assertSame(instance1, instance2, "Registry should be singleton");
    }

    @Test
    void testDefaultHandlersRegistered() {
        assertNotNull(registry.getHandler(String.class), "StringTypeHandler should be registered");
        assertNotNull(registry.getHandler(Integer.class), "IntegerTypeHandler should be registered");
        assertNotNull(registry.getHandler(Long.class), "LongTypeHandler should be registered");
        assertNotNull(registry.getHandler(Double.class), "DoubleTypeHandler should be registered");
        assertNotNull(registry.getHandler(Float.class), "FloatTypeHandler should be registered");
        assertNotNull(registry.getHandler(Boolean.class), "BooleanTypeHandler should be registered");
        assertNotNull(registry.getHandler(Timestamp.class), "TimestampTypeHandler should be registered");
        assertNotNull(registry.getHandler(byte[].class), "ByteArrayTypeHandler should be registered");
        assertNotNull(registry.getHandler(Map.class), "JsonMapTypeHandler should be registered");
        assertNotNull(registry.getHandler(Object.class), "ObjectTypeHandler should be registered");
    }

    @Test
    void testHasHandler() {
        assertTrue(registry.hasHandler(String.class));
        assertTrue(registry.hasHandler(Integer.class));
        // CustomType可能被其他测试注册，所以这里只测试基本类型
    }

    @Test
    void testRegisterCustomHandler() {
        CustomTypeHandler customHandler = new CustomTypeHandler();
        registry.register(CustomType.class, customHandler);

        assertTrue(registry.hasHandler(CustomType.class));
        assertSame(customHandler, registry.getHandler(CustomType.class));
    }

    @Test
    void testRegisterNamedHandler() {
        StringTypeHandler handler = new StringTypeHandler();
        registry.register("myStringHandler", handler);

        TypeHandler<String> retrieved = registry.getHandler("myStringHandler");
        assertSame(handler, retrieved);
    }

    @Test
    void testClear() {
        registry.register(CustomType.class, new CustomTypeHandler());
        assertTrue(registry.hasHandler(CustomType.class));

        registry.clear();

        assertFalse(registry.hasHandler(CustomType.class));
        assertTrue(registry.hasHandler(String.class), "Default handlers should be re-registered");
    }

    // 自定义类型用于测试
    static class CustomType {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    // 自定义TypeHandler用于测试
    static class CustomTypeHandler implements TypeHandler<CustomType> {
        @Override
        public void setParameter(java.sql.PreparedStatement ps, int index, CustomType parameter) {
        }

        @Override
        public CustomType getResult(java.sql.ResultSet rs, String columnName) {
            return null;
        }

        @Override
        public CustomType getResult(java.sql.ResultSet rs, int columnIndex) {
            return null;
        }

        @Override
        public Object toSqlValue(CustomType value) {
            return value != null ? value.getValue() : null;
        }

        @Override
        public CustomType fromSqlValue(Object sqlValue) {
            CustomType ct = new CustomType();
            ct.setValue(sqlValue != null ? sqlValue.toString() : null);
            return ct;
        }
    }
}
