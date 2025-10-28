package com.zephyrcicd.tdengineorm.entity;

import com.zephyrcicd.tdengineorm.annotation.TdTable;
import com.zephyrcicd.tdengineorm.annotation.TdTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * 传感器数据实体类 - 用于测试示例
 *
 * <p>对应TDengine超级表结构：</p>
 * <pre>
 * CREATE STABLE sensor_data (
 *     ts TIMESTAMP,
 *     temperature DOUBLE,
 *     humidity DOUBLE
 * ) TAGS (
 *     device_id NCHAR(50),
 *     location NCHAR(100)
 * );
 * </pre>
 *
 * @author Zephyr
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TdTable("sensor_data")
public class SensorData {

    /**
     * 设备ID - TAG字段
     */
    @TdTag
    private String deviceId;

    /**
     * 设备位置 - TAG字段
     */
    @TdTag
    private String location;

    /**
     * 温度
     */
    private Double temperature;

    /**
     * 湿度
     */
    private Double humidity;

    /**
     * 时间戳
     */
    private Timestamp ts;
}
