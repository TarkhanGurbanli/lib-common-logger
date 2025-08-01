📜 # Common Logger Library

![Version](https://img.shields.io/badge/version-v1.0.6-blue) ![License](https://img.shields.io/badge/license-MIT-green)

A powerful and configurable method and SQL logging library for Spring Boot applications. This library enables method-level logging using Spring AOP and SQL-level logging using a proxied `DataSource`.

## Features

### ✅ Method Logging (AOP)

- Logs entry, exit, arguments, return values
- Supports `INFO`, `DEBUG`, and `ERROR` log levels
- Reflective argument summarization for detailed logging
- Supports scoped logging with configurable base package
- Fallbacks to all `@Component` classes if no package configured

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
    <version>1.0.6</version>
</dependency>
```

Or for Gradle:

```groovy
implementation 'com.tarkhangurbanli:lib-common-logger:1.0.6'
```

---

## Quick Start

### 1. Enable Logging

In your main Spring Boot configuration class:

```java
@EnableLogging
@Configuration
public class AppConfig {
    // other beans
}
```

### 2. Enable SQL Logging (Optional)

```java
@EnableSqlLogging
@Configuration
public class SqlConfig {
    // other data source configs
}
```

### 3. Enable Logging and SQL Logging (Optional)

```java
@EnableLogging
@EnableSqlLogging
@EnableLogging
public class LibConfig {
    // other data source configs
}
```

---

## Configuration (application.yml)

```yaml
logging:
  aspect:
    enabled: true
    base-package: com.example.myapp  # Optional; fallback to @Component if not set

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

**INFO log:**

```
Executing: UserService.saveUser() with args summary: name=John, age=30
```

**DEBUG log:**

```
Enter: UserService.saveUser() with full arguments: [User(name=John, age=30)]
Exit: UserService.saveUser() with result: true
```

**SQL log:**

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

Enjoy clean, customizable and production-safe logging in your Spring Boot apps!

