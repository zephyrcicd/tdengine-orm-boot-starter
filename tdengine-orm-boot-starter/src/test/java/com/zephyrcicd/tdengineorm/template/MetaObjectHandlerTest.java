package com.zephyrcicd.tdengineorm.template;

import com.zephyrcicd.tdengineorm.entity.SensorData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MetaObjectHandler测试类
 */
class MetaObjectHandlerTest {

    @Test
    void testTsMetaObjectHandlerWithEntity() {
        TsMetaObjectHandler handler = new TsMetaObjectHandler();
        
        // 测试实体类ts字段填充
        SensorData sensorData = SensorData.builder()
                .deviceId("device_001")
                .location("location_001")
                .temperature(25.0)
                .humidity(60.0)
                // ts字段故意留空
                .build();
        
        assertNull(sensorData.getTs());
        
        // 执行填充
        handler.insertFill(sensorData);
        
        // 验证ts字段被填充
        assertNotNull(sensorData.getTs());
        assertTrue(sensorData.getTs() > 0);
    }

    @Test
    void testTsMetaObjectHandlerWithMap() {
        TsMetaObjectHandler handler = new TsMetaObjectHandler();
        
        // 测试Map的ts字段填充
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("device_id", "device_001");
        dataMap.put("location", "location_001");
        dataMap.put("temperature", 25.0);
        dataMap.put("humidity", 60.0);
        // ts字段故意留空
        
        assertFalse(dataMap.containsKey("ts"));
        
        // 执行填充
        handler.insertFill(dataMap);
        
        // 验证ts字段被填充
        assertTrue(dataMap.containsKey("ts"));
        assertNotNull(dataMap.get("ts"));
        assertTrue((Long) dataMap.get("ts") > 0);
    }

    @Test
    void testTsMetaObjectHandlerWithExistingTs() {
        TsMetaObjectHandler handler = new TsMetaObjectHandler();
        
        // 测试已有ts字段不会被覆盖
        long existingTs = System.currentTimeMillis() - 10000;
        
        SensorData sensorData = SensorData.builder()
                .deviceId("device_001")
                .location("location_001")
                .temperature(25.0)
                .humidity(60.0)
                .ts(existingTs) // 提前设置ts字段
                .build();
        
        assertEquals(existingTs, sensorData.getTs());
        
        // 执行填充
        handler.insertFill(sensorData);
        
        // 验证ts字段未被改变
        assertEquals(existingTs, sensorData.getTs());
    }
    
    /**
     * 测试Long类型时间戳
     */
    static class LongEntity {
        private Long ts;
        
        public Long getTs() { return ts; }
        public void setTs(Long ts) { this.ts = ts; }
    }
    
    @Test
    void testTsMetaObjectHandlerWithLongType() {
        TsMetaObjectHandler handler = new TsMetaObjectHandler();
        
        LongEntity longEntity = new LongEntity();
        
        // 执行填充前ts字段为null
        assertNull(longEntity.getTs(), "ts should be null initially");
        
        // 执行填充
        handler.insertFill(longEntity);
        
        // 验证ts字段被正确填充
        assertNotNull(longEntity.getTs(), "ts should not be null after insertFill");
        assertTrue(longEntity.getTs() > 0, "ts should be greater than 0");
    }
    
    /**
     * 测试Date类型时间戳
     */
    static class DateEntity {
        private Date ts;
        
        public Date getTs() { return ts; }
        public void setTs(Date ts) { this.ts = ts; }
    }
    
    @Test
    void testTsMetaObjectHandlerWithDateType() {
        TsMetaObjectHandler handler = new TsMetaObjectHandler();
        
        DateEntity dateEntity = new DateEntity();
        
        // 执行填充前ts字段为null
        assertNull(dateEntity.getTs(), "ts should be null initially");
        
        // 执行填充
        handler.insertFill(dateEntity);
        
        // 验证ts字段被正确填充
        assertNotNull(dateEntity.getTs(), "ts should not be null after insertFill");
        assertTrue(dateEntity.getTs().getTime() > 0, "ts should be greater than 0");
    }
    
    /**
     * 测试LocalDateTime类型时间戳
     */
    static class LocalDateTimeEntity {
        private LocalDateTime ts;
        
        public LocalDateTime getTs() { return ts; }
        public void setTs(LocalDateTime ts) { this.ts = ts; }
    }
    
    @Test
    void testTsMetaObjectHandlerWithLocalDateTimeType() {
        TsMetaObjectHandler handler = new TsMetaObjectHandler();
        
        LocalDateTimeEntity localDateTimeEntity = new LocalDateTimeEntity();
        
        // 执行填充前ts字段为null
        assertNull(localDateTimeEntity.getTs(), "ts should be null initially");
        
        // 执行填充
        handler.insertFill(localDateTimeEntity);
        
        // 验证ts字段被正确填充
        assertNotNull(localDateTimeEntity.getTs(), "ts should not be null after insertFill");
    }
    
    /**
     * 测试LocalDate类型时间戳
     */
    static class LocalDateEntity {
        private LocalDate ts;
        
        public LocalDate getTs() { return ts; }
        public void setTs(LocalDate ts) { this.ts = ts; }
    }
    
    @Test
    void testTsMetaObjectHandlerWithLocalDateType() {
        TsMetaObjectHandler handler = new TsMetaObjectHandler();
        
        LocalDateEntity localDateEntity = new LocalDateEntity();
        
        // 执行填充前ts字段为null
        assertNull(localDateEntity.getTs(), "ts should be null initially");
        
        // 执行填充
        handler.insertFill(localDateEntity);
        
        // 验证ts字段被正确填充
        assertNotNull(localDateEntity.getTs(), "ts should not be null after insertFill");
    }
    
    /**
     * 测试Instant类型时间戳
     */
    static class InstantEntity {
        private Instant ts;
        
        public Instant getTs() { return ts; }
        public void setTs(Instant ts) { this.ts = ts; }
    }
    
    @Test
    void testTsMetaObjectHandlerWithInstantType() {
        TsMetaObjectHandler handler = new TsMetaObjectHandler();
        
        InstantEntity instantEntity = new InstantEntity();
        
        // 执行填充前ts字段为null
        assertNull(instantEntity.getTs(), "ts should be null initially");
        
        // 执行填充
        handler.insertFill(instantEntity);
        
        // 验证ts字段被正确填充
        assertNotNull(instantEntity.getTs(), "ts should not be null after insertFill");
        assertTrue(instantEntity.getTs().toEpochMilli() > 0, "ts should be greater than 0");
    }
}