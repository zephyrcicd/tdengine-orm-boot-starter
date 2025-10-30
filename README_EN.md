## Introduction

[English](README_EN.md) | [中文](README.md)

> `tdengine-orm-boot-starter` is a semi-ORM framework based on SpringBootJdbc for convenient operation of TDengine data, inspired by MyBatisPlus design

### Tech Stack

- spring-boot-starter 2.X Mainly uses SpringBoot's auto-configuration feature. After SpringBoot 2.7, the auto-configuration method has changed, but the old way remains compatible
- spring-boot-starter-jdbc 2.x Mainly uses the JdbcTemplate object

## Quick Start

1. Add JitPack repository and dependency to your project's `pom.xml`:
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
        <version>main-SNAPSHOT</version> <!-- or use a specific tag like v1.0.0 -->
    </dependency>
    ```
2. Configure database connection in `application.yml`
3. Create entity classes with `@TdTable` and `@TdTag` annotations
4. Inject `TdTemplate` in your service class and start using it

## Detailed Usage Guide

### Supported Connection Pools

The starter supports the following connection pools, listed in order of priority:

1. **Druid** - Highest priority
2. **HikariCP** - Second priority
3. **Apache DBCP2** - Third priority
4. **Spring DriverManagerDataSource** - Fallback option

### Usage Steps

#### 1. Add Dependencies

Add JitPack repository and TDengine ORM Starter dependency to your project's `pom.xml`:

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
    <version>main-SNAPSHOT</version> <!-- Latest from main branch -->
    <!-- or use a specific tag: <version>v1.0.0</version> -->
</dependency>
```

**Version Selection**:
- `main-SNAPSHOT`: Latest code from main branch (always up-to-date)
- `v1.0.0` or other tags: Stable release versions (recommended for production)
- Commit hash: Specific commit version

#### 2. Add Connection Pool Dependency (Optional)

Choose a connection pool based on your needs:

##### Using Druid Connection Pool
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.8</version>
</dependency>
```

##### Using HikariCP Connection Pool
```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>
```

##### Using Apache DBCP2 Connection Pool
```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-dbcp2</artifactId>
    <version>2.9.0</version>
</dependency>
```

If no connection pool dependency is added, the starter will use Spring's DriverManagerDataSource as a fallback.

#### 3. Configure Database Connection

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

#### 4. Using TdTemplate

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

#### 5. More Usage Examples

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

#### 6. Entity Class Example

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
