# 📜 lib-common-logger

![Version](https://img.shields.io/badge/version-v1.0.3-blue) ![License](https://img.shields.io/badge/license-MIT-green)

A lightweight Spring AOP library for logging method calls and SQL queries in Java apps. 🌟 Simple, flexible, and performant! 🚀

---

## ✨ Features

- **Method Logging** 📋
  - INFO: `BookService.create() called, args: title=Book1, name=John`
  - DEBUG: Detailed entry/exit with full args/results
  - ERROR: Exception details with root cause

- **SQL Logging** 🗄️
  - Logs JPA/JdbcTemplate queries, e.g., `INFO: INSERT INTO books, params: [Book1, John]`

- **Configurable** ⚙️
  - Enable with `@EnableLogging`
  - Set log levels via `application.properties`

---

## 🛠️ Setup

Add to `build.gradle`:

### 1. JitPack Repository
```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

### 2. Dependency
```gradle
dependencies {
    implementation 'com.github.TarkhanGurbanli:lib-common-logger:v1.0.3'
}
```

---

## 🚀 Usage

1. **Enable Logging**:
   ```java
   @SpringBootApplication
   @EnableLogging
   public class App {
       public static void main(String[] args) {
           SpringApplication.run(App.class, args);
       }
   }
   ```

   **or**

   ```java
    @Configuration
    @EnableSqlLogging
    @EnableLogging
    public class LibConfiguration {

    }
   ```

3. **Set Log Level**:
   `application.properties`:
   ```properties
   logging.level.root=INFO  # or DEBUG
   ```

4. **Example**:
   ```java
   @Service
   public class BookService {
       public String create(String title, String name) {
           return "Book: " + title;
       }
   }
   ```

   **Output**:
   - INFO: `Executing: BookService.create(), args: title=Book1, name=John`
   - DEBUG: `Enter: BookService.create() with args: [Book1, John]`

---

## ⚙️ Configuration

- **Custom Packages**:
  ```java
  @EnableLogging(basePackages = {"com.example"})
  ```

- **Log Level**:
  ```properties
  logging.level.com.tarkhangurbanli.libcommonlogger.aspect=DEBUG
  ```

---

## 🤝 Contributing

Got ideas? 💡 Open an [issue](https://github.com/TarkhanGurbanli/lib-common-logger/issues) or submit a PR!

---

## 📜 License

MIT License - see [LICENSE](LICENSE).

---

🌐 [github.com/TarkhanGurbanli/lib-common-logger](https://github.com/TarkhanGurbanli/lib-common-logger)
