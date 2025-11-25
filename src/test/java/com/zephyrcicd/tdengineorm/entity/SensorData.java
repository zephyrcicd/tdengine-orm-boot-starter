package com.zephyrcicd.tdengineorm.entity;

/**
 * 传感器数据实体类 - 用于测试示例
 */
public class SensorData {
    
    /**
     * 时间戳
     */
    private Long ts;
    
    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 设备位置
     */
    private String location;

    /**
     * 温度
     */
    private Double temperature;

    /**
     * 湿度
     */
    private Double humidity;
    
    // Constructors
    public SensorData() {}
    
    public SensorData(String deviceId, String location, Double temperature, Double humidity, Long ts) {
        this.deviceId = deviceId;
        this.location = location;
        this.temperature = temperature;
        this.humidity = humidity;
        this.ts = ts;
    }
    
    // Getters and Setters
    public Long getTs() {
        return ts;
    }
    
    public void setTs(Long ts) {
        this.ts = ts;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    public Double getHumidity() {
        return humidity;
    }
    
    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }
    
    // Builder pattern methods
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Long ts;
        private String deviceId;
        private String location;
        private Double temperature;
        private Double humidity;
        
        public Builder ts(Long ts) {
            this.ts = ts;
            return this;
        }
        
        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }
        
        public Builder location(String location) {
            this.location = location;
            return this;
        }
        
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder humidity(Double humidity) {
            this.humidity = humidity;
            return this;
        }
        
        public SensorData build() {
            return new SensorData(deviceId, location, temperature, humidity, ts);
        }
    }
}