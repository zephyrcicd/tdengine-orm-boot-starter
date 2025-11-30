## Introduction

[English](README_EN.md) | [中文](README.md)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.zephyrcicd/tdengine-orm-boot-starter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.zephyrcicd/tdengine-orm-boot-starter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub stars](https://img.shields.io/github/stars/zephyrcicd/tdengine-orm-boot-starter.svg?style=social&label=Star)](https://github.com/zephyrcicd/tdengine-orm-boot-starter)

> `tdengine-orm-boot-starter` is a semi-ORM framework based on SpringBootJdbc for convenient operation of TDengine data, inspired by MyBatisPlus design

### Tech Stack

- spring-boot-starter 2.X Mainly uses SpringBoot's auto-configuration feature. After SpringBoot 2.7, the auto-configuration method has changed, but the old way remains compatible
- spring-boot-starter-jdbc 2.x Mainly uses the JdbcTemplate object

## Quick Start

### 1. Add Dependencies

**Maven** - Add to `pom.xml`:
```xml
<!-- TDengine ORM Boot Starter -->
<dependency>
    <groupId>io.github.zephyrcicd</groupId>
    <artifactId>tdengine-orm-boot-starter</artifactId>
    <version>${tdengine-orm.version}</version>  <!-- Check for the latest version -->
</dependency>

<!-- TDengine JDBC Driver (Required) -->
<dependency>
    <groupId>com.taosdata.jdbc</groupId>
    <artifactId>taos-jdbcdriver</artifactId>
    <version>${taos-jdbcdriver.version}</version>  <!-- Choose version compatible with your TDengine server -->
</dependency>
```

**Gradle Kotlin DSL** - Add to `build.gradle.kts`:
```kotlin
dependencies {
    // TDengine ORM Boot Starter
    implementation("io.github.zephyrcicd:tdengine-orm-boot-starter:${tdengineOrmVersion}")  // Check for the latest version

    // TDengine JDBC Driver (Required)
    implementation("com.taosdata.jdbc:taos-jdbcdriver:${taosJdbcdriverVersion}")  // Choose version compatible with your TDengine server
}
```

**Gradle Groovy DSL** - Add to `build.gradle`:
```groovy
dependencies {
    // TDengine ORM Boot Starter
    implementation "io.github.zephyrcicd:tdengine-orm-boot-starter:${tdengineOrmVersion}"  // Check for the latest version

    // TDengine JDBC Driver (Required)
    implementation "com.taosdata.jdbc:taos-jdbcdriver:${taosJdbcdriverVersion}"  // Choose version compatible with your TDengine server
}
```

> 💡 **Latest Version**: Check [Maven Central](https://central.sonatype.com/artifact/io.github.zephyrcicd/tdengine-orm-boot-starter) or [GitHub Releases](https://github.com/zephyrcicd/tdengine-orm-boot-starter/releases) for the latest version
> 💡 **TDengine JDBC Driver**: Please refer to [Maven Central - taos-jdbcdriver](https://central.sonatype.com/artifact/com.taosdata.jdbc/taos-jdbcdriver) and choose a version compatible with your TDengine server (e.g., 3.2.5, 3.6.3, etc.)

### 2. Configure DataSource

This framework does not create DataSource automatically. Users need to configure it themselves. We recommend using Spring Boot's standard configuration:

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
  page-size: 500  # Batch operation page size, default 500
```

> Note: Starting from version 2.x, the framework focuses on ORM functionality. DataSource management is delegated to users or dedicated DataSource starters.

### 3. Create Entity Class

Define entity with `@TdTable` and `@TdTag` annotations:
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

### 4. Start Using

Inject `TdTemplate` in your service class:
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

## Detailed Usage Guide

### DataSource Configuration

Starting from version 2.x, this framework no longer creates DataSource automatically but focuses on ORM functionality. Users need to configure TDengine DataSource themselves.

#### Option 1: Using Spring Boot Auto-Configuration (Recommended)

The simplest way is to use Spring Boot's standard DataSource configuration:

##### application.yml Example

```yaml
spring:
  datasource:
    url: jdbc:TAOS://localhost:6030/test
    username: root
    password: taosdata
    driver-class-name: com.taosdata.jdbc.TSDBDriver

td-orm:
  enabled: true  # Optional, defaults to true
  log-level: ERROR  # Log level: ERROR, WARN, INFO, DEBUG
  page-size: 500  # Batch operation page size, default 500
  enable-ts-auto-fill: true  # Enable ts field auto-fill, default true
```

##### application.properties Example

```properties
spring.datasource.url=jdbc:TAOS://localhost:6030/test
spring.datasource.username=root
spring.datasource.password=taosdata
spring.datasource.driver-class-name=com.taosdata.jdbc.TSDBDriver

td-orm.enabled=true
td-orm.log-level=ERROR
td-orm.page-size=500
```

#### Option 2: Custom DataSource Bean

For more fine-grained connection pool control, you can define a custom DataSource Bean:

##### Using HikariCP

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

        // Custom pool settings
        config.setMaximumPoolSize(30);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}
```

##### Using Druid

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

        // Custom pool settings
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

### Using TdTemplate

Inject and use `TdTemplate` in your service class:

```java
@Service
public class IoTDataService {

    @Autowired
    private TdTemplate tdTemplate;

    public void saveData(SensorData data) {
        // Insert single record
        tdTemplate.insert(data);
    }

    public List<SensorData> findData() {
        // Query data
        TdQueryWrapper<SensorData> wrapper = TdWrappers.queryWrapper(SensorData.class)
                .selectAll()
                .orderByDesc("ts")
                .limit(100);
        return tdTemplate.list(wrapper);
    }
}
```

#### 4. Complete Example Project

If you want to see complete, runnable usage examples, please check out our Demo project:

📦 **[tdengine-orm-demo](https://github.com/zephyrcicd/tdengine-orm-demo)**

Demo Project Features:
- ✅ 15 complete test cases covering all core features
- ✅ Includes performance statistics and throughput tests
- ✅ Demonstrates PARTITION BY queries, time window functions, and other advanced features
- ✅ Ready to use out of the box - just configure database connection and run
- ✅ Clean and clear code, perfect for learning and reference

By running the Demo project's test cases, you can quickly understand the various usage patterns of TdTemplate.

#### 5. Annotation Guide

The framework provides three core annotations to define TDengine entity classes:

##### @TdTable
Maps entity class to TDengine table or stable:
```java
@TdTable("sensor_data")  // Specify table name
public class SensorData {
    // ...
}
```

##### @TdTag
Marks TAG fields (TDengine metadata columns) for sub-table grouping and filtering:
```java
@TdTag
private String deviceId;  // TAG field
```

##### @TdColumn
Field column mapping annotation with various configurations:
```java
@TdColumn(value = "temp", type = TdFieldTypeEnum.DOUBLE, length = 8)
private Double temperature;

@TdColumn(exist = false)
private String internalField;  // Internal field excluded from SQL generation
```

**@TdColumn Main Attributes:**
- `value`: Custom column name (defaults to field name in snake_case)
- `type`: Specify TDengine field type (auto-inferred by default)
- `length`: Field length for NCHAR, BINARY, VARCHAR types
- `exist`: Controls whether field participates in SQL generation (default true)
- `comment`: Field comment
- `nullable`: Whether field allows null values
- `compositeKey`: Whether field is composite primary key (TDengine 3.3+ only)

#### 6. Entity Class Example

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

    // getter/setter methods...
}
```

### Auto-Configuration Details

#### Bean Creation

The starter automatically creates the following beans (based on user-provided DataSource):

- `tdengineJdbcTemplate` - TDengine-specific JdbcTemplate
- `tdengineNamedParameterJdbcTemplate` - TDengine-specific NamedParameterJdbcTemplate
- `tdTemplate` - TDengine template class for data access operations

> Note: Starting from version 2.x, the framework no longer creates DataSource automatically. Users need to configure the DataSource themselves.

### Disabling Auto-Configuration

To disable TDengine ORM auto-configuration, set in your configuration file:

```yaml
td-orm:
  enabled: false
```

Or exclude the auto-configuration in your startup class:

```java
@SpringBootApplication(exclude = {TdOrmAutoConfiguration.class})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Notes

1. Ensure TDengine service is running and DataSource is configured correctly
2. The framework depends on user-provided DataSource, make sure DataSource is properly configured
3. This starter is compatible with Spring Boot's auto-configuration and won't cause conflicts

### Build Instructions

This project uses Gradle for build and publication. Common commands:

```bash
# Clean and build (with tests)
./gradlew clean build

# Assemble artifacts only (skip running tests)
./gradlew clean assemble

# Publish to local Maven repository
./gradlew publishToMavenLocal

# Inspect dependency tree
./gradlew dependencies
```

## Contribution & Support

### Welcome Contributions

We warmly welcome developers to contribute to TDengine ORM Boot Starter! Whether it's:

- 🐛 **Reporting Issues** - Found a bug? Please submit it in [Issues](https://github.com/zephyrcicd/tdengine-orm-boot-starter/issues)
- 💡 **Feature Suggestions** - Have great ideas? Feel free to discuss them in Issues
- 🔧 **Submit Code** - Welcome to submit Pull Requests to improve the project
- 📖 **Improve Documentation** - Help us enhance documentation and examples

### Give us a Star ⭐

If this project helps you, please give us a Star! Your support motivates us to keep improving.
[![GitHub stars](https://img.shields.io/github/stars/zephyrcicd/tdengine-orm-boot-starter.svg?style=social&label=Star)](https://github.com/zephyrcicd/tdengine-orm-boot-starter)

[![Star History Chart](https://api.star-history.com/svg?repos=zephyrcicd/tdengine-orm-boot-starter&type=Date)](https://star-history.com/#zephyrcicd/tdengine-orm-boot-starter&Date)
