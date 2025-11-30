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

### 2. 配置数据源

本框架不负责创建数据源，需要用户自行配置。推荐使用 Spring Boot 标准方式配置：

```yaml
spring:
  datasource:
    url: jdbc:TAOS://localhost:6030/test
    username: root
    password: taosdata
    driver-class-name: com.taosdata.jdbc.TSDBDriver

td-orm:
  enabled: true
  log-level: ERROR
  page-size: 500  # 批量操作分页大小，默认500
```

> 💡 **注意**：从 2.x 版本开始，框架专注于 ORM 功能，数据源管理由用户或专门的数据源 starter 负责。

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

## 升级指南

### v1.5.6 破坏性变更

#### DefaultTagNameStrategy 重构为 Spring Bean

`DefaultTagNameStrategy` 现在需要通过 Spring 依赖注入使用，不再支持直接 `new` 实例化。

**变更原因：** 新增 Tag 顺序自动对齐 DDL 定义功能，生成子表名时 tag 值顺序与 TDengine DDL 定义保持一致。

**迁移方式：**

```java
// 旧用法 (不再支持)
DefaultTagNameStrategy<Entity> strategy = new DefaultTagNameStrategy<>();
tdTemplate.insert(strategy, entity);

// 新用法 - 通过 Spring DI 注入
@Autowired
private DefaultTagNameStrategy defaultTagNameStrategy;

public void save(Entity entity) {
    tdTemplate.insert(defaultTagNameStrategy, entity);
}
```

**新增功能：**
- `TagOrderCacheManager`：缓存超级表的 tag 定义顺序，避免重复查询
- `TdOrmConfig.getDatabaseName()`：从 JDBC URL 自动提取数据库名称
- Tag 顺序自动与 TDengine DDL 定义对齐，确保子表名生成一致性

## 详细使用指南

### 数据源配置

从 2.x 版本开始，本框架不再自动创建数据源，而是专注于 ORM 功能。用户需要自行配置 TDengine 数据源。

#### 方式一：使用 Spring Boot 自动配置（推荐）

最简单的方式是使用 Spring Boot 的标准数据源配置：

##### application.yml 示例

```yaml
spring:
  datasource:
    url: jdbc:TAOS://localhost:6030/test
    username: root
    password: taosdata
    driver-class-name: com.taosdata.jdbc.TSDBDriver

td-orm:
  enabled: true  # 可选，默认为 true
  log-level: ERROR  # 日志级别：ERROR, WARN, INFO, DEBUG
  page-size: 500  # 批量操作分页大小，默认 500
  enable-ts-auto-fill: true  # 是否启用 ts 字段自动填充，默认 true
```

##### application.properties 示例

```properties
spring.datasource.url=jdbc:TAOS://localhost:6030/test
spring.datasource.username=root
spring.datasource.password=taosdata
spring.datasource.driver-class-name=com.taosdata.jdbc.TSDBDriver

td-orm.enabled=true
td-orm.log-level=ERROR
td-orm.page-size=500
```

#### 方式二：自定义 DataSource Bean

如果需要更精细的连接池控制，可以自定义 DataSource Bean：

##### 使用 HikariCP

```java
@Configuration
public class TdengineDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
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

##### 使用 Druid

```java
@Configuration
public class TdengineDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
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

### 使用 TdTemplate

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

#### 4. 完整示例项目

如果您想查看完整的、可运行的使用案例，请参考我们的 Demo 项目：

📦 **[tdengine-orm-demo](https://github.com/zephyrcicd/tdengine-orm-demo)**

Demo 项目特点：

- ✅ 15个完整的测试用例，覆盖所有核心功能
- ✅ 包含性能统计和吞吐量测试
- ✅ 演示 PARTITION BY 分区查询、时间窗口等高级功能
- ✅ 开箱即用，配置数据库连接后即可运行
- ✅ 代码简洁清晰，适合学习参考

通过运行 Demo 项目的测试用例，您可以快速了解 TdTemplate 的各种使用方式。

#### 5. 注解说明

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

#### 6. 实体类定义示例

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

### 自动填充功能

框架提供自动填充功能，默认会自动填充名为 `ts` 的时间戳字段。该功能默认开启，可以通过配置进行关闭。

#### 配置选项

```yaml
td-orm:
  enabled: true
  log-level: ERROR
  enable-ts-auto-fill: true  # 是否启用ts字段自动填充，默认为true
```

#### 支持的数据类型

自动填充功能支持多种时间类型：
- `Long`/`long` - 毫秒时间戳
- `Date` - Java日期类型
- `LocalDateTime` - Java 8日期时间类型
- `LocalDate` - Java 8日期类型
- `Instant` - Java 8时间戳类型

#### 使用示例

实体类中只需定义名为 `ts` 的字段，框架会在插入数据时自动填充：

```java
@TdTable("sensor_data")
public class SensorData {
    private Long ts;  // 会自动填充为当前时间戳
    
    @TdTag
    private String deviceId;
    
    private Double temperature;
    private Double humidity;
    
    // getters and setters
}
```

#### 自定义填充逻辑

如果需要自定义填充逻辑，可以实现 `MetaObjectHandler` 接口：

```java
@Component
public class CustomMetaObjectHandler implements MetaObjectHandler {
    @Override
    public <T> void insertFill(T object) {
        // 自定义填充逻辑
    }
}
```

### 自动配置详情

#### Bean 创建

该 starter 会自动创建以下 Bean（基于用户提供的 DataSource）：

- `tdengineJdbcTemplate` - TDengine 专用的 JdbcTemplate
- `tdengineNamedParameterJdbcTemplate` - TDengine 专用的 NamedParameterJdbcTemplate
- `tdTemplate` - TDengine 数据访问模板类

> 💡 **注意**：从 2.x 版本开始，框架不再自动创建 DataSource，需要用户自行配置数据源。

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

1. 确保 TDengine 服务正在运行并且数据源配置正确
2. 框架依赖用户提供的 DataSource，请确保已正确配置数据源
3. 该 starter 与 Spring Boot 的自动配置兼容，不会冲突

### 构建说明

本项目已使用 Gradle 进行构建与发布，常用命令如下：

```bash
# 清理与构建（包含测试）
./gradlew clean build

# 仅打包产物（assemble，不运行测试）
./gradlew clean assemble

# 发布到本地 Maven 仓库
./gradlew publishToMavenLocal

# 查看依赖树
./gradlew dependencies
```

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
