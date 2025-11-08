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

> 💡 **Latest Version**: Check [Maven Central](https://central.sonatype.com/artifact/io.github.zephyrcicd/tdengine-orm-boot-starter) or [GitHub Releases](https://github.com/zephyrcicd/tdengine-orm-boot-starter/releases) for the latest version (current: 1.4.0)
> 💡 **TDengine JDBC Driver**: Please refer to [Maven Central - taos-jdbcdriver](https://central.sonatype.com/artifact/com.taosdata.jdbc/taos-jdbcdriver) and choose a version compatible with your TDengine server (e.g., 3.2.5, 3.6.3, etc.)

### 2. Configure Database Connection

Configure TDengine connection in `application.yml`:
```yaml
td-orm:
  enabled: true
  url: jdbc:TAOS://localhost:6030/test
  username: root
  password: taosdata
  driver-class-name: com.taosdata.jdbc.TSDBDriver
  log-level: ERROR
```

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

### Supported Connection Pools

The starter supports the following connection pools, listed in order of priority:

1. **Druid** - Highest priority
2. **HikariCP** - Second priority
3. **Apache DBCP2** - Third priority
4. **Spring DriverManagerDataSource** - Fallback option

### Usage Steps

#### 1. Add Connection Pool Dependency (Optional)

Choose a connection pool based on your needs:

##### Maven

**Using Druid Connection Pool**
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.8</version>
</dependency>
```

**Using HikariCP Connection Pool**
```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>
```

**Using Apache DBCP2 Connection Pool**
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
    <version>2.9.0</version>
</dependency>
```

##### Gradle

**Kotlin DSL**:

```kotlin
dependencies {
    // Druid
    implementation("com.alibaba:druid:1.2.8")

    // or HikariCP
    implementation("com.zaxxer:HikariCP:5.0.1")

    // or Apache DBCP2
    implementation("org.apache.commons:commons-dbcp2:2.9.0")
}
```

If no connection pool dependency is added, the starter will use Spring's DriverManagerDataSource as a fallback.

#### 2. Configure Database Connection

Configure TDengine connection information in `application.yml` or `application.properties`:

##### application.yml Example
```yaml
td-orm:
  enabled: true  # Optional, defaults to true
  url: jdbc:TAOS://localhost:6030/test
  username: root
  password: taosdata
  driver-class-name: com.taosdata.jdbc.TSDBDriver
  log-level: ERROR  # Log level: ERROR, WARN, INFO, DEBUG
```

##### application.properties Example
```properties
td-orm.enabled=true
td-orm.url=jdbc:TAOS://localhost:6030/test
td-orm.username=root
td-orm.password=taosdata
td-orm.driver-class-name=com.taosdata.jdbc.TSDBDriver
td-orm.log-level=ERROR
```

#### 3. Using TdTemplate

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

#### 4. More Usage Examples

The project includes comprehensive usage examples demonstrating various insertion scenarios:

**Example Code Location**: `src/test/java/com/zephyrcicd/tdengineorm/template/TdTemplateInsertExamples.java`

**Included Examples**:
- Example 0: Create super table using TdTemplate
- Example 1-2: Basic insert operations (normal table, super table)
- Example 3-4: Dynamic table name strategy insert (entity-based, Map-based)
- Example 5: USING syntax insert (auto-create sub-table)
- Example 6-7: Batch insert to different sub-tables (default/custom batch size)
- Example 8-10: Batch USING insert (default/custom strategy/custom batch size)
- Example 11-12: Complex scenarios (time-based partitioning, Lambda expressions)
- Example 13-14: Batch insert Map data (specified table, strategy-based table)

**Important Notes**:
- These are pure example code, not runnable test classes
- Please refer to these examples in your Spring Boot project
- In actual use, inject `TdTemplate` via `@Autowired`

**Usage Examples in Your Project**:

```java
@Service
public class IoTDataService {

    @Autowired
    private TdTemplate tdTemplate;

    // Refer to example methods and use tdTemplate directly
    public void saveData(SensorData data) {
        // Example 1: Basic insert
        tdTemplate.insert(data);

        // Example 3: Dynamic table name insert
        DynamicNameStrategy<SensorData> strategy = entity ->
            "sensor_" + entity.getDeviceId();
        tdTemplate.insert(strategy, data);

        // Example 5: USING syntax insert (auto-create sub-table)
        tdTemplate.insertUsing(data, strategy);
    }

    public void batchSaveData(List<SensorData> dataList) {
        // Example 6: Batch insert to different sub-tables
        DynamicNameStrategy<SensorData> strategy = entity ->
            "sensor_" + entity.getDeviceId();
        tdTemplate.batchInsert(SensorData.class, dataList, strategy);
    }

    public void batchSaveMapData(List<Map<String, Object>> dataList) {
        // Example 13: Batch insert Map data to specified table
        tdTemplate.batchInsert("sensor_device001", dataList);

        // Example 14: Batch insert Map data to different tables (using strategy)
        DynamicNameStrategy<Map<String, Object>> strategy = map -> "sensor_" + map.get("device_id");
        tdTemplate.batchInsert(dataList, strategy);
    }
}
```

#### 5. Complete Example Project

If you want to see complete, runnable usage examples, please check out our Demo project:

📦 **[tdengine-orm-demo](https://github.com/zephyrcicd/tdengine-orm-demo)**

Demo Project Features:
- ✅ 15 complete test cases covering all core features
- ✅ Includes performance statistics and throughput tests
- ✅ Demonstrates PARTITION BY queries, time window functions, and other advanced features
- ✅ Ready to use out of the box - just configure database connection and run
- ✅ Clean and clear code, perfect for learning and reference

By running the Demo project's test cases, you can quickly understand the various usage patterns of TdTemplate.

#### 6. Annotation Guide

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

#### 7. Entity Class Example

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

The starter automatically creates the following beans:

- `tdengineDataSource` - TDengine data source
- `tdengineJdbcTemplate` - TDengine-specific JdbcTemplate
- `tdengineNamedParameterJdbcTemplate` - TDengine-specific NamedParameterJdbcTemplate
- `tdTemplate` - TDengine template class for data access operations

#### Connection Pool Selection Logic

1. Check if Druid-related classes exist in the classpath, if yes, create Druid DataSource
2. If no Druid, check if HikariCP exists, if yes, create HikariCP DataSource
3. If no HikariCP, check if DBCP2 exists, if yes, create DBCP2 DataSource
4. If none of the above, use Spring's DriverManagerDataSource

#### Connection Pool Configurations

#### Customizing Connection Pool

If you need to customize the connection pool settings, you can create your own DataSource bean. The starter will use your custom DataSource if it's named `tdengineDataSource`. Here's how to do it:

##### Example: Custom HikariCP Configuration
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
        
        // Customize pool settings
        config.setMaximumPoolSize(30);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        return new HikariDataSource(config);
    }
}
```

##### Example: Custom Druid Configuration
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
        
        // Customize pool settings
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

#### Default Configurations

If you don't provide a custom DataSource bean, the starter will use the following default values:

##### Druid Default Configuration
- initialSize: 5
- maxActive: 20
- minIdle: 5
- maxWait: 60000ms
- validationQuery: "SELECT 1"
- testWhileIdle: true

##### HikariCP Default Configuration
- maximumPoolSize: 20
- minimumIdle: 5
- connectionTimeout: 30000ms
- idleTimeout: 600000ms
- maxLifetime: 1800000ms

##### DBCP2 Default Configuration
- initialSize: 5
- maxTotal: 20
- minIdle: 5
- maxIdle: 10
- maxWaitMillis: 60000ms

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

1. Ensure TDengine service is running and connection configuration is correct
2. If multiple connection pools are introduced, one will be selected based on priority
3. Advanced connection pool configurations can be overridden by custom DataSource Bean
4. This starter is compatible with Spring Boot's auto-configuration and won't cause conflicts

### Maven Build Instructions

Since the test code requires an actual TDengine database connection, it's recommended to skip tests when building the project:

```bash
# Package skipping tests
mvn clean package -DskipTests

# Install to local repository skipping tests
mvn clean install -DskipTests
```

If you need to run tests, ensure:
1. TDengine service is running
2. Database connection information in configuration files is correct
3. Test database is created with appropriate permissions

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

