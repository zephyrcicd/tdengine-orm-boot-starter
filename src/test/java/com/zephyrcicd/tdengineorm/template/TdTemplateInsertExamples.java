package com.zephyrcicd.tdengineorm.template;

import com.zephyrcicd.tdengineorm.entity.SensorData;
import com.zephyrcicd.tdengineorm.strategy.EntityTableNameStrategy;
import com.zephyrcicd.tdengineorm.strategy.MapTableNameStrategy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * TdTemplate插入操作使用示例代码
 *
 * <p>本类展示了TdTemplate各种插入方法的使用示例</p>
 * <p><b>重要说明：</b></p>
 * <ul>
 *     <li>这是纯示例代码，不是可运行的测试类</li>
 *     <li>请在您的Spring Boot项目中参考这些示例使用TdTemplate</li>
 *     <li>实际使用时，通过@Autowired注入TdTemplate即可</li>
 * </ul>
 *
 * <p><b>在您的项目中使用示例：</b></p>
 * <pre>{@code
 * @Service
 * public class IoTDataService {
 *     @Autowired
 *     private TdTemplate tdTemplate;
 *
 *     public void saveData() {
 *         // 参考下面的示例方法，直接使用 tdTemplate
 *         // 例如: tdTemplate.insert(data);
 *     }
 * }
 * }</pre>
 *
 * @author Zephyr
 */
public class TdTemplateInsertExamples {

    // ==================== 创建超级表示例 ====================

    /**
     * 示例0: 创建超级表
     * <p>使用TdTemplate的createStableTableIfNotExist方法根据实体类自动创建超级表</p>
     */
    public void example0_createSuperTable(TdTemplate tdTemplate) {
        System.out.println("开始创建超级表...");

        // 使用TdTemplate的createStableTableIfNotExist方法创建超级表
        // 该方法会根据实体类的@TdTable和@TdTag注解自动生成CREATE STABLE语句
        int result = tdTemplate.createStableTableIfNotExist(SensorData.class);

        System.out.println("超级表 sensor_data 创建成功，影响行数: " + result);
        System.out.println("表结构：");
        System.out.println("  普通字段: ts(TIMESTAMP), temperature(DOUBLE), humidity(DOUBLE)");
        System.out.println("  TAG字段: device_id(NCHAR), location(NCHAR)");
    }

    // ==================== 基础插入示例 ====================

    /**
     * 示例1: 插入单条数据到普通表
     */
    public void example1_insertToNormalTable(TdTemplate tdTemplate) {
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

    /**
     * 示例2: 插入单条数据到超级表（包含TAG字段）
     */
    public void example2_insertToSuperTable(TdTemplate tdTemplate) {
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

    /**
     * 示例3: 使用策略模式动态生成子表名插入
     */
    public void example3_insertWithDynamicTableName(TdTemplate tdTemplate) {
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

    /**
     * 示例4: 使用Map作为数据载体插入
     */
    public void example4_insertWithMap(TdTemplate tdTemplate) {
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

    /**
     * 示例5: 使用USING语法插入（自动创建子表）
     */
    public void example5_insertUsing(TdTemplate tdTemplate) {
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

    /**
     * 示例6: 批量插入到不同子表（使用默认批次大小）
     */
    public void example6_batchInsertToDifferentTables(TdTemplate tdTemplate) {
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

    /**
     * 示例7: 批量插入到不同子表（自定义批次大小）
     */
    public void example7_batchInsertWithCustomPageSize(TdTemplate tdTemplate) {
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

    /**
     * 示例8: 使用USING语法批量插入（默认批次大小）
     */
    public void example8_batchInsertUsing(TdTemplate tdTemplate) {
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

    /**
     * 示例9: 使用USING语法批量插入（自定义表名策略）
     */
    public void example9_batchInsertUsingWithStrategy(TdTemplate tdTemplate) {
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

    /**
     * 示例10: 使用USING语法批量插入（自定义批次大小）
     */
    public void example10_batchInsertUsingWithCustomPageSize(TdTemplate tdTemplate) {
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

    /**
     * 示例11: 根据时间动态分表插入
     */
    public void example11_insertWithTimeBasedPartition(TdTemplate tdTemplate) {
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

    /**
     * 示例12: 使用Lambda表达式的表名策略
     */
    public void example12_insertWithLambdaStrategy(TdTemplate tdTemplate) {
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
