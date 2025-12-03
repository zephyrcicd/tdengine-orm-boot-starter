package com.zephyrcicd.tdengineorm.typehandler;

import com.zephyrcicd.tdengineorm.annotation.TdPolymorphic;
import com.zephyrcicd.tdengineorm.annotation.TypeMapping;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多态TypeHandler单元测试
 */
class PolymorphicTypeHandlerTest {

    // ========== PolymorphicJsonTypeHandler ==========
    @Test
    void testPolymorphicJsonTypeHandlerWithMapping() {
        PolymorphicJsonTypeHandler handler = PolymorphicJsonTypeHandler.builder()
                .register("SENSOR", SensorData.class)
                .register("ALARM", AlarmData.class)
                .defaultType(BaseData.class)
                .build();

        // 测试类型解析
        assertEquals(SensorData.class, handler.resolveType("SENSOR"));
        assertEquals(AlarmData.class, handler.resolveType("ALARM"));
        assertEquals(BaseData.class, handler.resolveType("UNKNOWN"));
        assertEquals(BaseData.class, handler.resolveType(null));
    }

    @Test
    void testPolymorphicJsonTypeHandlerDeserialize() {
        PolymorphicJsonTypeHandler handler = PolymorphicJsonTypeHandler.builder()
                .register("SENSOR", SensorData.class)
                .register("ALARM", AlarmData.class)
                .build();

        String sensorJson = "{\"temperature\":25.5,\"humidity\":60.0}";
        Object result = handler.deserialize(sensorJson, "SENSOR");

        assertNotNull(result);
        assertTrue(result instanceof SensorData);
        SensorData sensor = (SensorData) result;
        assertEquals(25.5, sensor.getTemperature());
        assertEquals(60.0, sensor.getHumidity());
    }

    @Test
    void testPolymorphicJsonTypeHandlerWithResolver() {
        PolymorphicJsonTypeHandler handler = PolymorphicJsonTypeHandler.builder()
                .typeResolver(type -> {
                    if ("SENSOR".equals(type)) return SensorData.class;
                    if ("ALARM".equals(type)) return AlarmData.class;
                    return BaseData.class;
                })
                .build();

        assertEquals(SensorData.class, handler.resolveType("SENSOR"));
        assertEquals(AlarmData.class, handler.resolveType("ALARM"));
        assertEquals(BaseData.class, handler.resolveType("OTHER"));
    }

    // ========== PolymorphicFieldHandler ==========
    @Test
    void testPolymorphicFieldHandler() {
        PolymorphicFieldHandler handler = PolymorphicFieldHandler.builder()
                .typeColumn("type")
                .dataColumn("data_json")
                .register("SENSOR", SensorData.class)
                .register("ALARM", AlarmData.class)
                .defaultType(BaseData.class)
                .build();

        Map<String, Object> row = new HashMap<>();
        row.put("type", "SENSOR");
        row.put("data_json", "{\"temperature\":30.0,\"humidity\":55.0}");

        Object result = handler.deserializeFromMap(row);
        assertNotNull(result);
        assertTrue(result instanceof SensorData);
        assertEquals(30.0, ((SensorData) result).getTemperature());
    }

    // ========== PolymorphicTypeResolver ==========
    @Test
    void testPolymorphicTypeResolverWithAnnotation() throws NoSuchFieldException {
        Field dataField = EventEntity.class.getDeclaredField("data");

        PolymorphicTypeResolver.PolymorphicMeta meta = PolymorphicTypeResolver.getMeta(dataField);
        assertNotNull(meta);
        assertEquals("type", meta.getTypeFieldName());
        assertEquals("data", meta.getDataFieldName());
        assertEquals(SensorData.class, meta.resolveType("SENSOR"));
        assertEquals(AlarmData.class, meta.resolveType("ALARM"));
        assertEquals(BaseData.class, meta.resolveType("UNKNOWN"));
    }

    @Test
    void testPolymorphicTypeResolverDeserialize() throws NoSuchFieldException {
        Field dataField = EventEntity.class.getDeclaredField("data");

        String json = "{\"temperature\":28.0,\"humidity\":65.0}";
        Object result = PolymorphicTypeResolver.deserialize(dataField, json, "SENSOR");

        assertNotNull(result);
        assertTrue(result instanceof SensorData);
        assertEquals(28.0, ((SensorData) result).getTemperature());
    }

    @Test
    void testPolymorphicTypeResolverResolveFields() throws NoSuchFieldException {
        EventEntity entity = new EventEntity();
        entity.setType("ALARM");

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("type", "ALARM");
        jsonMap.put("data", "{\"level\":\"HIGH\",\"message\":\"Temperature too high\"}");

        PolymorphicTypeResolver.resolvePolymorphicFields(entity, jsonMap);

        assertNotNull(entity.getData());
        assertTrue(entity.getData() instanceof AlarmData);
        AlarmData alarm = (AlarmData) entity.getData();
        assertEquals("HIGH", alarm.getLevel());
        assertEquals("Temperature too high", alarm.getMessage());
    }

    // 测试实体类
    static class EventEntity {
        private String type;

        @TdPolymorphic(
                typeField = "type",
                mappings = {
                        @TypeMapping(type = "SENSOR", target = SensorData.class),
                        @TypeMapping(type = "ALARM", target = AlarmData.class)
                },
                defaultType = BaseData.class
        )
        private Object data;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
    }

    // 基础数据类
    static class BaseData {
        private String id;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    // 传感器数据类
    static class SensorData extends BaseData {
        private double temperature;
        private double humidity;

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }
        public double getHumidity() { return humidity; }
        public void setHumidity(double humidity) { this.humidity = humidity; }
    }

    // 告警数据类
    static class AlarmData extends BaseData {
        private String level;
        private String message;

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
