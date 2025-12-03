package com.zephyrcicd.tdengineorm.typehandler;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 各种TypeHandler单元测试
 *
 * @author zjarlin
 * @since 2.4.0
 */
class TypeHandlerTest {

    // ========== StringTypeHandler ==========
    @Test
    void testStringTypeHandler() {
        StringTypeHandler handler = new StringTypeHandler();

        assertEquals("hello", handler.toSqlValue("hello"));
        assertEquals("hello", handler.fromSqlValue("hello"));
        assertNull(handler.toSqlValue(null));
        assertNull(handler.fromSqlValue(null));
        assertEquals("123", handler.fromSqlValue(123));
    }

    // ========== IntegerTypeHandler ==========
    @Test
    void testIntegerTypeHandler() {
        IntegerTypeHandler handler = new IntegerTypeHandler();

        assertEquals(42, handler.toSqlValue(42));
        assertEquals(42, handler.fromSqlValue(42));
        assertEquals(42, handler.fromSqlValue("42"));
        assertEquals(42, handler.fromSqlValue(42L));
        assertNull(handler.fromSqlValue(null));
    }

    // ========== LongTypeHandler ==========
    @Test
    void testLongTypeHandler() {
        LongTypeHandler handler = new LongTypeHandler();

        assertEquals(123456789L, handler.toSqlValue(123456789L));
        assertEquals(123456789L, handler.fromSqlValue(123456789L));
        assertEquals(123L, handler.fromSqlValue(123));
        assertEquals(123L, handler.fromSqlValue("123"));
        assertNull(handler.fromSqlValue(null));
    }

    // ========== DoubleTypeHandler ==========
    @Test
    void testDoubleTypeHandler() {
        DoubleTypeHandler handler = new DoubleTypeHandler();

        assertEquals(3.14, handler.toSqlValue(3.14));
        assertEquals(3.14, handler.fromSqlValue(3.14));
        assertEquals(3.0, handler.fromSqlValue(3));
        assertEquals(3.14, handler.fromSqlValue("3.14"));
        assertNull(handler.fromSqlValue(null));
    }

    // ========== BooleanTypeHandler ==========
    @Test
    void testBooleanTypeHandler() {
        BooleanTypeHandler handler = new BooleanTypeHandler();

        assertEquals(true, handler.toSqlValue(true));
        assertEquals(true, handler.fromSqlValue(true));
        assertEquals(true, handler.fromSqlValue("true"));
        assertEquals(true, handler.fromSqlValue(1));
        assertEquals(false, handler.fromSqlValue("false"));
        assertEquals(false, handler.fromSqlValue(0));
        assertNull(handler.fromSqlValue(null));
    }

    // ========== TimestampTypeHandler ==========
    @Test
    void testTimestampTypeHandler() {
        TimestampTypeHandler handler = new TimestampTypeHandler();

        long now = System.currentTimeMillis();
        Timestamp ts = new Timestamp(now);

        assertEquals(ts, handler.toSqlValue(ts));
        assertEquals(ts, handler.fromSqlValue(ts));
        assertEquals(ts, handler.fromSqlValue(now));
        assertNull(handler.fromSqlValue(null));
    }

    // ========== ByteArrayTypeHandler ==========
    @Test
    void testByteArrayTypeHandler() {
        ByteArrayTypeHandler handler = new ByteArrayTypeHandler();

        byte[] bytes = {1, 2, 3, 4, 5};
        assertArrayEquals(bytes, (byte[]) handler.toSqlValue(bytes));
        assertArrayEquals(bytes, handler.fromSqlValue(bytes));
        assertArrayEquals("hello".getBytes(), handler.fromSqlValue("hello"));
        assertNull(handler.fromSqlValue(null));
    }

    // ========== JsonMapTypeHandler ==========
    @Test
    void testJsonMapTypeHandler() {
        JsonMapTypeHandler handler = new JsonMapTypeHandler();

        Map<String, Object> map = new HashMap<>();
        map.put("name", "test");
        map.put("value", 123);

        String json = (String) handler.toSqlValue(map);
        assertNotNull(json);
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"test\""));

        Map<String, Object> result = handler.fromSqlValue(json);
        assertNotNull(result);
        assertEquals("test", result.get("name"));
        assertEquals(123, result.get("value"));

        assertNull(handler.fromSqlValue(null));
        assertNull(handler.fromSqlValue(""));
    }

    // ========== ObjectTypeHandler ==========
    @Test
    void testObjectTypeHandler() {
        ObjectTypeHandler handler = new ObjectTypeHandler();

        // 基础类型直接toString
        assertEquals("hello", handler.toSqlValue("hello"));
        assertEquals("123", handler.toSqlValue(123));
        assertEquals("true", handler.toSqlValue(true));

        // 复杂对象序列化为JSON
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        String json = (String) handler.toSqlValue(map);
        assertTrue(json.contains("\"key\""));
        assertTrue(json.contains("\"value\""));

        assertNull(handler.toSqlValue(null));
    }

    // ========== JsonTypeHandler ==========
    @Test
    void testJsonTypeHandler() {
        JsonTypeHandler<TestPojo> handler = new JsonTypeHandler<>(TestPojo.class);

        TestPojo pojo = new TestPojo();
        pojo.setName("test");
        pojo.setAge(25);

        String json = (String) handler.toSqlValue(pojo);
        assertNotNull(json);
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"test\""));

        TestPojo result = handler.fromSqlValue(json);
        assertNotNull(result);
        assertEquals("test", result.getName());
        assertEquals(25, result.getAge());

        assertNull(handler.fromSqlValue(null));
    }

    // ========== EnumTypeHandler ==========
    @Test
    void testEnumTypeHandler() {
        EnumTypeHandler<TestEnum> handler = new EnumTypeHandler<>(TestEnum.class);

        assertEquals("ACTIVE", handler.toSqlValue(TestEnum.ACTIVE));
        assertEquals(TestEnum.ACTIVE, handler.fromSqlValue("ACTIVE"));
        assertEquals(TestEnum.INACTIVE, handler.fromSqlValue("INACTIVE"));
        assertNull(handler.fromSqlValue(null));
    }

    // ========== EnumOrdinalTypeHandler ==========
    @Test
    void testEnumOrdinalTypeHandler() {
        EnumOrdinalTypeHandler<TestEnum> handler = new EnumOrdinalTypeHandler<>(TestEnum.class);

        assertEquals(0, handler.toSqlValue(TestEnum.ACTIVE));
        assertEquals(1, handler.toSqlValue(TestEnum.INACTIVE));
        assertEquals(TestEnum.ACTIVE, handler.fromSqlValue(0));
        assertEquals(TestEnum.INACTIVE, handler.fromSqlValue(1));
        assertNull(handler.fromSqlValue(null));
    }

    // ========== ListTypeHandler ==========
    @Test
    void testListTypeHandler() {
        ListTypeHandler<String> handler = new ListTypeHandler<>(String.class);

        List<String> list = Arrays.asList("a", "b", "c");
        String json = (String) handler.toSqlValue(list);
        assertNotNull(json);
        assertTrue(json.contains("\"a\""));

        List<String> result = handler.fromSqlValue(json);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));

        assertNull(handler.fromSqlValue(null));
    }

    // 测试用的POJO
    static class TestPojo {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    // 测试用的枚举
    enum TestEnum {
        ACTIVE, INACTIVE
    }
}
