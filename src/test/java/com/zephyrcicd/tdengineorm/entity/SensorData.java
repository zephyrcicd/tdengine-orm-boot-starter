package com.zephyrcicd.tdengineorm.entity;

import com.zephyrcicd.tdengineorm.annotation.TdColumn;
import com.zephyrcicd.tdengineorm.annotation.TdTable;
import com.zephyrcicd.tdengineorm.annotation.TdTag;
import com.zephyrcicd.tdengineorm.enums.TdFieldTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
 * <p>注意：ts字段使用Long类型存储时间戳（毫秒）</p>
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
     * 时间戳
     */
    private Long ts;
    
    /**
     * 设备ID - TAG字段
     */
    @TdTag
    @TdColumn(length = 50, comment = "设备唯一标识")
    private String deviceId;

    /**
     * 设备位置 - TAG字段
     */
    @TdTag
    @TdColumn(value = "location", length = 100, comment = "设备部署位置")
    private String location;

    /**
     * 温度 - 自定义列名和类型
     */
    @TdColumn(value = "temp", type = TdFieldTypeEnum.DOUBLE, comment = "环境温度")
    private Double temperature;

    /**
     * 湿度 - 使用默认映射
     */
    private Double humidity;

    /**
     * 内部计算字段 - 不参与SQL生成
     */
    @TdColumn(exist = false)
    private String calculatedField;

}
