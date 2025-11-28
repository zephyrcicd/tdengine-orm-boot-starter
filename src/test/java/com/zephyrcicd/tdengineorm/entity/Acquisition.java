package com.zephyrcicd.tdengineorm.entity;

import com.zephyrcicd.tdengineorm.annotation.TdColumn;
import com.zephyrcicd.tdengineorm.annotation.TdTable;
import com.zephyrcicd.tdengineorm.annotation.TdTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Acquisition 表实体类 - 对应 iot_data.acquisition 超级表
 * 用于测试 tag 顺序提取
 *
 * @author zjarlin
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TdTable("acquisition")
public class Acquisition {

    /**
     * 时间戳（主键）
     */
    @TdColumn("ts")
    private Long ts;

    /**
     * 采集值
     */
    @TdColumn("value")
    private Double value;

    /**
     * 能源类型代码 - TAG
     */
    @TdTag
    @TdColumn("energy_type_code")
    private String energyTypeCode;

    /**
     * 产品ID - TAG
     */
    @TdTag
    @TdColumn("product_id")
    private String productId;

    /**
     * 设备ID - TAG
     */
    @TdTag
    @TdColumn("device_id")
    private String deviceId;
}
