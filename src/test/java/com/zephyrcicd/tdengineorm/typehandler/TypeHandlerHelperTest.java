package com.zephyrcicd.tdengineorm.typehandler;

import com.zephyrcicd.tdengineorm.annotation.TdTypeHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TypeHandlerHelper 单元测试
 */
class TypeHandlerHelperTest {

    @BeforeEach
    void setUp() {
        TypeHandlerHelper.clearCache();
    }

    @Test
    void testToSqlValueWithSimpleTypes() throws NoSuchFieldException {
        Field stringField = TestEntity.class.getDeclaredField("name");
        Field intField = TestEntity.class.getDeclaredField("age");

        assertEquals("hello", TypeHandlerHelper.toSqlValue(stringField, "hello"));
        assertEquals(25, TypeHandlerHelper.toSqlValue(intField, 25));
        assertNull(TypeHandlerHelper.toSqlValue(stringField, null));
    }

    @Test
    void testToSqlValueWithComplexType() throws NoSuchFieldException {
        Field mapField = TestEntity.class.getDeclaredField("attributes");

        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        Object result = TypeHandlerHelper.toSqlValue(mapField, map);
        assertNotNull(result);
        assertTrue(result instanceof String, "Map should be serialized to JSON string");
        assertTrue(((String) result).contains("\"key\""));
    }

    @Test
    void testToSqlValueWithAnnotatedField() throws NoSuchFieldException {
        Field configField = TestEntity.class.getDeclaredField("config");

        TestConfig config = new TestConfig();
        config.setEnabled(true);
        config.setTimeout(30);

        Object result = TypeHandlerHelper.toSqlValue(configField, config);
        assertNotNull(result);
        assertTrue(result instanceof String, "Config should be serialized to JSON");
        assertTrue(((String) result).contains("\"enabled\""));
    }

    @Test
    void testFromSqlValueWithSimpleTypes() throws NoSuchFieldException {
        Field stringField = TestEntity.class.getDeclaredField("name");
        Field intField = TestEntity.class.getDeclaredField("age");

        assertEquals("hello", TypeHandlerHelper.fromSqlValue(stringField, "hello"));
        assertEquals(25, TypeHandlerHelper.fromSqlValue(intField, 25));
        assertNull(TypeHandlerHelper.fromSqlValue(stringField, null));
    }

    @Test
    void testFromSqlValueWithMapField() throws NoSuchFieldException {
        Field mapField = TestEntity.class.getDeclaredField("attributes");

        String json = "{\"key\":\"value\",\"count\":123}";
        Object result = TypeHandlerHelper.fromSqlValue(mapField, json);

        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("value", map.get("key"));
    }

    @Test
    void testGetHandlerWithAnnotation() throws NoSuchFieldException {
        Field configField = TestEntity.class.getDeclaredField("config");
        TypeHandler<?> handler = TypeHandlerHelper.getHandler(configField);

        assertNotNull(handler);
        assertTrue(handler instanceof TestConfigTypeHandler);
    }

    @Test
    void testGetHandlerFromRegistry() throws NoSuchFieldException {
        Field nameField = TestEntity.class.getDeclaredField("name");
        TypeHandler<?> handler = TypeHandlerHelper.getHandler(nameField);

        assertNotNull(handler);
        assertTrue(handler instanceof StringTypeHandler);
    }

    @Test
    void testCacheWorks() throws NoSuchFieldException {
        Field nameField = TestEntity.class.getDeclaredField("name");

        TypeHandler<?> handler1 = TypeHandlerHelper.getHandler(nameField);
        TypeHandler<?> handler2 = TypeHandlerHelper.getHandler(nameField);

        assertSame(handler1, handler2, "Same handler instance should be returned from cache");
    }

    @Test
    void testObjectFieldUsesObjectTypeHandler() throws NoSuchFieldException {
        Field payloadField = TestEntity.class.getDeclaredField("payload");

        // 复杂对象
        TestConfig config = new TestConfig();
        config.setEnabled(true);
        config.setTimeout(60);

        Object result = TypeHandlerHelper.toSqlValue(payloadField, config);
        assertNotNull(result);
        assertTrue(result instanceof String);
        assertTrue(((String) result).contains("enabled"));

        // 简单类型
        assertEquals("simple", TypeHandlerHelper.toSqlValue(payloadField, "simple"));
        assertEquals("123", TypeHandlerHelper.toSqlValue(payloadField, 123));
    }

    // 测试实体类
    static class TestEntity {
        private String name;
        private Integer age;
        private Map<String, Object> attributes;
        private Object payload;

        @TdTypeHandler(TestConfigTypeHandler.class)
        private TestConfig config;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public Map<String, Object> getAttributes() { return attributes; }
        public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
        public TestConfig getConfig() { return config; }
        public void setConfig(TestConfig config) { this.config = config; }
        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }
    }

    // 测试配置类
    static class TestConfig {
        private boolean enabled;
        private int timeout;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }
    }

    // 自定义TypeHandler用于测试
    public static class TestConfigTypeHandler extends JsonTypeHandler<TestConfig> {
        public TestConfigTypeHandler() {
            super(TestConfig.class);
        }
    }
}
