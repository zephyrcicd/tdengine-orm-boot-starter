## 介绍

[English](README_EN.md) | [中文](README.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.zephyrcicd/tdengine-orm-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.zephyrcicd/tdengine-orm-boot-starter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub stars](https://img.shields.io/github/stars/zephyrcicd/tdengine-orm-boot-starter.svg?style=social&label=Star)](https://github.com/zephyrcicd/tdengine-orm-boot-starter)

> `tdengine-orm-boot-starter` 是一个基于 SpringBootJdbc 的半 ORM 框架，用于便捷操作 TDengine 数据，其设计参考了
> MyBatisPlus

### 技术栈

- spring-boot-starter 2.X：主要使用 SpringBoot 的自动装配功能，虽然 SpringBoot 2.7 之后自动装配方式有所修改，但旧的方式仍然兼容
- spring-boot-starter-jdbc 2.x：主要使用 JdbcTemplate 对象

## 快速开始

### 1. 添加依赖

**Maven** - 在 `pom.xml` 中添加：
```xml
<!-- TDengine ORM Boot Starter -->
<dependency>
    <groupId>io.github.zephyrcicd</groupId>
    <artifactId>tdengine-orm-boot-starter</artifactId>
    <version>${tdengine-orm.version}</version>  <!-- 请查看最新版本 -->
</dependency>

<!-- TDengine JDBC 驱动（必需） -->
<dependency>
    <groupId>com.taosdata.jdbc</groupId>
    <artifactId>taos-jdbcdriver</artifactId>
    <version>${taos-jdbcdriver.version}</version>  <!-- 请根据您的 TDengine 版本选择合适的驱动版本 -->
</dependency>
```

**Gradle Kotlin DSL** - 在 `build.gradle.kts` 中添加：
```kotlin
dependencies {
    // TDengine ORM Boot Starter
    implementation("io.github.zephyrcicd:tdengine-orm-boot-starter:${tdengineOrmVersion}")  // 请查看最新版本

    // TDengine JDBC 驱动（必需）
    implementation("com.taosdata.jdbc:taos-jdbcdriver:${taosJdbcdriverVersion}")  // 请根据您的 TDengine 版本选择
}
```

**Gradle Groovy DSL** - 在 `build.gradle` 中添加：
```groovy
dependencies {
    // TDengine ORM Boot Starter
    implementation "io.github.zephyrcicd:tdengine-orm-boot-starter:${tdengineOrmVersion}"  // 请查看最新版本

    // TDengine JDBC 驱动（必需）
    implementation "com.taosdata.jdbc:taos-jdbcdriver:${taosJdbcdriverVersion}"  // 请根据您的 TDengine 版本选择
}
```

> 💡 **最新版本**：请访问 [Maven Central](https://central.sonatype.com/artifact/io.github.zephyrcicd/tdengine-orm-boot-starter) 或 [GitHub Releases](https://github.com/zephyrcicd/tdengine-orm-boot-starter/releases) 查看最新版本
> 💡 **TDengine JDBC 驱动**：请参考 [Maven Central - taos-jdbcdriver](https://central.sonatype.com/artifact/com.taosdata.jdbc/taos-jdbcdriver) 选择与您的 TDengine 服务器版本兼容的驱动版本（如 3.2.5、3.6.3 等）

### 2. 配置数据库连接

在 `application.yml` 中配置 TDengine 连接信息：
```yaml
td-orm:
  enabled: true
  url: jdbc:TAOS://localhost:6030/test
  username: root
  password: taosdata
  driver-class-name: com.taosdata.jdbc.TSDBDriver
  log-level: ERROR
```

### 3. 创建实体类

使用 `@TdTable` 和 `@TdTag` 注解定义实体：
```java
@TdTable("sensor_data")
public class SensorData {
    @TdTag
    private String deviceId;

    private Double temperature;
    private Long ts;
    // getter/setter...
}
```

### 4. 开始使用

在服务类中注入 `TdTemplate` 即可使用：
```java
@Service
public class IoTDataService {
    @Autowired
    private TdTemplate tdTemplate;

    public void saveData(SensorData data) {
        tdTemplate.insert(data);
    }
}
```

## 详细使用指南

### 支持的连接池

该 starter 支持以下连接池，按优先级排序：

1. **Druid** - 优先级最高
2. **HikariCP** - 优先级次之
3. **Apache DBCP2** - 优先级再次之
4. **Spring DriverManagerDataSource** - 兜底方案

### 使用方法

#### 1. 添加连接池依赖（可选）

根据需要选择一个连接池：

##### Maven

**使用 Druid 连接池**

```xml

<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.8</version>
</dependency>
```

**使用 HikariCP 连接池**

```xml

<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>
```

**使用 Apache DBCP2 连接池**

```xml

<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
    <version>2.9.0</version>
</dependency>
```

##### Gradle

**Kotlin DSL**：

```kotlin
dependencies {
    // Druid
    implementation("com.alibaba:druid:1.2.8")

    // 或 HikariCP
    implementation("com.zaxxer:HikariCP:5.0.1")

    // 或 Apache DBCP2
    implementation("org.apache.commons:commons-dbcp2:2.9.0")
}
```

如果不添加任何连接池依赖，starter 将使用 Spring 的 DriverManagerDataSource 作为兜底方案。

#### 2. 配置数据库连接

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

#### 3. 使用 TdTemplate

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

#### 4. 查看更多使用示例

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

#### 5. 完整示例项目

如果您想查看完整的、可运行的使用案例，请参考我们的 Demo 项目：

📦 **[tdengine-orm-demo](https://github.com/zephyrcicd/tdengine-orm-demo)**

Demo 项目特点：

- ✅ 15个完整的测试用例，覆盖所有核心功能
- ✅ 包含性能统计和吞吐量测试
- ✅ 演示 PARTITION BY 分区查询、时间窗口等高级功能
- ✅ 开箱即用，配置数据库连接后即可运行
- ✅ 代码简洁清晰，适合学习参考

通过运行 Demo 项目的测试用例，您可以快速了解 TdTemplate 的各种使用方式。

#### 6. 注解说明

该框架提供三个核心注解来定义 TDengine 实体类：

##### @TdTable

用于映射实体类到 TDengine 表或超级表：

```java

@TdTable("sensor_data")  // 指定表名
public class SensorData {
    // ...
}
```

##### @TdTag

标记 TAG 字段（TDengine 的元数据列），用于子表分组和过滤：

```java

@TdTag
private String deviceId;  // TAG 字段
```

##### @TdColumn

字段列映射注解，支持多种配置：

```java

@TdColumn(value = "temp", type = TdFieldTypeEnum.DOUBLE, length = 8)
private Double temperature;

@TdColumn(exist = false)
private String internalField;  // 不参与 SQL 生成的内部字段
```

**@TdColumn 主要属性：**

- `value`：自定义列名（默认使用字段的下划线形式）
- `type`：指定 TDengine 字段类型（默认自动推断）
- `length`：字段长度，适用于 NCHAR、BINARY、VARCHAR 等类型
- `exist`：控制字段是否参与 SQL 生成（默认 true）
- `comment`：字段注释
- `nullable`：是否允许为空
- `compositeKey`：是否为复合主键（仅 TDengine 3.3+ 支持）

#### 7. 实体类定义示例

```java

@TdTable("sensor_data")
public class SensorData {

    @TdTag
    private String deviceId;

    @TdTag
    @TdColumn(value = "location", length = 100)
    private String location;

    @TdColumn(value = "temp", type = TdFieldTypeEnum.DOUBLE)
    private Double temperature;

    private Double humidity;
    private Long ts;

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

如果你需要自定义连接池配置，可以创建自己的 DataSource bean。只要将 bean 命名为 `tdengineDataSource`，starter
就会使用你的自定义配置。以下是具体方法：

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

## 贡献与支持

### 欢迎贡献

我们非常欢迎开发者为 TDengine ORM Boot Starter 贡献代码！无论是：

- 🐛 **报告问题** - 发现 Bug 请在 [Issues](https://github.com/zephyrcicd/tdengine-orm-boot-starter/issues) 中提交
- 💡 **功能建议** - 有好的想法欢迎在 Issues 中讨论
- 🔧 **提交代码** - 欢迎提交 Pull Request 改进项目
- 📖 **完善文档** - 帮助我们改进文档和示例

### 给个 Star ⭐

如果这个项目对您有帮助，欢迎给个 Star 支持一下！您的支持是我们持续改进的动力。
[![GitHub stars](https://img.shields.io/github/stars/zephyrcicd/tdengine-orm-boot-starter.svg?style=social&label=Star)](https://github.com/zephyrcicd/tdengine-orm-boot-starter)

[![Star History Chart](https://api.star-history.com/svg?repos=zephyrcicd/tdengine-orm-boot-starter&type=Date)](https://star-history.com/#zephyrcicd/tdengine-orm-boot-starter&Date)