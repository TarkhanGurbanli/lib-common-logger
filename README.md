# 📜 Common Logger Library

![Version](https://img.shields.io/badge/version-v1.0.7-blue) ![License](https://img.shields.io/badge/license-MIT-green)

A powerful and configurable method and SQL logging library for Spring Boot applications. This library enables method-level logging using Spring AOP and SQL-level logging using a proxied `DataSource`.

---

## Features

### ✅ Method Logging (AOP)

- Logs method entry, exit, arguments, and return values
- Supports `INFO`, `DEBUG`, and `ERROR` log levels
- Reflective argument summarization for detailed logging
- Configurable scoped logging with `@EnableLogging(basePackage = "...")`
- Fallback to all Spring-managed `@Component` classes if `basePackage` is not provided

### ✅ SQL Logging (DataSource Proxy)

- Logs executed SQL queries and execution time
- Inline parameter rendering (dev/local only)
- Logs batch size and affected rows
- Safety-first: avoids parameter exposure in production

---

## Installation

Add the dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>com.tarkhangurbanli</groupId>
    <artifactId>lib-common-logger</artifactId>
    <version>1.0.7</version>
</dependency>
```

Or for Gradle:

```groovy
implementation 'com.tarkhangurbanli:lib-common-logger:1.0.7'
```

---

## Quick Start

### 1. Enable Method Logging

Add `@EnableLogging` to your main configuration class:

```java
@EnableLogging
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

#### ✅ Optionally, restrict logging to a specific package:

```java
@EnableLogging(basePackage = "com.example.myapp.service")
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

- If `basePackage` is **not provided**, the logger will automatically log all Spring-managed beans (`@Component`, `@Service`, `@RestController`, etc.)
- If `basePackage` **is provided**, only classes under that package will be logged.

---

### 2. Enable SQL Logging (Optional)

```java
@EnableSqlLogging
@Configuration
public class SqlConfig {
    // your datasource configuration
}
```

### 3. Combined Usage (Method + SQL Logging)

```java
@EnableLogging(basePackage = "com.example")
@EnableSqlLogging
@Configuration
public class AppConfig {
    // your configuration
}
```

---

## Configuration (application.yml)

```yaml
spring:
  jpa:
    sql-logging:
      enabled: true
      show-parameters: true  # Only for 'dev' or 'local' profiles
```

---

## Logging Levels

| Level | Description                                |
| ----- | ------------------------------------------ |
| INFO  | Summarized method arguments                |
| DEBUG | Full arguments, return values, stack trace |
| ERROR | Exceptions with root cause                 |

---

## Output Example

### **Method Logging**

**INFO log:**
```
Executing: UserService.saveUser() with args summary: name=John, age=30
```

**DEBUG log:**
```
Enter: UserService.saveUser() with full arguments: [User(name=John, age=30)]
Exit: UserService.saveUser() with result: true
```

### **SQL log:**
```
Query: INSERT INTO users (name, age) VALUES ('John', 30); | rowsAffected=1 time=12ms
```

---

## 📜 License

MIT License - see [LICENSE](LICENSE).

---

## Author

**Tarkhan Gurbanli**

---

🌐 [github.com/TarkhanGurbanli/lib-common-logger](https://github.com/TarkhanGurbanli/lib-common-logger)

---

Enjoy clean, customizable, and production-safe logging in your Spring Boot apps!
