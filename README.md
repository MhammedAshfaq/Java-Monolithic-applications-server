# Spring Monolith Template

A production-ready Spring Boot monolith template with comprehensive features including RESTful APIs, database migrations, security, and API documentation.

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Technology Stack](#technology-stack)
- [Project Setup](#project-setup)
- [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Testing](#testing)

## 🔧 Prerequisites

Before you begin, ensure you have the following installed:

- **Java 21** or higher
- **Maven 3.6+** (or use the included Maven Wrapper)
- **PostgreSQL 12+** (for database)
- **Git** (for version control)

## 🛠 Technology Stack

- **Framework**: Spring Boot 4.0.2
- **Java Version**: 21
- **Build Tool**: Maven
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **Database Migration**: Flyway
- **Security**: Spring Security
- **API Documentation**: Swagger/OpenAPI 3 (SpringDoc)
- **Message Queue**: RabbitMQ (AMQP)
- **Validation**: Bean Validation
- **Connection Pool**: HikariCP

## 🚀 Project Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd spring-monolith-template
```

### 2. Install Dependencies

Using Maven Wrapper (recommended):
```bash
./mvnw clean install
```

Or using Maven directly:
```bash
mvn clean install
```

## 🗄 Database Setup

### 1. Install PostgreSQL

Ensure PostgreSQL is installed and running on your system.

### 2. Create Database

Create a new PostgreSQL database:

```sql
CREATE DATABASE "java-spring-mololithic";
```

### 3. Configure Database Connection

Update the database credentials in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/java-spring-mololithic
    username: postgres
    password: postgres
```

**Note**: Change the credentials according to your PostgreSQL setup.

### 4. Database Migrations

Flyway is configured to automatically run migrations on application startup. Migration scripts should be placed in:
```
src/main/resources/db/migration/
```

Migration files should follow the naming convention: `V{version}__{description}.sql`
Example: `V1__create_users_table.sql`

## ▶️ Running the Application

### Using Maven Wrapper

```bash
./mvnw spring-boot:run
```

### Using Maven

```bash
mvn spring-boot:run
```

### Using Java directly

```bash
./mvnw clean package
java -jar target/spring-monolith-template-0.0.1-SNAPSHOT.jar
```

### Application Defaults

- **Server Port**: `8082`
- **Server Address**: `0.0.0.0` (accessible from all network interfaces)

The application will be available at: `http://localhost:8082`

## 📚 API Documentation

### Swagger UI

Once the application is running, access the interactive API documentation at:

**Swagger UI**: http://localhost:8082/apidocs

### API Docs JSON

**OpenAPI JSON**: http://localhost:8082/v1/api-docs

### Features

- Interactive API testing
- Request/Response examples
- Authentication support (JWT Bearer Token)
- Try-it-out functionality enabled
- Operations sorted by HTTP method
- Tags sorted alphabetically

## 📁 Project Structure

```
spring-monolith-template/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/javainfraexample/spring_monolith_template/
│   │   │       ├── api/                    # REST Controllers
│   │   │       │   └── health/             # Health check endpoints
│   │   │       ├── config/                 # Configuration classes
│   │   │       │   ├── security/           # Security configuration
│   │   │       │   └── swagger/            # Swagger/OpenAPI configuration
│   │   │       └── SpringMonolithTemplateApplication.java
│   │   └── resources/
│   │       ├── application.yaml            # Main configuration
│   │       ├── application-dev.yml         # Development profile
│   │       ├── application-prod.yml        # Production profile
│   │       └── db/
│   │           └── migration/              # Flyway migration scripts
│   └── test/                               # Test classes
├── pom.xml                                  # Maven dependencies
├── mvnw                                     # Maven wrapper (Unix)
├── mvnw.cmd                                 # Maven wrapper (Windows)
└── README.md                                # This file
```

## ⚙️ Configuration

### Application Configuration

Main configuration file: `src/main/resources/application.yaml`

#### Key Configuration Sections:

- **Server**: Port and address configuration
- **Database**: PostgreSQL connection settings with HikariCP pool configuration
- **JPA**: Hibernate settings and database dialect
- **Flyway**: Database migration configuration
- **Swagger**: API documentation settings

### Environment Profiles

The project supports multiple profiles:

- **Default**: Uses `application.yaml`
- **Development**: `application-dev.yml` (activate with `-Dspring.profiles.active=dev`)
- **Production**: `application-prod.yml` (activate with `-Dspring.profiles.active=prod`)

### Running with Profile

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## 🔒 Security

Spring Security is configured with:

- CSRF protection disabled (for API usage)
- Public endpoints: `/`, `/health`, `/apidocs/**`, `/v1/api-docs/**`
- All other endpoints require authentication
- JWT Bearer token authentication support configured

**Note**: Update `SecurityConfig.java` to implement your authentication mechanism.

## 🧪 Testing

Run tests using Maven:

```bash
./mvnw test
```

### Test Structure

Tests are located in `src/test/java/` and follow the same package structure as the main code.

## 📝 Available Endpoints

### Health Check

- **GET** `/health` - Returns application health status

### API Documentation

- **GET** `/apidocs` - Swagger UI interface
- **GET** `/v1/api-docs` - OpenAPI JSON specification

## 🔧 Development Tips

1. **Hot Reload**: Use Spring Boot DevTools (if added) for automatic application restart on code changes
2. **Database Changes**: Always create Flyway migration scripts instead of modifying existing ones
3. **API Documentation**: Use Swagger annotations (`@Tag`, `@Operation`, `@ApiResponse`) for better API docs
4. **Logging**: Check console output for SQL queries (enabled in development)

## 📦 Building for Production

Create a production JAR:

```bash
./mvnw clean package -DskipTests
```

The JAR file will be created in `target/spring-monolith-template-0.0.1-SNAPSHOT.jar`

Run the production JAR:

```bash
java -jar target/spring-monolith-template-0.0.1-SNAPSHOT.jar
```

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Ensure tests pass
4. Submit a pull request

## 📄 License

This project is licensed under the Apache License 2.0.

## 📞 Support

For issues and questions, please contact:
- **Email**: support@example.com
- **Documentation**: https://example.com/support

---

**Happy Coding! 🚀**
