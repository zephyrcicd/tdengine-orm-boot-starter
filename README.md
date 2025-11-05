## 介绍

[English](README_EN.md) | [中文](README.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.zephyrcicd/tdengine-orm-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.zephyrcicd/tdengine-orm-boot-starter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> `tdengine-orm-boot-starter` 是一个基于 SpringBootJdbc 的半 ORM 框架，用于便捷操作 TDengine 数据，其设计参考了 MyBatisPlus

### 技术栈

- spring-boot-starter 2.X：主要使用 SpringBoot 的自动装配功能，虽然 SpringBoot 2.7 之后自动装配方式有所修改，但旧的方式仍然兼容
- spring-boot-starter-jdbc 2.x：主要使用 JdbcTemplate 对象

## 快速开始

1. 在你的项目 `pom.xml` 中添加依赖:
    ```xml
    <dependency>
        <groupId>io.github.zephyrcicd</groupId>
        <artifactId>tdengine-orm-boot-starter</artifactId>
        <version>1.1.0</version>
    </dependency>
    ```
2. 在 `application.yml` 中配置数据库连接
3. 使用 `@TdTable` 和 `@TdTag` 注解创建实体类
4. 在服务类中注入 `TdTemplate` 开始使用

## 详细使用指南

### 支持的连接池

该 starter 支持以下连接池，按优先级排序：

1. **Druid** - 优先级最高
2. **HikariCP** - 优先级次之
3. **Apache DBCP2** - 优先级再次之
4. **Spring DriverManagerDataSource** - 兜底方案

### 使用方法

#### 1. 添加依赖

在你的项目 `pom.xml` 中添加 TDengine ORM Starter 依赖：

```xml
<dependency>
    <groupId>io.github.zephyrcicd</groupId>
    <artifactId>tdengine-orm-boot-starter</artifactId>
    <version>1.1.0</version>
</dependency>
```

**版本说明**:
- `1.1.0`: 当前稳定版本(推荐生产环境使用)
- 查看 [Maven Central](https://central.sonatype.com/artifact/io.github.zephyrcicd/tdengine-orm-boot-starter) 获取最新版本

**可选：从 JitPack 获取开发版本**
如果需要使用最新的开发版本，可以从 JitPack 获取：

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.zephyrcicd</groupId>
    <artifactId>tdengine-orm-boot-starter</artifactId>
    <version>main-SNAPSHOT</version> <!-- 最新开发版本 -->
</dependency>
```

#### 2. 添加连接池依赖（可选）

根据需要选择一个连接池：

##### 使用 Druid 连接池
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.8</version>
</dependency>
```

##### 使用 HikariCP 连接池
```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>
```

##### 使用 Apache DBCP2 连接池
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
    <version>2.9.0</version>
</dependency>
```

如果不添加任何连接池依赖，starter 将使用 Spring 的 DriverManagerDataSource 作为兜底方案。

#### 3. 配置数据库连接

在 `application.yml` 或 `application.properties` 中配置 TDengine 连接信息：

##### application.yml 示例
```yaml
td-orm:
  enabled: true  # 可选，默认为 true
  url: jdbc:TAOS://localhost:6030/test
  username: root
  password: taosdata
  driver-class-name: com.taosdata.jdbc.TSDBDriver
  log-level: ERROR  # 日志级别：ERROR, WARN, INFO, DEBUG
```

##### application.properties 示例
```properties
td-orm.enabled=true
td-orm.url=jdbc:TAOS://localhost:6030/test
td-orm.username=root
td-orm.password=taosdata
td-orm.driver-class-name=com.taosdata.jdbc.TSDBDriver
td-orm.log-level=ERROR
```

#### 4. 使用 TdTemplate

在你的服务类中注入和使用 `TdTemplate`：

```java
@Service
public class IoTDataService {

    @Autowired
    private TdTemplate tdTemplate;

    public void saveData(SensorData data) {
        // 插入单条数据
        tdTemplate.insert(data);
    }

    public List<SensorData> findData() {
        // 查询数据
        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .selectAll()
                .orderByDesc("ts")
                .limit(100);
        return tdTemplate.list(wrapper);
    }
}
```

#### 5. 查看更多使用示例

项目中包含了完整的使用示例代码，展示了各种插入场景的用法：

**示例代码位置**: `src/test/java/com/zephyrcicd/tdengineorm/template/TdTemplateInsertExamples.java`

**包含的示例**:
- 示例0: 使用TdTemplate创建超级表
- 示例1-2: 基础插入操作（普通表、超级表）
- 示例3-4: 动态表名策略插入（基于实体、基于Map）
- 示例5: USING语法插入（自动创建子表）
- 示例6-7: 批量插入到不同子表（默认/自定义批次大小）
- 示例8-10: 批量USING插入（默认/自定义策略/自定义批次）
- 示例11-12: 复杂场景（时间分表、Lambda表达式）
- 示例13-14: 批量插入Map数据（指定表名、策略表名）

**重要说明**:
- 这些是纯示例代码，不是可运行的测试类
- 请在您的Spring Boot项目中参考这些示例
- 实际使用时，通过`@Autowired`注入`TdTemplate`即可

**在您的项目中使用示例**:

```java
@Service
public class IoTDataService {

    @Autowired
    private TdTemplate tdTemplate;

    // 参考示例代码中的方法，直接使用 tdTemplate
    public void saveData(SensorData data) {
        // 示例1: 基础插入
        tdTemplate.insert(data);

        // 示例3: 动态表名插入
        DynamicNameStrategy<SensorData> strategy = entity ->
            "sensor_" + entity.getDeviceId();
        tdTemplate.insert(strategy, data);

        // 示例5: USING语法插入（自动创建子表）
        tdTemplate.insertUsing(data, strategy);
    }

    public void batchSaveData(List<SensorData> dataList) {
        // 示例6: 批量插入到不同子表
        DynamicNameStrategy<SensorData> strategy = entity ->
            "sensor_" + entity.getDeviceId();
        tdTemplate.batchInsert(SensorData.class, dataList, strategy);
    }

    public void batchSaveMapData(List<Map<String, Object>> dataList) {
        // 示例13: 批量插入Map数据到指定表
        tdTemplate.batchInsert("sensor_device001", dataList);

        // 示例14: 批量插入Map数据到不同表（使用策略）
        DynamicNameStrategy<Map<String, Object>> strategy = map -> "sensor_" + map.get("device_id");
        tdTemplate.batchInsert(dataList, strategy);
    }
}
```

#### 6. 实体类定义示例

```java
@TdTable("sensor_data")
public class SensorData {

    @TdTag
    private String deviceId;

    @TdTag
    private String location;

    private Double temperature;
    private Double humidity;
    private Timestamp ts;

    // getter/setter 方法...
}
```

### 自动配置详情

#### Bean 创建

该 starter 会自动创建以下 Bean：

- `tdengineDataSource` - TDengine 数据源
- `tdengineJdbcTemplate` - TDengine 专用的 JdbcTemplate
- `tdengineNamedParameterJdbcTemplate` - TDengine 专用的 NamedParameterJdbcTemplate
- `tdTemplate` - TDengine 数据访问模板类

#### 连接池选择逻辑

1. 检测 classpath 中是否存在 Druid 相关类，如果存在则创建 Druid DataSource
2. 如果没有 Druid，检测是否存在 HikariCP，如果存在则创建 HikariCP DataSource
3. 如果没有 HikariCP，检测是否存在 DBCP2，如果存在则创建 DBCP2 DataSource
4. 如果都没有，使用 Spring 的 DriverManagerDataSource

#### 连接池配置

#### 自定义连接池

如果你需要自定义连接池配置，可以创建自己的 DataSource bean。只要将 bean 命名为 `tdengineDataSource`，starter 就会使用你的自定义配置。以下是具体方法：

##### 示例：自定义 HikariCP 配置
```java
@Configuration
public class CustomDataSourceConfig {
    
    @Bean("tdengineDataSource")
    public DataSource tdengineDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:TAOS://localhost:6030/test");
        config.setUsername("root");
        config.setPassword("taosdata");
        config.setDriverClassName("com.taosdata.jdbc.TSDBDriver");
        
        // 自定义连接池配置
        config.setMaximumPoolSize(30);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        return new HikariDataSource(config);
    }
}
```

##### 示例：自定义 Druid 配置
```java
@Configuration
public class CustomDataSourceConfig {
    
    @Bean("tdengineDataSource")
    public DataSource tdengineDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:TAOS://localhost:6030/test");
        dataSource.setUsername("root");
        dataSource.setPassword("taosdata");
        dataSource.setDriverClassName("com.taosdata.jdbc.TSDBDriver");
        
        // 自定义连接池配置
        dataSource.setInitialSize(10);
        dataSource.setMaxActive(50);
        dataSource.setMinIdle(10);
        dataSource.setMaxWait(30000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        
        return dataSource;
    }
}
```

#### 默认配置

如果不提供自定义的 DataSource bean，starter 将使用以下默认值：

##### Druid 默认配置
- initialSize: 5
- maxActive: 20
- minIdle: 5
- maxWait: 60000ms
- validationQuery: "SELECT 1"
- testWhileIdle: true

##### HikariCP 默认配置
- maximumPoolSize: 20
- minimumIdle: 5
- connectionTimeout: 30000ms
- idleTimeout: 600000ms
- maxLifetime: 1800000ms

##### DBCP2 默认配置
- initialSize: 5
- maxTotal: 20
- minIdle: 5
- maxIdle: 10
- maxWaitMillis: 60000ms

### 禁用自动配置

如果需要禁用 TDengine ORM 的自动配置，可以在配置文件中设置：

```yaml
td-orm:
  enabled: false
```

或者在启动类上排除自动配置：

```java
@SpringBootApplication(exclude = {TdOrmAutoConfiguration.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 注意事项

1. 确保 TDengine 服务正在运行并且连接配置正确
2. 如果同时引入多个连接池，将按照优先级选择一个
3. 连接池的高级配置可以通过自定义 DataSource Bean 来覆盖默认配置
4. 该 starter 与 Spring Boot 的自动配置兼容，不会冲突

### Maven 构建说明

由于测试代码需要实际的 TDengine 数据库连接，在打包项目时建议跳过测试：

```bash
# 跳过测试打包
mvn clean package -DskipTests

# 或者跳过测试安装到本地仓库
mvn clean install -DskipTests
```

如果需要运行测试，请确保：
1. TDengine 服务正在运行
2. 配置文件中的数据库连接信息正确
3. 测试数据库已创建并有相应权限
