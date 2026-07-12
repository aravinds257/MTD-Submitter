# MTD Submitter

A web application for submitting **Making Tax Digital (MTD) for Income Tax Self Assessment (ITSA)** returns to HMRC. Built with Spring Boot, it provides a streamlined interface for landlords and self-employed individuals to manage quarterly updates and final declarations through the HMRC MTD APIs.

## Tech Stack

| Layer          | Technology                                   |
|----------------|----------------------------------------------|
| Runtime        | Java 21                                      |
| Framework      | Spring Boot 3.4.1                            |
| Templating     | Thymeleaf + Layout Dialect                   |
| Security       | Spring Security + OAuth2 (HMRC)              |
| Database       | PostgreSQL 16                                |
| Migrations     | Flyway                                       |
| ORM            | Spring Data JPA / Hibernate                  |
| HTTP Client    | Spring WebFlux (WebClient)                   |
| Payments       | Stripe                                       |
| CSV Parsing    | OpenCSV                                      |
| Mapping        | MapStruct                                    |
| Encryption     | Jasypt                                       |
| Build          | Maven                                        |
| Containers     | Docker                                       |

## Prerequisites

- **Java 21** (Eclipse Temurin or equivalent)
- **Maven 3.9+**
- **Docker & Docker Compose** (for local PostgreSQL)

## Getting Started

### 1. Start the database

```bash
docker-compose up -d
```

### 2. Run the application

```bash
mvn spring-boot:run
```

The application will be available at [http://localhost:8080](http://localhost:8080).

### 3. Run tests

```bash
mvn test
```

## Environment Variables

| Variable               | Description                         | Default / Dev Value                      |
|------------------------|-------------------------------------|------------------------------------------|
| `HMRC_CLIENT_ID`       | HMRC OAuth2 client ID               | `your-sandbox-client-id`                 |
| `HMRC_CLIENT_SECRET`   | HMRC OAuth2 client secret           | `your-sandbox-client-secret`             |
| `JASYPT_PASSWORD`      | Jasypt encryption master password   | `dev-encryption-key-change-in-prod`      |
| `STRIPE_API_KEY`       | Stripe secret API key               | `sk_test_placeholder`                    |
| `STRIPE_PRICE_ID`      | Stripe price ID for subscription    | `price_placeholder`                      |
| `STRIPE_WEBHOOK_SECRET`| Stripe webhook signing secret       | `whsec_placeholder`                      |
| `DATABASE_URL`         | PostgreSQL JDBC URL (prod)          | `jdbc:postgresql://localhost:5432/mtdsubmitter` |
| `DATABASE_USERNAME`    | PostgreSQL username (prod)          | `mtdsubmitter`                           |
| `DATABASE_PASSWORD`    | PostgreSQL password (prod)          | `mtdsubmitter_dev`                       |

## Project Structure

```
mtd-submitter/
├── src/
│   ├── main/
│   │   ├── java/com/mtdsubmitter/
│   │   │   ├── config/          # Security, WebClient, Stripe configuration
│   │   │   ├── controller/      # MVC controllers
│   │   │   ├── dto/             # Data transfer objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── exception/       # Custom exceptions & handlers
│   │   │   ├── mapper/          # MapStruct mappers
│   │   │   ├── repository/      # Spring Data repositories
│   │   │   ├── service/         # Business logic
│   │   │   └── MtdSubmitterApplication.java
│   │   └── resources/
│   │       ├── db/migration/    # Flyway SQL migrations
│   │       ├── static/          # CSS, JS, images
│   │       ├── templates/       # Thymeleaf templates
│   │       ├── application.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/mtdsubmitter/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

## Docker

### Build and run the Docker image

```bash
docker build -t mtd-submitter .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/mtdsubmitter \
  -e DATABASE_USERNAME=mtdsubmitter \
  -e DATABASE_PASSWORD=mtdsubmitter_dev \
  mtd-submitter
```

## License

Proprietary. All rights reserved.
