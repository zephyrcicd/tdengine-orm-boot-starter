package com.zephyrcicd.tdengineorm.template;

import com.zephyrcicd.tdengineorm.entity.SensorData;
import com.zephyrcicd.tdengineorm.strategy.EntityTableNameStrategy;
import com.zephyrcicd.tdengineorm.strategy.MapTableNameStrategy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * TdTemplate插入操作示例测试类
 *
 * <p>本测试类展示了TdTemplate的各种插入方法的使用示例</p>
 * <p>注意：这些测试用例仅作为使用示例，实际运行需要配置TDengine数据库连接</p>
 *
 * @author Zephyr
 */
@SpringBootTest
@DisplayName("TdTemplate插入操作使用示例")
public class TdTemplateInsertExampleTest {

    @Autowired
    private TdTemplate tdTemplate;

    /**
     * 创建超级表（在所有测试之前执行一次）
     * 使用TdTemplate的createStableTable方法根据实体类自动创建超级表
     */
    @BeforeAll
    public static void createSuperTable(@Autowired TdTemplate tdTemplate) {
        System.out.println("开始创建超级表...");

        // 使用TdTemplate的createStableTable方法创建超级表
        // 该方法会根据实体类的@TdTable和@TdTag注解自动生成CREATE STABLE语句
        int result = tdTemplate.createStableTable(SensorData.class);

        System.out.println("超级表 sensor_data 创建成功，影响行数: " + result);
        System.out.println("表结构：");
        System.out.println("  普通字段: ts(TIMESTAMP), temperature(DOUBLE), humidity(DOUBLE)");
        System.out.println("  TAG字段: device_id(NCHAR), location(NCHAR)");
    }

    // ==================== 基础插入示例 ====================

    @Test
    @DisplayName("示例1: 插入单条数据到普通表")
    public void example1_insertToNormalTable() {
        // 创建传感器数据
        SensorData data = SensorData.builder()
                .deviceId("device001")
                .location("Beijing")
                .temperature(25.5)
                .humidity(60.0)
                .ts(System.currentTimeMillis())
                .build();

        // 插入数据，使用实体类上@TdTable注解的表名
        int rows = tdTemplate.insert(data);
        System.out.println("插入影响行数: " + rows);
    }

    @Test
    @DisplayName("示例2: 插入单条数据到超级表（包含TAG字段）")
    public void example2_insertToSuperTable() {
        // 创建传感器数据，包含TAG字段值
        SensorData data = SensorData.builder()
                .deviceId("device001")      // TAG字段
                .location("Beijing")        // TAG字段
                .temperature(25.5)
                .humidity(60.0)
                .ts(System.currentTimeMillis())
                .build();

        // 插入到超级表，TAG字段会被自动处理
        int rows = tdTemplate.insert(data);
        System.out.println("插入影响行数: " + rows);
    }

    // ==================== 动态表名插入示例 ====================

    @Test
    @DisplayName("示例3: 使用策略模式动态生成子表名插入")
    public void example3_insertWithDynamicTableName() {
        // 创建传感器数据
        SensorData data = SensorData.builder()
                .deviceId("device001")
                .location("Beijing")
                .temperature(25.5)
                .humidity(60.0)
                .ts(System.currentTimeMillis())
                .build();

        // 定义表名策略：根据设备ID生成子表名
        EntityTableNameStrategy<SensorData> strategy = entity ->
                "sensor_" + entity.getDeviceId();

        // 使用策略插入，实际会插入到 sensor_device001 表
        int rows = tdTemplate.insert(strategy, data);
        System.out.println("插入到子表: sensor_" + data.getDeviceId());
        System.out.println("插入影响行数: " + rows);
    }

    @Test
    @DisplayName("示例4: 使用Map作为数据载体插入")
    public void example4_insertWithMap() {
        // 使用Map存储数据
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("temperature", 25.5);
        dataMap.put("humidity", 60.0);
        dataMap.put("ts", System.currentTimeMillis());

        // 定义表名策略：根据Map中的数据生成表名
        MapTableNameStrategy strategy = map -> {
            // 可以根据Map中的数据动态决定表名
            return "sensor_device001";
        };

        // 使用Map插入数据
        int rows = tdTemplate.insert(strategy, dataMap);
        System.out.println("插入影响行数: " + rows);
    }

    // ==================== USING语法插入示例 ====================

    @Test
    @DisplayName("示例5: 使用USING语法插入（自动创建子表）")
    public void example5_insertUsing() {
        // 创建传感器数据，包含TAG字段
        SensorData data = SensorData.builder()
                .deviceId("device001")      // TAG字段
                .location("Beijing")        // TAG字段
                .temperature(25.5)
                .humidity(60.0)
                .ts(System.currentTimeMillis())
                .build();

        // 定义子表名策略
        EntityTableNameStrategy<SensorData> strategy = entity ->
                "sensor_" + entity.getDeviceId();

        // 使用USING语法插入，如果子表不存在会自动创建
        // SQL示例: INSERT INTO sensor_device001 USING sensor_data TAGS('device001', 'Beijing') VALUES(...)
        int rows = tdTemplate.insertUsing(data, strategy);
        System.out.println("插入影响行数: " + rows);
    }

    // ==================== 批量插入示例 ====================

    @Test
    @DisplayName("示例6: 批量插入到不同子表（使用默认批次大小）")
    public void example6_batchInsertToDifferentTables() {
        // 创建多个设备的数据
        List<SensorData> dataList = new ArrayList<>();
        long baseTime = System.currentTimeMillis();

        // 设备1的数据
        for (int i = 0; i < 100; i++) {
            dataList.add(SensorData.builder()
                    .deviceId("device001")
                    .location("Beijing")
                    .temperature(25.0 + i * 0.1)
                    .humidity(60.0 + i * 0.1)
                    .ts(baseTime + i * 1000)  // 每秒递增
                    .build());
        }

        // 设备2的数据
        for (int i = 0; i < 100; i++) {
            dataList.add(SensorData.builder()
                    .deviceId("device002")
                    .location("Shanghai")
                    .temperature(26.0 + i * 0.1)
                    .humidity(65.0 + i * 0.1)
                    .ts(baseTime + i * 1000)  // 每秒递增
                    .build());
        }

        // 定义表名策略：根据设备ID生成子表名
        EntityTableNameStrategy<SensorData> strategy = entity ->
                "sensor_" + entity.getDeviceId();

        // 批量插入，会自动按表名分组
        // device001的数据会插入到sensor_device001表
        // device002的数据会插入到sensor_device002表
        int[] rows = tdTemplate.batchInsert(SensorData.class, dataList, strategy);
        System.out.println("批量插入完成，批次数: " + rows.length);
        System.out.println("总影响行数: " + Arrays.stream(rows).sum());
    }

    @Test
    @DisplayName("示例7: 批量插入到不同子表（自定义批次大小）")
    public void example7_batchInsertWithCustomPageSize() {
        // 创建大量数据（10000条）
        List<SensorData> largeDataList = new ArrayList<>();
        long baseTime = System.currentTimeMillis();

        for (int deviceNum = 1; deviceNum <= 5; deviceNum++) {
            for (int i = 0; i < 2000; i++) {
                largeDataList.add(SensorData.builder()
                        .deviceId("device" + String.format("%03d", deviceNum))
                        .location("Location" + deviceNum)
                        .temperature(25.0 + i * 0.01)
                        .humidity(60.0 + i * 0.01)
                        .ts(baseTime + i * 1000)  // 每秒递增
                        .build());
            }
        }

        // 定义表名策略
        EntityTableNameStrategy<SensorData> strategy = entity ->
                "sensor_" + entity.getDeviceId();

        // 批量插入，每500条一批
        // 数据会先按表名分组，然后每组内按500条分批插入
        int[] rows = tdTemplate.batchInsert(SensorData.class, largeDataList, 500, strategy);
        System.out.println("批量插入完成，批次数: " + rows.length);
        System.out.println("总影响行数: " + Arrays.stream(rows).sum());
    }

    // ==================== 批量USING插入示例 ====================

    @Test
    @DisplayName("示例8: 使用USING语法批量插入（默认批次大小）")
    public void example8_batchInsertUsing() {
        // 创建同一设备的多条数据
        List<SensorData> dataList = new ArrayList<>();
        long baseTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            dataList.add(SensorData.builder()
                    .deviceId("device001")      // 所有数据的TAG值相同
                    .location("Beijing")        // 所有数据的TAG值相同
                    .temperature(25.0 + i * 0.1)
                    .humidity(60.0 + i * 0.1)
                    .ts(baseTime + i * 1000)  // 每秒递增
                    .build());
        }

        // 使用默认表名策略
        int[] rows = tdTemplate.batchInsertUsing(SensorData.class, dataList);
        System.out.println("批量插入完成，批次数: " + rows.length);
        System.out.println("总影响行数: " + Arrays.stream(rows).sum());
    }

    @Test
    @DisplayName("示例9: 使用USING语法批量插入（自定义表名策略）")
    public void example9_batchInsertUsingWithStrategy() {
        // 创建同一设备的多条数据
        List<SensorData> dataList = new ArrayList<>();
        long baseTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            dataList.add(SensorData.builder()
                    .deviceId("device001")      // TAG字段
                    .location("Beijing")        // TAG字段
                    .temperature(25.0 + i * 0.1)
                    .humidity(60.0 + i * 0.1)
                    .ts(baseTime + i * 1000)  // 每秒递增
                    .build());
        }

        // 定义子表名策略
        EntityTableNameStrategy<SensorData> strategy = entity ->
                "sensor_" + entity.getDeviceId();

        // 批量插入，使用自定义表名策略
        int[] rows = tdTemplate.batchInsertUsing(SensorData.class, dataList, strategy);
        System.out.println("批量插入完成，批次数: " + rows.length);
        System.out.println("总影响行数: " + Arrays.stream(rows).sum());
    }

    @Test
    @DisplayName("示例10: 使用USING语法批量插入（自定义批次大小）")
    public void example10_batchInsertUsingWithCustomPageSize() {
        // 创建同一设备的大量历史数据（10000条）
        List<SensorData> largeDataList = new ArrayList<>();
        long baseTime = System.currentTimeMillis();

        for (int i = 0; i < 10000; i++) {
            largeDataList.add(SensorData.builder()
                    .deviceId("device001")      // TAG字段
                    .location("Beijing")        // TAG字段
                    .temperature(25.0 + i * 0.01)
                    .humidity(60.0 + i * 0.01)
                    .ts(baseTime + i * 1000)  // 每秒递增
                    .build());
        }

        // 定义子表名策略
        EntityTableNameStrategy<SensorData> strategy = entity ->
                "sensor_" + entity.getDeviceId();

        // 批量插入，每500条一批
        int[] rows = tdTemplate.batchInsertUsing(SensorData.class, largeDataList, 500, strategy);
        System.out.println("批量插入完成，批次数: " + rows.length);
        System.out.println("总影响行数: " + Arrays.stream(rows).sum());
    }

    // ==================== 复杂场景示例 ====================

    @Test
    @DisplayName("示例11: 根据时间动态分表插入")
    public void example11_insertWithTimeBasedPartition() {
        // 创建传感器数据
        long currentTimeMillis = System.currentTimeMillis();
        SensorData data = SensorData.builder()
                .deviceId("device001")
                .location("Beijing")
                .temperature(25.5)
                .humidity(60.0)
                .ts(currentTimeMillis)
                .build();

        // 定义基于时间的分表策略：按月分表
        EntityTableNameStrategy<SensorData> strategy = entity -> {
            // 将Long类型的时间戳转换为LocalDateTime
            LocalDateTime dateTime = Instant.ofEpochMilli(entity.getTs())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            String yearMonth = String.format("%04d%02d",
                    dateTime.getYear(),
                    dateTime.getMonthValue());
            return "sensor_" + entity.getDeviceId() + "_" + yearMonth;
        };

        // 插入，实际表名类似: sensor_device001_202510
        int rows = tdTemplate.insert(strategy, data);
        System.out.println("插入影响行数: " + rows);
    }

    @Test
    @DisplayName("示例12: 使用Lambda表达式的表名策略")
    public void example12_insertWithLambdaStrategy() {
        // 创建多条数据
        List<SensorData> dataList = Arrays.asList(
                SensorData.builder()
                        .deviceId("device001")
                        .location("Beijing")
                        .temperature(25.5)
                        .humidity(60.0)
                        .ts(System.currentTimeMillis())
                        .build(),
                SensorData.builder()
                        .deviceId("device002")
                        .location("Shanghai")
                        .temperature(26.5)
                        .humidity(65.0)
                        .ts(System.currentTimeMillis())
                        .build()
        );

        // 使用Lambda表达式定义表名策略
        int[] rows = tdTemplate.batchInsert(
                SensorData.class,
                dataList,
                entity -> "sensor_" + entity.getDeviceId()  // Lambda表达式
        );

        System.out.println("批量插入完成，总影响行数: " + Arrays.stream(rows).sum());
    }
}
