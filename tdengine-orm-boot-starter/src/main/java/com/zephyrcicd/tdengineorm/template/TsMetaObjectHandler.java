package com.zephyrcicd.tdengineorm.template;

import com.zephyrcicd.tdengineorm.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 默认填充ts字段的元对象处理器
 * <p>在插入数据前自动填充ts字段为当前时间，解决并发生成时间戳问题</p>
 *
 * @author Zephyr
 */
public class TsMetaObjectHandler implements MetaObjectHandler {

    private static final Logger log = LoggerFactory.getLogger(TsMetaObjectHandler.class);

    /**
     * 插入元对象字段填充（用于插入时对公共字段的填充）
     *
     * @param object 实体对象或Map
     * @param <T>    实体类型或Map类型
     */
    @Override
    public <T> void insertFill(T object) {
        if (object == null) {
            return;
        }

        // 处理Map类型
        if (object instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) object;
            // 只有在ts字段不存在或为空时才进行填充
            if (!map.containsKey("ts") || map.get("ts") == null) {
                map.put("ts", getCurrentTimeForType(Object.class));
            }
            return;
        }

        // 处理实体对象
        ClassUtil.getAllFields(object.getClass())
                .stream()
                .filter(field -> "ts".equals(field.getName()))
                .forEach(field -> {
                    try {
                        field.setAccessible(true);
                        // 只有在ts字段为空时才进行填充
                        if (field.get(object) == null) {
                            Object timeValue = getCurrentTimeForType(field.getType());
                            field.set(object, timeValue);
                        }
                    } catch (IllegalAccessException e) {
                        log.warn("Failed to access field: {}", field.getName(), e);
                        // 忽略访问异常
                    }
                });
    }

    /**
     * 根据字段类型获取当前时间值
     *
     * @param fieldType 字段类型
     * @return 对应类型的时间值
     */
    private Object getCurrentTimeForType(Class<?> fieldType) {
        // 使用ThreadLocalRandom避免并发问题
        long timestamp = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(0, 1000);

        if (fieldType == long.class || fieldType == Long.class) {
            return timestamp;
        } else if (fieldType == int.class || fieldType == Integer.class) {
            return (int) timestamp;
        } else if (fieldType == Date.class) {
            return new Date(timestamp);
        } else if (fieldType == LocalDateTime.class) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        } else if (fieldType == LocalDate.class) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).toLocalDate();
        } else if (fieldType == Instant.class) {
            return Instant.ofEpochMilli(timestamp);
        } else {
            // 默认返回Long类型时间戳
            return timestamp;
        }
    }
}
