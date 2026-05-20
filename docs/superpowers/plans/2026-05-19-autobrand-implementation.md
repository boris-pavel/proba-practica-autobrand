# Autobrand Practical Test — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construirea aplicației Java + Spring Boot pentru proba practică Autobrand: scraping web cu cron orar, persistență Postgres, UI CRUD cu HTMX, parsing PDF la CSV, autentificare, curs valutar BNR și dashboard.

**Architecture:** Monolit modular Spring Boot cu packages clare (controller / service / repository / scraper / pdf / config). Server-side rendering Thymeleaf + Tailwind/DaisyUI + HTMX. PostgreSQL în Docker, Flyway pentru migrări, Testcontainers pentru teste repository. Toate deciziile arhitecturale sunt justificate în [spec](../specs/2026-05-19-autobrand-proba-practica-design.md).

**Tech Stack:** Java 21 LTS · Spring Boot 3.3 · Maven · Thymeleaf · Tailwind CSS (Play CDN) + DaisyUI · HTMX 2.x · Chart.js · PostgreSQL 16 (Docker) · Spring Data JPA + Hibernate · Flyway 10.x · Spring Security · Jsoup 1.17 · Apache PDFBox 3.x · Apache Commons CSV · JUnit 5 · Mockito · Testcontainers.

**User:** Boris Pavel — beginner Java/Spring Boot. Fiecare concept Spring/JPA nou apare cu explicație în 1 frază + de ce îl folosim, **nu doar ce face**.

---

## File Structure

### Source code
```
src/main/java/ro/autobrand/proba/
├── ProbaApplication.java                     # @SpringBootApplication entry point
├── config/
│   ├── SecurityConfig.java                   # Spring Security filter chain
│   ├── SchedulerConfig.java                  # @EnableScheduling
│   └── WebConfig.java                        # MultipartConfig (5MB limit)
├── controller/
│   ├── DashboardController.java              # GET /
│   ├── ProductController.java                # /products + filter + sort
│   ├── InvoiceController.java                # PDF upload + CSV download
│   ├── ScrapeAdminController.java            # POST /admin/scrape (manual trigger)
│   └── AuthController.java                   # GET /login
├── service/
│   ├── ScrapingService.java                  # Orchestrates scrape run
│   ├── ProductService.java                   # CRUD + upsert logic + search
│   ├── PdfInvoiceService.java                # Parse PDF → InvoiceLines
│   ├── CsvExportService.java                 # InvoiceLines → CSV bytes
│   ├── ExchangeRateService.java              # BNR fetch + recompute price_ron
│   └── AppUserDetailsService.java            # Spring Security user loader
├── repository/
│   ├── ProductRepository.java                # Spring Data JPA + Specifications
│   ├── ExchangeRateRepository.java
│   ├── AppUserRepository.java
│   └── ScrapeRunRepository.java
├── model/                                    # JPA entities
│   ├── Product.java
│   ├── ExchangeRate.java
│   ├── AppUser.java
│   └── ScrapeRun.java
├── dto/
│   ├── ProductDto.java                       # cu Bean Validation pentru form
│   ├── ScrapedProductDto.java                # output din scraper
│   ├── InvoiceLineDto.java                   # rezultat parser PDF
│   └── BnrRateDto.java                       # mapping XML BNR
├── scraper/
│   ├── Scraper.java                          # interfață
│   └── WebScrapingDevScraper.java            # Jsoup implementation
├── pdf/
│   ├── InvoiceParser.java                    # interfață
│   └── AdAutoTotalInvoiceParser.java         # parser specific factură
├── specification/
│   └── ProductSpecifications.java            # filter dinamic
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ProductNotFoundException.java
    └── InvalidPdfException.java
```

### Resources
```
src/main/resources/
├── application.yml                           # comun
├── application-dev.yml                       # Postgres Docker, cron OFF
├── application-docker.yml                    # full Docker run
├── application-test.yml                      # Testcontainers
├── db/migration/
│   ├── V1__create_product_table.sql
│   ├── V2__create_exchange_rate_table.sql
│   ├── V3__create_app_user_table.sql
│   ├── V4__seed_admin_user.sql
│   └── V5__create_scrape_run_table.sql
├── templates/
│   ├── fragments/
│   │   ├── layout.html                       # head + navbar + footer + sidebar
│   │   └── messages.html                     # flash success/error
│   ├── dashboard.html
│   ├── products/
│   │   ├── list.html
│   │   ├── _row.html                         # HTMX fragment
│   │   └── edit.html
│   ├── invoice/
│   │   └── upload.html
│   ├── auth/
│   │   └── login.html
│   └── errors/
│       ├── 404.html
│       └── 500.html
└── static/                                   # eventual custom CSS minimal
```

### Tests
```
src/test/java/ro/autobrand/proba/
├── service/
│   ├── ProductServiceTest.java               # upsert logic, manually_edited
│   ├── ExchangeRateServiceTest.java          # multiplier formula
│   └── CsvExportServiceTest.java
├── repository/
│   └── ProductRepositoryTest.java            # @DataJpaTest + Testcontainers
├── controller/
│   ├── ProductControllerTest.java            # @WebMvcTest + MockMvc
│   └── InvoiceControllerTest.java
├── scraper/
│   └── WebScrapingDevScraperTest.java        # HTML fixture
├── pdf/
│   └── AdAutoTotalInvoiceParserTest.java     # sample PDF fixture
└── integration/
    └── ApplicationSmokeTest.java             # @SpringBootTest
```

### Infrastructure
```
proba-practica-autobrand/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── README.md
├── .env.example
├── .gitignore
└── docs/                                     # specs/plans/screenshots/gifs
```

---

# Phase 1 — Foundation: Environment & Project Setup

> Scope: install tools, create empty Spring Boot project, verify "hello world" + Postgres connectivity.

### Task 1.1: Install JDK 21 LTS

**Files:** none

- [ ] **Step 1: Download Eclipse Temurin JDK 21 (Windows MSI x64)**

URL: https://adoptium.net/temurin/releases/?package=jdk&version=21
File: `OpenJDK21U-jdk_x64_windows_hotspot_21.0.x_y.msi`

- [ ] **Step 2: Install with "Set JAVA_HOME" + "Add to PATH" options bifate**

- [ ] **Step 3: Verify install (PowerShell, sesiune nouă)**

```powershell
java -version
javac -version
```

Expected: `openjdk version "21.0.x" ... Temurin-21.0.x` și `javac 21.0.x`.

**Pause: dacă nu apare 21, repornește terminalul; dacă tot nu apare, verifică `$env:PATH` și `$env:JAVA_HOME`.**

---

### Task 1.2: Install IntelliJ IDEA Community Edition

- [ ] **Step 1: Download** https://www.jetbrains.com/idea/download/?section=windows — "Community Edition" (gratuit)

- [ ] **Step 2: Install cu defaults**

- [ ] **Step 3: La primul start**
  - Skip plugin import (proiect nou)
  - Theme: la alegere
  - Plugins recomandate: **Lombok** (built-in la versiuni noi), **HTMX** (opțional)

---

### Task 1.3: Install Docker Desktop

- [ ] **Step 1: Download** https://www.docker.com/products/docker-desktop/ — Windows version
- [ ] **Step 2: Install** (acceptă WSL2 backend dacă întreabă)
- [ ] **Step 3: Restart Windows dacă cere**
- [ ] **Step 4: Pornește Docker Desktop, așteaptă să arate "Engine running"**
- [ ] **Step 5: Verify**

```powershell
docker --version
docker compose version
```

Expected: `Docker version 24.x` și `Docker Compose version v2.x`.

---

### Task 1.4: Generate Spring Boot project from Initializr

- [ ] **Step 1: Open** https://start.spring.io în browser

- [ ] **Step 2: Settings:**
  - Project: **Maven**
  - Language: **Java**
  - Spring Boot: **3.3.x** (cel mai recent stable non-SNAPSHOT)
  - Group: `ro.autobrand`
  - Artifact: `proba`
  - Name: `proba`
  - Description: `Autobrand practical test — Boris Pavel`
  - Package name: `ro.autobrand.proba`
  - Packaging: **Jar**
  - Java: **21**

- [ ] **Step 3: Add Dependencies (click "ADD DEPENDENCIES" și caută):**
  - **Spring Web**
  - **Spring Data JPA**
  - **Thymeleaf**
  - **Spring Security**
  - **Validation**
  - **PostgreSQL Driver**
  - **Flyway Migration**
  - **Lombok**
  - **Spring Boot DevTools**

- [ ] **Step 4: Click "GENERATE" → download `proba.zip`**

- [ ] **Step 5: Extract în `D:\proba-practica-autobrand\` cu pași expliciți (arhiva conține un folder `proba/` în interior — îl mutăm la root):**

PowerShell (rulează linii separate, NU paste într-un singur block dacă apare prompt):

```powershell
# 1. Extract temporar
Expand-Archive -Path "$env:USERPROFILE\Downloads\proba.zip" -DestinationPath "D:\proba-practica-autobrand\_extract" -Force

# 2. Mută conținutul folderului proba/ la root
Move-Item -Path "D:\proba-practica-autobrand\_extract\proba\*" -Destination "D:\proba-practica-autobrand\" -Force

# 3. Cleanup folderul temporar
Remove-Item -Path "D:\proba-practica-autobrand\_extract" -Recurse -Force
```

- [ ] **Step 6: Verify structure**

```powershell
ls
```

Expected: `pom.xml`, `mvnw`, `mvnw.cmd`, `src/`, `docs/`, `.git/`, `.gitignore`, etc.

---

### Task 1.5: Open în IntelliJ + first build

- [ ] **Step 1: IntelliJ → File → Open → `D:\proba-practica-autobrand`**

- [ ] **Step 2: Trust project (când întreabă)**

- [ ] **Step 3: Așteaptă "Indexing..." să se termine**

- [ ] **Step 4: Maven imports automatic. Dacă nu, dreapta-click `pom.xml` → Maven → Reload Project**

- [ ] **Step 5: Verify Java SDK setup**

Settings → Build → Build Tools → Maven → Runner → JRE: setat la JDK 21.
File → Project Structure → Project SDK: 21.

- [ ] **Step 6: Run** `ProbaApplication.java` (icon-ul ▶ verde lângă clasa main)

Expected: log Spring Boot pornit, `Started ProbaApplication in X seconds`, dar **va eșua** la conexiunea Postgres (n-am pornit DB-ul încă) — e OK, înseamnă că auto-config-ul Flyway/JPA cere DB-ul. Vom rezolva în Task 1.7.

---

### Task 1.6: Create docker-compose.yml pentru Postgres

**Files:**
- Create: `docker-compose.yml` (root)

- [ ] **Step 1: Write `docker-compose.yml`:**

```yaml
services:
  db:
    image: postgres:16-alpine
    container_name: autobrand-db
    environment:
      POSTGRES_DB: autobrand
      POSTGRES_USER: autobrand
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"
    volumes:
      - db_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "autobrand"]
      interval: 5s
      timeout: 3s
      retries: 5

volumes:
  db_data:
```

- [ ] **Step 2: Start DB**

```powershell
docker compose up -d db
```

- [ ] **Step 3: Verify**

```powershell
docker compose ps
docker logs autobrand-db --tail 20
```

Expected: container `autobrand-db` running, healthy. Log: `database system is ready to accept connections`.

---

### Task 1.7: Configure application.yml profiles

**Files:**
- Delete: `src/main/resources/application.properties` (generat de Initializr)
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-dev.yml`

- [ ] **Step 1: Delete `application.properties`** (preferăm YAML, mai concis)

- [ ] **Step 2: Create `application.yml`:**

```yaml
spring:
  application:
    name: proba
  profiles:
    active: dev
  thymeleaf:
    cache: false
  jpa:
    hibernate:
      ddl-auto: validate            # nu touch schema, lasă Flyway
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    baseline-on-migrate: true

logging:
  level:
    ro.autobrand.proba: DEBUG
    org.springframework: INFO
    org.hibernate.SQL: WARN

server:
  port: 8080
  servlet:
    encoding:
      charset: UTF-8
      force: true
```

- [ ] **Step 3: Create `application-dev.yml`:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/autobrand
    username: autobrand
    password: dev_password
    driver-class-name: org.postgresql.Driver

# Cron OFF în dev — declanșăm manual din UI
app:
  scraping:
    cron-enabled: false
```

> **Spring Profile** = un set de config-uri activabile. `spring.profiles.active=dev` încarcă `application.yml` + `application-dev.yml` (suprascriere).

---

### Task 1.8: First Flyway migration (smoke test)

**Files:**
- Create: `src/main/resources/db/migration/V1__create_product_table.sql`

- [ ] **Step 1: Write migration:**

```sql
-- Smoke test migration: va fi eliminat în Phase 2 când scriem tabela product reală
CREATE TABLE smoke_test (id BIGSERIAL PRIMARY KEY, message VARCHAR(50));
INSERT INTO smoke_test (message) VALUES ('hello flyway');
```

- [ ] **Step 2: Run app from IntelliJ** (▶ pe `ProbaApplication`)

Expected: Spring pornește, Flyway aplică V1, log:
```
Flyway Community Edition ... by Redgate
Successfully validated 1 migrations
Migrating schema "public" to version "1 - init smoke"
Successfully applied 1 migration to schema "public"
```

- [ ] **Step 3: Verify DB**

```powershell
docker exec -it autobrand-db psql -U autobrand -d autobrand -c "SELECT * FROM smoke_test;"
```

Expected: 1 rând cu `hello flyway`.

- [ ] **Step 4: Open browser** http://localhost:8080

Expected: pagină Whitelabel Error (404) — normal, n-avem încă endpoint-uri. Înseamnă că app rulează.

---

### Task 1.9: First commit

- [ ] **Step 1: Set up `.gitignore` adițional pentru Java**

Append la `.gitignore`:
```
# Maven
target/
*.jar
*.war

# IntelliJ
.idea/
*.iml
*.iws
*.ipr

# Logs
logs/
*.log

# Env
.env
application-local.yml
```

- [ ] **Step 2: Commit**

```powershell
git add .
git status
git commit -m "feat: scaffold Spring Boot project with Postgres in Docker

- Spring Boot 3.3 + Java 21 from Initializr
- Maven build with Spring Web, JPA, Thymeleaf, Security, Flyway, Lombok
- docker-compose for Postgres 16 with healthcheck
- application.yml with dev profile + Flyway smoke migration"
```

---

# Phase 2 — Domain Model: Product Entity

> Scope: definește entitatea `Product`, repository-ul, migration-ul real, primul test cu Testcontainers.

### Task 2.1: Add Testcontainers dependency

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Open `pom.xml`, găsește `<dependencies>` și adaugă (înainte de `</dependencies>`):**

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Maven reload** (IntelliJ: dreapta-click `pom.xml` → Maven → Reload)

Spring Boot Parent POM gestionează versiunile (BOM) — nu trebuie să specifici versiune.

---

### Task 2.2: Replace smoke migration cu Product schema

**Files:**
- Delete: `src/main/resources/db/migration/V1__create_product_table.sql`
- Create: `src/main/resources/db/migration/V1__create_product_table.sql`

- [ ] **Step 1: Drop smoke table din DB**

```powershell
docker exec -it autobrand-db psql -U autobrand -d autobrand -c "DROP TABLE IF EXISTS smoke_test; DROP TABLE IF EXISTS flyway_schema_history;"
```

(Repornim Flyway tracking de la zero.)

- [ ] **Step 2: Replace V1 file content cu:**

```sql
CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    price_ron NUMERIC(12, 2),
    image_url VARCHAR(1000),
    source_url VARCHAR(1000),
    manually_edited BOOLEAN NOT NULL DEFAULT FALSE,
    first_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_scraped TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_product_currency ON product(currency);
CREATE INDEX idx_product_price ON product(price);
```

---

### Task 2.3: Create Product entity

**Files:**
- Create: `src/main/java/ro/autobrand/proba/model/Product.java`

- [ ] **Step 1: Write entity:**

```java
package ro.autobrand.proba.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "price_ron", precision = 12, scale = 2)
    private BigDecimal priceRon;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    @Column(name = "manually_edited", nullable = false)
    @Builder.Default
    private boolean manuallyEdited = false;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_scraped", nullable = false)
    private LocalDateTime lastScraped;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (firstSeen == null) firstSeen = now;
        if (lastScraped == null) lastScraped = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

> **Concepte noi:**
> - `@Entity` + `@Table` — Hibernate mapează clasa la o tabelă DB.
> - `@Id` + `@GeneratedValue(IDENTITY)` — Postgres BIGSERIAL auto-increment.
> - Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder` — generează getters, setters, constructori, pattern Builder, la compilare.
> - `@PrePersist` / `@PreUpdate` — callback-uri JPA executate de Hibernate înainte de INSERT / UPDATE.

---

### Task 2.4: Create ProductRepository

**Files:**
- Create: `src/main/java/ro/autobrand/proba/repository/ProductRepository.java`

- [ ] **Step 1: Write interface:**

```java
package ro.autobrand.proba.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ro.autobrand.proba.model.Product;

import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByName(String name);

    boolean existsByName(String name);
}
```

> **Concepte:**
> - `JpaRepository<Product, Long>` — Spring Data generează automat `save`, `findById`, `findAll`, `delete`, etc.
> - `JpaSpecificationExecutor` — adaugă `findAll(Specification, Pageable)` pentru filter dinamic (necesar pentru bonus #2).
> - `findByName(String)` — Spring Data **derive** SQL automat din numele metodei: `SELECT * FROM product WHERE name = ?`. Magic, dar predictibil.

---

### Task 2.5: Write repository test cu Testcontainers

**Files:**
- Create: `src/test/java/ro/autobrand/proba/repository/ProductRepositoryTest.java`
- Create: `src/test/resources/application-test.yml`

- [ ] **Step 1: Create `application-test.yml`:**

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16-alpine:///autobrand_test
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true

app:
  scraping:
    cron-enabled: false
```

> **`jdbc:tc:postgresql:16-alpine:///...`** = Testcontainers JDBC URL — Testcontainers pornește automat un container Postgres când Spring deschide datasource-ul.

- [ ] **Step 2: Write failing test:**

```java
package ro.autobrand.proba.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ro.autobrand.proba.model.Product;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    ProductRepository repository;

    @Test
    void saves_and_finds_by_name() {
        Product saved = repository.save(Product.builder()
                .name("Apple")
                .price(new BigDecimal("9.99"))
                .currency("USD")
                .build());

        Optional<Product> found = repository.findByName("Apple");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getPrice()).isEqualByComparingTo("9.99");
    }

    @Test
    void enforces_unique_name() {
        repository.save(Product.builder()
                .name("Banana")
                .price(BigDecimal.ONE)
                .currency("USD")
                .build());

        // Al doilea save cu același name trebuie să arunce
        assertThatThrownBy(() ->
                repository.saveAndFlush(Product.builder()
                        .name("Banana")
                        .price(BigDecimal.TEN)
                        .currency("USD")
                        .build())
        ).hasCauseInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }
}
```

(Adaugă `import static org.assertj.core.api.Assertions.assertThatThrownBy;`)

- [ ] **Step 3: Run test**

```powershell
./mvnw test -Dtest=ProductRepositoryTest
```

Expected first run: descarcă Testcontainers (~30s), pornește container Postgres, aplică Flyway V1, rulează teste. Ambele teste PASS.

**Pause: dacă Testcontainers nu pornește, verifică că Docker Desktop e running.**

---

### Task 2.6: Commit

- [ ] **Step 1: Stage și commit**

```powershell
git add src/ pom.xml
git commit -m "feat: add Product entity, repository, and Flyway V1 migration

- Product entity with Lombok + JPA annotations
- UNIQUE constraint on name enforced at DB level
- ProductRepository extends JpaSpecificationExecutor for future filter
- Testcontainers-based repository tests verify save/find/unique"
```

---

# Phase 3 — Scraping: Jsoup + Login + Parse

> Scope: implementare scraper interface, login + fetch products, upsert logic.

### Task 3.1: Add Jsoup dependency

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add:**

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>
```

- [ ] **Step 2: Maven reload**

---

### Task 3.2: Verify web-scraping.dev login flow (manual reconnaissance)

> **Important:** înainte să scriem cod, trebuie să știm CE credențiale și CE selectoare CSS folosește site-ul. Asta NU se poate doar din spec.

- [ ] **Step 1: Open in browser** https://www.web-scraping.dev/login

- [ ] **Step 2: Citește pagina** — caută credențialele demo afișate vizibil. La momentul scrierii spec-ului erau `user123` / `password`, dar **verifică**.

- [ ] **Step 3: Login manual cu credențialele afișate. Verifică că ajungi pe o pagină authentificată.**

- [ ] **Step 4: Open** https://www.web-scraping.dev/products?category=consumables

- [ ] **Step 5: Right-click → Inspect pe un product card. Notează:**
  - Class container pentru un product card (ex: `div.product`)
  - Class pentru poza (ex: `img.product-image`)
  - Class pentru nume (ex: `h3.product-title`)
  - Class pentru preț (ex: `span.product-price`)
  - Class pentru descriere (ex: `div.product-description`)

- [ ] **Step 6: Notează aici (înlocuiește înainte de implementare):**

```
- Credentials: user=___ password=___
- Product card: ___
- Image:       ___
- Name:        ___
- Price:       ___
- Description: ___
```

**Pause: dacă login-ul cere CSRF token sau JS, switch la Selenium (vezi Task 3.10 - fallback).**

---

### Task 3.3: Create Scraper interface

**Files:**
- Create: `src/main/java/ro/autobrand/proba/scraper/Scraper.java`
- Create: `src/main/java/ro/autobrand/proba/dto/ScrapedProductDto.java`

- [ ] **Step 1: Define DTO:**

```java
package ro.autobrand.proba.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class ScrapedProductDto {
    String name;
    String description;
    BigDecimal price;
    String currency;
    String imageUrl;
    String sourceUrl;
}
```

> `@Value` = `@Data` + final fields + no setters → immutable.

- [ ] **Step 2: Define interface:**

```java
package ro.autobrand.proba.scraper;

import ro.autobrand.proba.dto.ScrapedProductDto;

import java.util.List;

public interface Scraper {
    List<ScrapedProductDto> scrape();
}
```

---

### Task 3.4: Save sample HTML fixture (manual)

**Files:**
- Create: `src/test/resources/fixtures/products-page.html`

- [ ] **Step 1: Open** https://www.web-scraping.dev/products?category=consumables logat

- [ ] **Step 2: Ctrl+U → View source**

- [ ] **Step 3: Copiază totul → salvează ca `src/test/resources/fixtures/products-page.html`**

Asta va fi fixture-ul pentru testul de parser, fără a hit-ui rețeaua în CI.

---

### Task 3.5: Write test for WebScrapingDevScraper.parseProducts() (TDD)

**Files:**
- Create: `src/test/java/ro/autobrand/proba/scraper/WebScrapingDevScraperTest.java`

- [ ] **Step 1: Write failing test:**

```java
package ro.autobrand.proba.scraper;

import org.junit.jupiter.api.Test;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import ro.autobrand.proba.dto.ScrapedProductDto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebScrapingDevScraperTest {

    @Test
    void parses_products_from_fixture_html() throws IOException {
        Document doc;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/products-page.html")) {
            doc = Jsoup.parse(in, StandardCharsets.UTF_8.name(), "https://www.web-scraping.dev/");
        }

        WebScrapingDevScraper scraper = new WebScrapingDevScraper(null, null, null); // creds + base URL injected
        List<ScrapedProductDto> products = scraper.parseProducts(doc);

        assertThat(products).isNotEmpty();
        ScrapedProductDto first = products.get(0);
        assertThat(first.getName()).isNotBlank();
        assertThat(first.getPrice()).isPositive();
        assertThat(first.getCurrency()).hasSize(3);
        assertThat(first.getImageUrl()).startsWith("http");
    }
}
```

- [ ] **Step 2: Run, expected FAIL** ("WebScrapingDevScraper not found")

---

### Task 3.6: Implement WebScrapingDevScraper

**Files:**
- Create: `src/main/java/ro/autobrand/proba/scraper/WebScrapingDevScraper.java`

- [ ] **Step 1: Skeleton + parseProducts (folosește selectoarele tale notate la Task 3.2):**

```java
package ro.autobrand.proba.scraper;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ro.autobrand.proba.dto.ScrapedProductDto;
import ro.autobrand.proba.exception.ScrapingException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class WebScrapingDevScraper implements Scraper {

    private static final Pattern PRICE_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*([A-Z]{3}|\\$|€|£)");

    private final String baseUrl;
    private final String username;
    private final String password;

    public WebScrapingDevScraper(
            @Value("${app.scraping.base-url:https://www.web-scraping.dev}") String baseUrl,
            @Value("${app.scraping.username:user123}") String username,
            @Value("${app.scraping.password:password}") String password
    ) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    @Override
    public List<ScrapedProductDto> scrape() {
        try {
            Map<String, String> cookies = login();
            Document doc = fetchProductsPage(cookies);
            return parseProducts(doc);
        } catch (IOException e) {
            log.error("Scraping failed", e);
            throw new ScrapingException("Failed to scrape products", e);
        }
    }

    Map<String, String> login() throws IOException {
        Connection.Response res = Jsoup.connect(baseUrl + "/login")
                .data("username", username)
                .data("password", password)
                .method(Connection.Method.POST)
                .followRedirects(true)
                .execute();
        if (res.statusCode() >= 400) {
            throw new IOException("Login failed: HTTP " + res.statusCode());
        }
        log.info("Login succeeded, cookies: {}", res.cookies().keySet());
        return res.cookies();
    }

    Document fetchProductsPage(Map<String, String> cookies) throws IOException {
        return Jsoup.connect(baseUrl + "/products?category=consumables")
                .cookies(cookies)
                .get();
    }

    List<ScrapedProductDto> parseProducts(Document doc) {
        // TODO: înlocuiește selectorii cu cei reali notați la Task 3.2
        List<ScrapedProductDto> result = new ArrayList<>();
        for (Element card : doc.select("div.product")) {           // <-- selector container
            String name = textOrEmpty(card, "h3.product-title");   // <-- selector nume
            String desc = textOrEmpty(card, "div.description");    // <-- selector descriere
            String priceRaw = textOrEmpty(card, "span.price");     // <-- selector preț
            String imageUrl = card.selectFirst("img") != null
                    ? card.selectFirst("img").absUrl("src") : null;
            String sourceUrl = card.selectFirst("a") != null
                    ? card.selectFirst("a").absUrl("href") : null;

            PriceParsed parsed = parsePrice(priceRaw);
            if (parsed == null) {
                log.warn("Skipping product '{}' due to unparseable price", name);
                continue;
            }
            if (name == null || name.isBlank()) {
                log.warn("Skipping product card with blank name");
                continue;
            }

            result.add(ScrapedProductDto.builder()
                    .name(name)
                    .description(desc)
                    .price(parsed.amount())
                    .currency(parsed.currency())
                    .imageUrl(imageUrl)
                    .sourceUrl(sourceUrl)
                    .build());
        }
        log.info("Parsed {} products", result.size());
        return result;
    }

    private String textOrEmpty(Element parent, String selector) {
        Element el = parent.selectFirst(selector);
        return el == null ? "" : el.text();
    }

    private PriceParsed parsePrice(String raw) {
        Matcher m = PRICE_PATTERN.matcher(raw);
        if (!m.find()) {
            log.warn("Could not parse price from raw text: '{}' — skipping product", raw);
            return null; // semnalează skip; caller filtrează
        }
        BigDecimal amount = new BigDecimal(m.group(1));
        String symbol = m.group(2);
        String currency = switch (symbol) {
            case "$" -> "USD";
            case "€" -> "EUR";
            case "£" -> "GBP";
            default -> symbol;
        };
        return new PriceParsed(amount, currency);
    }

    private record PriceParsed(BigDecimal amount, String currency) {
    }
}
```

- [ ] **Step 2: Create exception class:**

```java
package ro.autobrand.proba.scraper;

public class ScrapingException extends RuntimeException {
    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 3: Update test constructor** (acum cere baseUrl, username, password — pasează `null` sau valori test):

```java
WebScrapingDevScraper scraper = new WebScrapingDevScraper("https://test", "u", "p");
```

- [ ] **Step 4: Run test**

```powershell
./mvnw test -Dtest=WebScrapingDevScraperTest
```

Expected: PASS dacă selectoarele match-uiesc fixture-ul. Dacă nu, ajustează selectoarele în `parseProducts`.

---

### Task 3.7: Test ProductService.upsertAll() logic (TDD)

**Files:**
- Create: `src/test/java/ro/autobrand/proba/service/ProductServiceTest.java`

- [ ] **Step 1: Write failing test:**

```java
package ro.autobrand.proba.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.autobrand.proba.dto.ScrapedProductDto;
import ro.autobrand.proba.model.Product;
import ro.autobrand.proba.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    ProductRepository repo;
    ProductService service;

    @BeforeEach
    void setUp() {
        repo = mock(ProductRepository.class);
        service = new ProductService(repo);
    }

    @Test
    void inserts_new_product() {
        when(repo.findByName("Apple")).thenReturn(Optional.empty());

        service.upsertAll(List.of(scraped("Apple", "9.99")));

        verify(repo).save(argThat(p ->
                p.getName().equals("Apple") &&
                p.getPrice().compareTo(new BigDecimal("9.99")) == 0
        ));
    }

    @Test
    void updates_existing_when_not_manually_edited() {
        Product existing = Product.builder()
                .id(1L)
                .name("Apple")
                .price(new BigDecimal("5.00"))
                .currency("USD")
                .manuallyEdited(false)
                .firstSeen(LocalDateTime.now().minusDays(1))
                .build();
        when(repo.findByName("Apple")).thenReturn(Optional.of(existing));

        service.upsertAll(List.of(scraped("Apple", "9.99")));

        verify(repo).save(argThat(p ->
                p.getPrice().compareTo(new BigDecimal("9.99")) == 0 &&
                !p.isManuallyEdited()
        ));
    }

    @Test
    void preserves_manually_edited_fields_only_updates_last_scraped() {
        Product existing = Product.builder()
                .id(2L)
                .name("Banana")
                .price(new BigDecimal("3.00"))
                .currency("EUR")
                .description("Edited manually")
                .manuallyEdited(true)
                .firstSeen(LocalDateTime.now().minusDays(2))
                .lastScraped(LocalDateTime.now().minusHours(2))
                .build();
        when(repo.findByName("Banana")).thenReturn(Optional.of(existing));

        service.upsertAll(List.of(scraped("Banana", "4.50")));

        verify(repo).save(argThat(p ->
                p.getPrice().compareTo(new BigDecimal("3.00")) == 0 && // NESCHIMBAT
                p.getDescription().equals("Edited manually") &&         // NESCHIMBAT
                p.isManuallyEdited() &&
                p.getLastScraped().isAfter(existing.getLastScraped())   // UPDATED
        ));
    }

    private ScrapedProductDto scraped(String name, String price) {
        return ScrapedProductDto.builder()
                .name(name)
                .price(new BigDecimal(price))
                .currency("USD")
                .description("desc")
                .imageUrl("https://img")
                .sourceUrl("https://src")
                .build();
    }
}
```

- [ ] **Step 2: Run, expected FAIL**

---

### Task 3.8: Implement ProductService

**Files:**
- Create: `src/main/java/ro/autobrand/proba/service/ProductService.java`

- [ ] **Step 1: Write:**

```java
package ro.autobrand.proba.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.autobrand.proba.dto.ScrapedProductDto;
import ro.autobrand.proba.model.Product;
import ro.autobrand.proba.repository.ProductRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository repository;

    @Transactional
    public UpsertResult upsertAll(List<ScrapedProductDto> scraped) {
        int inserted = 0;
        int updated = 0;
        int preserved = 0;
        LocalDateTime now = LocalDateTime.now();

        for (ScrapedProductDto s : scraped) {
            Optional<Product> existing = repository.findByName(s.getName());
            if (existing.isEmpty()) {
                repository.save(Product.builder()
                        .name(s.getName())
                        .description(s.getDescription())
                        .price(s.getPrice())
                        .currency(s.getCurrency())
                        .imageUrl(s.getImageUrl())
                        .sourceUrl(s.getSourceUrl())
                        .manuallyEdited(false)
                        .firstSeen(now)
                        .lastScraped(now)
                        .updatedAt(now)
                        .build());
                inserted++;
            } else {
                Product p = existing.get();
                if (p.isManuallyEdited()) {
                    p.setLastScraped(now);
                    repository.save(p);
                    preserved++;
                } else {
                    p.setDescription(s.getDescription());
                    p.setPrice(s.getPrice());
                    p.setCurrency(s.getCurrency());
                    p.setImageUrl(s.getImageUrl());
                    p.setSourceUrl(s.getSourceUrl());
                    p.setLastScraped(now);
                    repository.save(p);
                    updated++;
                }
            }
        }
        log.info("Upsert done: {} inserted, {} updated, {} preserved (manually edited)",
                inserted, updated, preserved);
        return new UpsertResult(inserted, updated, preserved);
    }

    public record UpsertResult(int inserted, int updated, int preserved) {
        public int total() { return inserted + updated + preserved; }
    }
}
```

> **Concepte:**
> - `@Service` — marker pentru Spring că asta e un bean (componentă gestionată).
> - `@RequiredArgsConstructor` (Lombok) — generează constructor cu toate field-urile `final`. Înlocuiește `@Autowired`.
> - `@Transactional` — Spring wrap-uiește metoda într-o tranzacție DB. Tot ce face e commit la final sau rollback la exception.

- [ ] **Step 2: Run test**

```powershell
./mvnw test -Dtest=ProductServiceTest
```

Expected: 3 PASS.

---

### Task 3.9: Wire ScrapingService.runScrape()

**Files:**
- Create: `src/main/java/ro/autobrand/proba/service/ScrapingService.java`

- [ ] **Step 1: Write:**

```java
package ro.autobrand.proba.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ro.autobrand.proba.dto.ScrapedProductDto;
import ro.autobrand.proba.scraper.Scraper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapingService {

    private final Scraper scraper;
    private final ProductService productService;

    public ProductService.UpsertResult runScrape() {
        log.info("Starting scrape run");
        List<ScrapedProductDto> scraped = scraper.scrape();
        ProductService.UpsertResult result = productService.upsertAll(scraped);
        log.info("Scrape run completed: {}", result);
        return result;
    }
}
```

---

### Task 3.10: Add manual scrape trigger endpoint

**Files:**
- Create: `src/main/java/ro/autobrand/proba/controller/ScrapeAdminController.java`

- [ ] **Step 1: Write:**

```java
package ro.autobrand.proba.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.autobrand.proba.service.ProductService;
import ro.autobrand.proba.service.ScrapingService;

@Controller
@RequestMapping("/admin/scrape")
@RequiredArgsConstructor
public class ScrapeAdminController {

    private final ScrapingService scrapingService;

    @PostMapping
    public String runNow(RedirectAttributes ra) {
        ProductService.UpsertResult result = scrapingService.runScrape();
        ra.addFlashAttribute("success",
                "Scrape: %d adăugate, %d actualizate, %d păstrate (editate manual)"
                        .formatted(result.inserted(), result.updated(), result.preserved()));
        return "redirect:/products";
    }
}
```

---

### Task 3.11: Add scraper config to application-dev.yml

**Files:**
- Modify: `src/main/resources/application-dev.yml`

- [ ] **Step 1: Append:**

```yaml
app:
  scraping:
    base-url: https://www.web-scraping.dev
    username: ${SCRAPER_USERNAME:user123}    # înlocuiește cu cel real notat la 3.2
    password: ${SCRAPER_PASSWORD:password}   # idem
```

---

### Task 3.12: Manual end-to-end test

- [ ] **Step 1: Asigură DB pornit:** `docker compose up -d db`

- [ ] **Step 2: Pornește app** (▶ în IntelliJ)

- [ ] **Step 3: Browser** `http://localhost:8080/products` → 404 (n-avem încă controller-ul products, va veni în Phase 4). Folosim `curl` să testăm scrape-ul.

- [ ] **Step 4: Trigger scrape din PowerShell** (CSRF disabled până când punem Spring Security config):

```powershell
curl -X POST http://localhost:8080/admin/scrape -i
```

> **Notă:** Spring Security default cere autentificare + CSRF. Pentru moment, dezactivăm Security autoconfig **temporar** — vom configura proper în Phase 8.

În `application.yml` adaugă temporar (la nivel top, sub `spring:`):
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

> **Atenție:** asta dezactivează **toate** filter-ele de securitate Spring (NU adaugă utilizator default — fără filter, fără auth check). Endpoint-urile sunt accesibile fără login până în Phase 8.

Restart, retry POST. Expected: 302 redirect + log "Scrape run completed: UpsertResult[inserted=N, ...]".

- [ ] **Step 5: Verify DB**

```powershell
docker exec -it autobrand-db psql -U autobrand -d autobrand -c "SELECT id, name, price, currency FROM product LIMIT 5;"
```

Expected: produse populate.

**Pause: dacă scrape fails cu "Login failed" sau "0 products parsed", verifică Task 3.2 (selectoare CSS + credențiale).**

---

### Task 3.13: Commit

```powershell
git add src/ pom.xml
git commit -m "feat: implement web scraping with Jsoup + product upsert

- Scraper interface + WebScrapingDevScraper (Jsoup-based)
- Login with form POST + cookie-based session
- ProductService.upsertAll handles new/updated/manually_edited cases
- ScrapingService orchestrates scrape runs
- POST /admin/scrape for manual trigger
- HTML fixture for offline parser tests
- Temporary Security autoconfig exclusion (will be replaced in Phase 8)"
```

---

# Phase 4 — UI Base: Thymeleaf + Tailwind + Product List

> Scope: layout fragment, Tailwind+DaisyUI CDN, listă produse simplă.

### Task 4.1: Create layout fragment

**Files:**
- Create: `src/main/resources/templates/fragments/layout.html`

- [ ] **Step 1: Write:**

```html
<!DOCTYPE html>
<html lang="ro" xmlns:th="http://www.thymeleaf.org" th:fragment="layout(title, content)">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title} + ' — Autobrand'">Autobrand</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdn.jsdelivr.net/npm/daisyui@4.12.10/dist/full.min.css" rel="stylesheet">
    <script src="https://unpkg.com/htmx.org@2.0.2"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script>
        // Theme toggle
        function toggleTheme() {
            const html = document.documentElement;
            const next = html.getAttribute('data-theme') === 'corporate' ? 'dim' : 'corporate';
            html.setAttribute('data-theme', next);
            localStorage.setItem('theme', next);
        }
        document.addEventListener('DOMContentLoaded', () => {
            const saved = localStorage.getItem('theme') || 'corporate';
            document.documentElement.setAttribute('data-theme', saved);
        });
    </script>
</head>
<body class="min-h-screen bg-base-200" data-theme="corporate">
<div class="drawer lg:drawer-open">
    <input id="drawer-toggle" type="checkbox" class="drawer-toggle"/>
    <div class="drawer-content flex flex-col">
        <!-- Top navbar -->
        <div class="navbar bg-base-100 shadow">
            <div class="flex-none lg:hidden">
                <label for="drawer-toggle" class="btn btn-square btn-ghost">☰</label>
            </div>
            <div class="flex-1 px-2 text-xl font-bold">🚗 Autobrand</div>
            <button class="btn btn-ghost btn-circle" onclick="toggleTheme()" title="Toggle theme">🌓</button>
            <form th:action="@{/logout}" method="post" class="inline">
                <button type="submit" class="btn btn-ghost btn-sm">Logout</button>
            </form>
        </div>

        <!-- Flash messages -->
        <div class="p-4">
            <div th:if="${success}" class="alert alert-success" th:text="${success}"></div>
            <div th:if="${error}" class="alert alert-error" th:text="${error}"></div>
        </div>

        <!-- Main content -->
        <main class="p-4 flex-1">
            <th:block th:replace="${content}"></th:block>
        </main>
    </div>

    <!-- Sidebar -->
    <div class="drawer-side">
        <label for="drawer-toggle" class="drawer-overlay"></label>
        <ul class="menu p-4 w-64 min-h-full bg-base-100">
            <li class="text-sm font-bold opacity-50 px-2 mb-2">Navigation</li>
            <li><a th:href="@{/}" class="font-medium">🏠 Dashboard</a></li>
            <li><a th:href="@{/products}">📦 Products</a></li>
            <li><a th:href="@{/invoice}">📄 Upload Invoice</a></li>
            <li class="mt-4 text-sm font-bold opacity-50 px-2">Actions</li>
            <li>
                <form th:action="@{/admin/scrape}" method="post">
                    <button type="submit" class="w-full text-left">🔄 Run Scrape Now</button>
                </form>
            </li>
        </ul>
    </div>
</div>
</body>
</html>
```

---

### Task 4.2: Create products list controller + template

**Files:**
- Create: `src/main/java/ro/autobrand/proba/controller/ProductController.java`
- Create: `src/main/resources/templates/products/list.html`

- [ ] **Step 1: Write controller (versiune minimă, vom extinde la filter/sort în Phase 9):**

```java
package ro.autobrand.proba.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ro.autobrand.proba.model.Product;
import ro.autobrand.proba.repository.ProductRepository;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repository;

    @GetMapping
    public String list(
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            Model model
    ) {
        Page<Product> page = repository.findAll(pageable);
        model.addAttribute("page", page);
        return "products/list";
    }
}
```

- [ ] **Step 2: Write template:**

```html
<!DOCTYPE html>
<html lang="ro" xmlns:th="http://www.thymeleaf.org">
<head><title>Products</title></head>
<body>
<th:block th:replace="~{fragments/layout :: layout('Products', ~{::main-content})}">
    <th:block th:fragment="main-content">
        <h1 class="text-2xl font-bold mb-4">Products</h1>

        <div class="overflow-x-auto bg-base-100 rounded-lg shadow">
            <table class="table table-zebra">
                <thead>
                <tr>
                    <th>Image</th>
                    <th>Name</th>
                    <th>Description</th>
                    <th>Price</th>
                    <th>RON</th>
                    <th>Edited?</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                <tr th:each="p : ${page.content}">
                    <td><img th:if="${p.imageUrl}" th:src="${p.imageUrl}" class="w-12 h-12 object-cover rounded"/></td>
                    <td th:text="${p.name}"></td>
                    <td th:text="${#strings.abbreviate(p.description, 50)}"></td>
                    <td th:text="${p.price + ' ' + p.currency}"></td>
                    <td th:text="${p.priceRon != null ? p.priceRon + ' RON' : '-'}"></td>
                    <td>
                        <span th:if="${p.manuallyEdited}" class="badge badge-warning">✏</span>
                    </td>
                    <td>
                        <a th:href="@{/products/{id}/edit(id=${p.id})}" class="btn btn-sm btn-primary">Edit</a>
                        <!-- Delete va veni cu HTMX în Phase 5 -->
                    </td>
                </tr>
                <tr th:if="${page.empty}">
                    <td colspan="7" class="text-center opacity-50 py-8">No products yet. Click "Run Scrape Now" în sidebar.</td>
                </tr>
                </tbody>
            </table>
        </div>

        <div class="mt-4 flex gap-2 items-center" th:if="${page.totalPages > 1}">
            <span th:text="${'Page ' + (page.number + 1) + ' of ' + page.totalPages}"></span>
        </div>
    </th:block>
</th:block>
</body>
</html>
```

---

### Task 4.3: Verify în browser

- [ ] **Step 1: Restart app** (IntelliJ ▶ Stop, apoi Run again)

- [ ] **Step 2:** Browser → `http://localhost:8080/products`

Expected: Layout cu sidebar, navbar cu logo + toggle theme, tabel produse (sau "No products yet").

- [ ] **Step 3: Click "Run Scrape Now"** în sidebar → flash message success → tabel populat.

- [ ] **Step 4: Click pe 🌓** → theme schimbă între light (corporate) și dark (dim).

---

### Task 4.4: Commit

```powershell
git add src/
git commit -m "feat: add Thymeleaf layout with Tailwind+DaisyUI and products list page

- Drawer layout with sidebar navigation
- Tailwind Play CDN + DaisyUI + HTMX + Chart.js loaded
- Theme toggle (corporate/dim) persisted in localStorage
- ProductController lists products with pagination
- Empty state message when no products"
```

---

# Phase 5 — CRUD: Edit, Delete, Reset

> Scope: form de edit cu Bean Validation, delete via HTMX, reset flag.

### Task 5.1: ProductDto cu validation

**Files:**
- Create: `src/main/java/ro/autobrand/proba/dto/ProductDto.java`

- [ ] **Step 1: Write:**

```java
package ro.autobrand.proba.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import ro.autobrand.proba.model.Product;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {
    private Long id;

    @NotBlank(message = "Numele e obligatoriu")
    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    @NotNull(message = "Prețul e obligatoriu")
    @Positive(message = "Prețul trebuie să fie pozitiv")
    private BigDecimal price;

    @NotBlank
    @Size(min = 3, max = 3, message = "Moneda trebuie să fie cod ISO de 3 litere")
    private String currency;

    @Size(max = 1000)
    private String imageUrl;

    @Size(max = 1000)
    private String sourceUrl;

    public static ProductDto from(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .imageUrl(p.getImageUrl())
                .sourceUrl(p.getSourceUrl())
                .build();
    }

    public void applyTo(Product p) {
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setCurrency(currency);
        p.setImageUrl(imageUrl);
        p.setSourceUrl(sourceUrl);
        p.setManuallyEdited(true);
    }
}
```

---

### Task 5.2: Add edit + update + delete + reset endpoints

**Files:**
- Modify: `src/main/java/ro/autobrand/proba/controller/ProductController.java`

- [ ] **Step 1: Replace controller content:**

```java
package ro.autobrand.proba.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.autobrand.proba.dto.ProductDto;
import ro.autobrand.proba.exception.ProductNotFoundException;
import ro.autobrand.proba.model.Product;
import ro.autobrand.proba.repository.ProductRepository;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repository;

    @GetMapping
    public String list(
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            Model model
    ) {
        Page<Product> page = repository.findAll(pageable);
        model.addAttribute("page", page);
        return "products/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product p = repository.findById(id).orElseThrow(() ->
                new ProductNotFoundException("Product not found: " + id));
        model.addAttribute("productDto", ProductDto.from(p));
        model.addAttribute("product", p);
        return "products/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("productDto") ProductDto dto,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model) {
        Product p = repository.findById(id).orElseThrow(() ->
                new ProductNotFoundException("Product not found: " + id));
        if (bindingResult.hasErrors()) {
            model.addAttribute("product", p);
            return "products/edit";
        }
        dto.applyTo(p);
        repository.save(p);
        ra.addFlashAttribute("success", "Produs actualizat");
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        repository.deleteById(id);
        ra.addFlashAttribute("success", "Produs șters");
        return "redirect:/products";
    }

    @PostMapping("/{id}/reset")
    public String reset(@PathVariable Long id, RedirectAttributes ra) {
        Product p = repository.findById(id).orElseThrow(() ->
                new ProductNotFoundException("Product not found: " + id));
        p.setManuallyEdited(false);
        repository.save(p);
        ra.addFlashAttribute("success", "Flag manually_edited resetat; la următorul scrape, produsul va fi rescris cu datele scraped.");
        return "redirect:/products";
    }
}
```

- [ ] **Step 2: Create exception:**

```java
package ro.autobrand.proba.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
```

---

### Task 5.3: Edit template

**Files:**
- Create: `src/main/resources/templates/products/edit.html`

```html
<!DOCTYPE html>
<html lang="ro" xmlns:th="http://www.thymeleaf.org">
<head><title>Edit</title></head>
<body>
<th:block th:replace="~{fragments/layout :: layout('Edit Product', ~{::main-content})}">
    <th:block th:fragment="main-content">
        <h1 class="text-2xl font-bold mb-4">Edit Product #<span th:text="${product.id}"></span></h1>

        <form th:action="@{/products/{id}(id=${product.id})}" method="post" th:object="${productDto}"
              class="card bg-base-100 shadow p-6 max-w-2xl space-y-4">

            <div class="form-control">
                <label class="label"><span class="label-text">Name *</span></label>
                <input type="text" th:field="*{name}" class="input input-bordered" required/>
                <span th:if="${#fields.hasErrors('name')}" th:errors="*{name}" class="text-error text-sm"></span>
            </div>

            <div class="form-control">
                <label class="label"><span class="label-text">Description</span></label>
                <textarea th:field="*{description}" class="textarea textarea-bordered" rows="4"></textarea>
            </div>

            <div class="grid grid-cols-2 gap-4">
                <div class="form-control">
                    <label class="label"><span class="label-text">Price *</span></label>
                    <input type="number" step="0.01" th:field="*{price}" class="input input-bordered" required/>
                    <span th:if="${#fields.hasErrors('price')}" th:errors="*{price}" class="text-error text-sm"></span>
                </div>
                <div class="form-control">
                    <label class="label"><span class="label-text">Currency *</span></label>
                    <input type="text" th:field="*{currency}" maxlength="3" class="input input-bordered uppercase" required/>
                </div>
            </div>

            <div class="form-control">
                <label class="label"><span class="label-text">Image URL</span></label>
                <input type="url" th:field="*{imageUrl}" class="input input-bordered"/>
                <img th:if="${productDto.imageUrl}" th:src="${productDto.imageUrl}"
                     class="w-32 h-32 object-cover rounded mt-2"/>
            </div>

            <div class="form-control">
                <label class="label"><span class="label-text">Source URL</span></label>
                <input type="url" th:field="*{sourceUrl}" class="input input-bordered"/>
            </div>

            <div class="text-sm opacity-70">
                Last scraped: <span th:text="${product.lastScraped}"></span><br>
                First seen: <span th:text="${product.firstSeen}"></span><br>
                Manually edited: <span th:text="${product.manuallyEdited}"></span>
            </div>

            <div class="flex gap-2 justify-end">
                <a th:href="@{/products}" class="btn btn-ghost">Cancel</a>
                <button th:if="${product.manuallyEdited}" type="submit"
                        th:formaction="@{/products/{id}/reset(id=${product.id})}"
                        class="btn btn-warning">Reset to scraped</button>
                <button type="submit" class="btn btn-primary">Save changes</button>
            </div>
        </form>

        <form th:action="@{/products/{id}/delete(id=${product.id})}" method="post"
              class="mt-4 max-w-2xl"
              onsubmit="return confirm('Sigur ștergi produsul?');">
            <button type="submit" class="btn btn-error btn-outline">🗑 Delete product</button>
        </form>
    </th:block>
</th:block>
</body>
</html>
```

---

### Task 5.4: Test CRUD manually

- [ ] **Step 1: Restart app**
- [ ] **Step 2: `/products` → Edit pe un produs**
- [ ] **Step 3: Modifică prețul → Save**
- [ ] **Step 4: Verifică în listă: are badge ✏ (manually edited)**
- [ ] **Step 5: Re-deschide editul, Reset to scraped**
- [ ] **Step 6: Verifică: badge dispare**
- [ ] **Step 7: Delete un produs → confirmare → produs dispărut din listă**

---

### Task 5.5: Commit

```powershell
git add src/
git commit -m "feat: add product CRUD with Bean Validation

- ProductDto with @NotBlank/@Positive/@Size validations
- Edit form with validation error display
- Update sets manually_edited=true
- Delete with confirm dialog
- Reset to scraped flag (clears manually_edited)
- Image preview in edit form"
```

---

# Phase 6 — Cron Scheduler

### Task 6.1: Enable scheduling

**Files:**
- Create: `src/main/java/ro/autobrand/proba/config/SchedulerConfig.java`

```java
package ro.autobrand.proba.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
}
```

---

### Task 6.2: Add scheduled task într-o clasă separată

**Files:**
- Create: `src/main/java/ro/autobrand/proba/service/ScheduledScrapeTask.java`

> **Important pentru beginner:** `@ConditionalOnProperty` funcționează la nivel de **bean creation** (clase / metode `@Bean`), NU pe metode `@Scheduled` individuale. Dacă o pui pe metoda `@Scheduled`, e ignorată — scheduler-ul tot va rula. Soluția corectă: pune `@Scheduled` într-o clasă separată cu `@ConditionalOnProperty` la nivel de clasă, astfel întreaga clasă (și implicit scheduler-ul) e creată doar când flag-ul e `true`.

- [ ] **Step 1: Create clasa scheduled:**

```java
package ro.autobrand.proba.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.scraping.cron-enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ScheduledScrapeTask {

    private final ScrapingService scrapingService;

    @Scheduled(cron = "${app.scraping.cron:0 0 12-18 * * *}")
    public void scheduledRun() {
        log.info("Scheduled scrape triggered");
        try {
            scrapingService.runScrape();
        } catch (Exception e) {
            log.error("Scheduled scrape failed", e);
        }
    }
}
```

> **Cron Spring format:** 6 fields = `secunde minute ore zi-luna luna zi-saptamana`. `0 0 12-18 * * *` = ora 12, 13, ..., 18 fix.
>
> **De ce clasa separată (interview defense):** *"`@ConditionalOnProperty` la nivel de clasă oprește Spring să creeze bean-ul când flag-ul e false; fără bean, fără scheduler. Dacă l-aș fi pus direct pe `@Scheduled`, scheduler-ul ar fi rulat oricum — annotation-urile condiționale nu se aplică pe metode `@Scheduled` în Spring."*

---

### Task 6.3: Run scrape on application ready

**Files:**
- Modify: `src/main/java/ro/autobrand/proba/service/ScrapingService.java`

- [ ] **Step 1: Add listener:**

```java
@EventListener(ApplicationReadyEvent.class)
public void runOnStartup() {
    if (runOnStartup) {
        log.info("Running scrape on application startup");
        try {
            runScrape();
        } catch (Exception e) {
            log.error("Startup scrape failed", e);
        }
    }
}
```

cu field:
```java
@Value("${app.scraping.run-on-startup:false}")
private boolean runOnStartup;
```

---

### Task 6.4: Update profile configs

**Files:**
- Modify: `application-dev.yml` — add `cron-enabled: false`, `run-on-startup: false`
- Create: `application-docker.yml`

```yaml
# application-docker.yml
spring:
  datasource:
    url: jdbc:postgresql://db:5432/autobrand
    username: autobrand
    password: dev_password

app:
  scraping:
    base-url: https://www.web-scraping.dev
    username: ${SCRAPER_USERNAME:user123}
    password: ${SCRAPER_PASSWORD:password}
    cron-enabled: true
    run-on-startup: true
```

---

### Task 6.5: Commit

```powershell
git add src/
git commit -m "feat: add @Scheduled cron 12-18 + ApplicationReadyEvent trigger

- Configurable via app.scraping.cron-enabled / run-on-startup
- Dev profile: both off (manual trigger only)
- Docker profile: both on
- Cron expression: 0 0 12-18 * * *"
```

---

# Phase 7 — PDF Invoice Upload + CSV Export

### Task 7.1: Add dependencies

**Files:**
- Modify: `pom.xml`

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-csv</artifactId>
    <version>1.11.0</version>
</dependency>
```

Maven reload.

---

### Task 7.2: InvoiceParser interface + DTO

**Files:**
- Create: `src/main/java/ro/autobrand/proba/dto/InvoiceLineDto.java`
- Create: `src/main/java/ro/autobrand/proba/pdf/InvoiceParser.java`

```java
// InvoiceLineDto.java
package ro.autobrand.proba.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class InvoiceLineDto {
    String productCode;
    String productName;
    BigDecimal unitPrice;
    String currency;
    BigDecimal quantity;
}
```

```java
// InvoiceParser.java
package ro.autobrand.proba.pdf;

import ro.autobrand.proba.dto.InvoiceLineDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface InvoiceParser {
    List<InvoiceLineDto> parse(InputStream pdfStream) throws IOException;
}
```

---

### Task 7.3: Copy sample PDF to test resources

- [ ] **Step 1: Create directory + copy:**

```powershell
New-Item -ItemType Directory -Force -Path "src\test\resources\fixtures"
Copy-Item "AD AUTO TOTAL SRL_20241747776_2024_03_01.PDF" "src\test\resources\fixtures\sample-invoice.pdf"
```

- [ ] **Step 2: Verify `git status` arată fișierul ca untracked.** (`.gitignore` curent ignoră PDF-urile doar la root, nu și în `src/test/resources/`.)

---

### Task 7.4: Test AdAutoTotalInvoiceParser (TDD)

**Files:**
- Create: `src/test/java/ro/autobrand/proba/pdf/AdAutoTotalInvoiceParserTest.java`

```java
package ro.autobrand.proba.pdf;

import org.junit.jupiter.api.Test;
import ro.autobrand.proba.dto.InvoiceLineDto;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdAutoTotalInvoiceParserTest {

    @Test
    void parses_lines_from_sample_invoice() throws Exception {
        AdAutoTotalInvoiceParser parser = new AdAutoTotalInvoiceParser();
        try (InputStream in = getClass().getResourceAsStream("/fixtures/sample-invoice.pdf")) {
            List<InvoiceLineDto> lines = parser.parse(in);

            assertThat(lines).isNotEmpty();
            InvoiceLineDto first = lines.get(0);
            assertThat(first.getProductCode()).isNotBlank();
            assertThat(first.getProductName()).isNotBlank();
            assertThat(first.getUnitPrice()).isPositive();
            assertThat(first.getQuantity()).isPositive();
            assertThat(first.getCurrency()).isEqualTo("RON");
        }
    }
}
```

Run, expected FAIL.

---

### Task 7.5: Implement AdAutoTotalInvoiceParser

**Files:**
- Create: `src/main/java/ro/autobrand/proba/pdf/AdAutoTotalInvoiceParser.java`

> **Important:** parser-ul concret depinde de structura textului PDF-ului. Înainte să scrii regex-urile, **rulează un PDFTextStripper manual** pe sample să vezi cum arată text-ul extras.

- [ ] **Step 1: Manual reconnaissance — într-un test sau main scratch:**

```java
try (PDDocument doc = Loader.loadPDF(new File("AD AUTO TOTAL SRL_20241747776_2024_03_01.PDF"))) {
    System.out.println(new PDFTextStripper().getText(doc));
}
```

Rulează, notează formatul liniilor. Identifică:
- Header coloane (de regulă o linie cu "Cod", "Denumire", "U.M.", "Cantitate", "Preț", "Valoare")
- Format linie produs (separare prin spații multiple?)
- Sfârșit secțiune produse (de regulă "Total", "TVA", "Total general")

- [ ] **Step 2: Implementare bazată pe ce ai văzut:**

```java
package ro.autobrand.proba.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import ro.autobrand.proba.dto.InvoiceLineDto;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class AdAutoTotalInvoiceParser implements InvoiceParser {

    // Pattern bazat pe sample-ul AD AUTO TOTAL — ADAPTEAZĂ după reconnaissance
    // Așteptare: cod denumire (multiple cuvinte) ... cantitate preț valoare
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^(\\S+)\\s+(.+?)\\s+(\\d+[.,]\\d+)\\s+(\\d+[.,]\\d+)\\s+\\d+[.,]\\d+\\s*$",
            Pattern.MULTILINE
    );

    @Override
    public List<InvoiceLineDto> parse(InputStream pdfStream) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfStream.readAllBytes())) {
            String text = new PDFTextStripper().getText(doc);
            log.debug("PDF text extracted, length={}", text.length());
            return extractLines(text);
        }
    }

    List<InvoiceLineDto> extractLines(String text) {
        List<InvoiceLineDto> result = new ArrayList<>();
        boolean inProductSection = false;
        for (String line : text.split("\\r?\\n")) {
            // marker section start/end — adaptează după sample
            if (line.contains("Cod") && line.contains("Denumire")) {
                inProductSection = true;
                continue;
            }
            if (line.contains("Total") || line.contains("TVA")) {
                inProductSection = false;
            }
            if (!inProductSection) continue;

            Matcher m = LINE_PATTERN.matcher(line.trim());
            if (m.matches()) {
                result.add(InvoiceLineDto.builder()
                        .productCode(m.group(1))
                        .productName(m.group(2).trim())
                        .quantity(toBigDecimal(m.group(3)))
                        .unitPrice(toBigDecimal(m.group(4)))
                        .currency("RON")
                        .build());
            }
        }
        log.info("Extracted {} invoice lines", result.size());
        return result;
    }

    private BigDecimal toBigDecimal(String s) {
        return new BigDecimal(s.replace(",", "."));
    }
}
```

- [ ] **Step 3: Run test**

```powershell
./mvnw test -Dtest=AdAutoTotalInvoiceParserTest
```

Itereaza regex + section markers până trece testul.

**Pause: dacă PDF-ul are layout multicoloană sau text inversat, ar putea fi nevoie de `PDFTextStripperByArea` (zone-based extraction). Notează ce găsești.**

---

### Task 7.6: CsvExportService

**Files:**
- Create: `src/main/java/ro/autobrand/proba/service/CsvExportService.java`
- Create: `src/test/java/ro/autobrand/proba/service/CsvExportServiceTest.java`

- [ ] **Step 1: Test first:**

```java
package ro.autobrand.proba.service;

import org.junit.jupiter.api.Test;
import ro.autobrand.proba.dto.InvoiceLineDto;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    CsvExportService service = new CsvExportService();

    @Test
    void includes_utf8_bom_and_header() {
        byte[] result = service.toCsv(List.of(
                InvoiceLineDto.builder()
                        .productCode("ABC123")
                        .productName("Filtru aer")
                        .unitPrice(new BigDecimal("25.50"))
                        .currency("RON")
                        .quantity(new BigDecimal("2"))
                        .build()
        ));

        String csv = new String(result, java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).startsWith("﻿"); // BOM
        assertThat(csv).contains("cod_produs,denumire,pret_unitar,moneda,cantitate");
        assertThat(csv).contains("ABC123,Filtru aer,25.50,RON,2");
    }
}
```

- [ ] **Step 2: Implementation:**

```java
package ro.autobrand.proba.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import ro.autobrand.proba.dto.InvoiceLineDto;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CsvExportService {

    private static final String[] HEADER = {
            "cod_produs", "denumire", "pret_unitar", "moneda", "cantitate"
    };

    public byte[] toCsv(List<InvoiceLineDto> lines) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xEF); out.write(0xBB); out.write(0xBF); // UTF-8 BOM
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(HEADER).build())) {
            for (InvoiceLineDto line : lines) {
                printer.printRecord(
                        line.getProductCode(),
                        line.getProductName(),
                        line.getUnitPrice(),
                        line.getCurrency(),
                        line.getQuantity()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV generation failed", e);
        }
        return out.toByteArray();
    }
}
```

- [ ] **Step 3: Run test, PASS**

---

### Task 7.7: PdfInvoiceService + Controller + Template

**Files:**
- Create: `src/main/java/ro/autobrand/proba/service/PdfInvoiceService.java`
- Create: `src/main/java/ro/autobrand/proba/controller/InvoiceController.java`
- Create: `src/main/java/ro/autobrand/proba/exception/InvalidPdfException.java`
- Create: `src/main/resources/templates/invoice/upload.html`

- [ ] **Step 1: Exception class:**

```java
package ro.autobrand.proba.exception;
public class InvalidPdfException extends RuntimeException {
    public InvalidPdfException(String message) { super(message); }
}
```

- [ ] **Step 2: PdfInvoiceService:**

```java
package ro.autobrand.proba.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ro.autobrand.proba.dto.InvoiceLineDto;
import ro.autobrand.proba.exception.InvalidPdfException;
import ro.autobrand.proba.pdf.InvoiceParser;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfInvoiceService {

    private final InvoiceParser parser;
    private final CsvExportService csvExportService;

    public byte[] processToCsv(MultipartFile file) {
        validatePdf(file);
        try {
            List<InvoiceLineDto> lines = parser.parse(file.getInputStream());
            log.info("Parsed {} lines from {}", lines.size(), file.getOriginalFilename());
            return csvExportService.toCsv(lines);
        } catch (IOException e) {
            throw new InvalidPdfException("Eroare la citirea PDF-ului: " + e.getMessage());
        }
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidPdfException("Selectează un fișier.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new InvalidPdfException("Fișierul trebuie să fie PDF.");
        }
    }
}
```

- [ ] **Step 3: Controller:**

```java
package ro.autobrand.proba.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ro.autobrand.proba.service.PdfInvoiceService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final PdfInvoiceService service;

    @GetMapping
    public String form() {
        return "invoice/upload";
    }

    @PostMapping("/upload")
    public ResponseEntity<ByteArrayResource> upload(@RequestParam("file") MultipartFile file) {
        byte[] csv = service.processToCsv(file);
        String filename = "factura-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(csv));
    }
}
```

- [ ] **Step 4: Upload template:**

```html
<!DOCTYPE html>
<html lang="ro" xmlns:th="http://www.thymeleaf.org">
<head><title>Upload Invoice</title></head>
<body>
<th:block th:replace="~{fragments/layout :: layout('Upload Invoice', ~{::main-content})}">
    <th:block th:fragment="main-content">
        <h1 class="text-2xl font-bold mb-4">Upload Invoice (PDF)</h1>

        <form th:action="@{/invoice/upload}" method="post" enctype="multipart/form-data"
              class="card bg-base-100 shadow p-6 max-w-2xl space-y-4">
            <div class="form-control">
                <label class="label"><span class="label-text">PDF File (max 5MB)</span></label>
                <input type="file" name="file" accept="application/pdf" required class="file-input file-input-bordered"/>
            </div>
            <button type="submit" class="btn btn-primary">Process & Download CSV</button>
        </form>

        <div class="mt-4 text-sm opacity-70 max-w-2xl">
            <p>Aplicația suportă format-ul facturilor <strong>AD AUTO TOTAL SRL</strong>.</p>
            <p>După upload, CSV-ul va fi descărcat automat.</p>
        </div>
    </th:block>
</th:block>
</body>
</html>
```

- [ ] **Step 5: Add multipart config în `application.yml`:**

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 5MB
```

---

### Task 7.8: Manual test

- [ ] **Step 1: Restart, browser → `/invoice`**
- [ ] **Step 2: Upload sample PDF din root → CSV downloaded**
- [ ] **Step 3: Deschide CSV în Notepad/Excel — verifică linii corecte, diacritice OK**

---

### Task 7.9: Commit

```powershell
git add src/
git commit -m "feat: PDF invoice upload to CSV export

- Apache PDFBox 3.x for text extraction
- AdAutoTotalInvoiceParser with regex-based line extraction
- CsvExportService with UTF-8 BOM (Excel diacritic compat)
- POST /invoice/upload returns CSV download
- 5MB multipart limit"
```

---

# Phase 8 — Authentication: Spring Security

### Task 8.1: V2 + V3 migrations (app_user + seed)

> **Notă de numerotare Flyway:** Spec-ul (sec. 4) listează tabelele în ordine **logică**, dar migrațiile Flyway trebuie aplicate în ordine **cronologică** (după ordinea în care le creăm). Numerotarea reală în plan:
> - V1 = product (Phase 2)
> - **V2 = app_user (acum)**
> - **V3 = seed_admin_user (acum)**
> - V4 = exchange_rate (Phase 10)
> - V5 = scrape_run (Phase 11)
>
> Această ordine produce schema corectă; spec section 4 va fi actualizat să corespundă.

**Files:**
- Create: `src/main/resources/db/migration/V2__create_app_user_table.sql`
- Create: `src/main/resources/db/migration/V3__seed_admin_user.sql`

```sql
-- V2
CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

```sql
-- V3: username=admin, parola=Autobrand2026! (hash BCrypt cost 10)
-- BCrypt salt-uri sunt randomizate per call; orice hash valid generat din
-- "Autobrand2026!" va verifica corect (passwordEncoder.matches(...) face check-ul).
-- Hash de mai jos a fost pre-generat cu cost=10:
INSERT INTO app_user (username, password_hash, role) VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN'
);
```

> **Pentru a regenera hash-ul** (dacă schimbi parola): scrie un JUnit test temporar `HashTool` care printează `new BCryptPasswordEncoder().encode("noua-parola")` și rulează-l cu `./mvnw test -Dtest=HashTool`. Înlocuiește hash-ul în V3 înainte de prima aplicare a migration-ului.
>
> **Atenție:** hash-ul de mai sus este DOAR exemplu — **regenerează-l cu propriul tău BCrypt** ca să fii sigur de match-ul cu "Autobrand2026!". (Hash-urile BCrypt depind de salt random, deci nu pot fi verificate vizual.)

- [ ] **Step 1: Generate BCrypt hash propriu** — creează un test temporar `src/test/java/ro/autobrand/proba/HashTool.java`:

```java
package ro.autobrand.proba;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HashTool {
    @Test
    void printHash() {
        System.out.println(new BCryptPasswordEncoder().encode("Autobrand2026!"));
    }
}
```

Rulează: `./mvnw test -Dtest=HashTool`. Copiază output-ul (`$2a$10$...`), înlocuiește hash-ul placeholder în V3.

**Șterge** `HashTool.java` după ce ai hash-ul — nu vrei un test care printează parole în CI.

---

### Task 8.2: AppUser entity + repository

**Files:**
- Create: `src/main/java/ro/autobrand/proba/model/AppUser.java`
- Create: `src/main/java/ro/autobrand/proba/repository/AppUserRepository.java`

```java
@Entity
@Table(name = "app_user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(nullable = false, length = 20)
    private String role;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

```java
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}
```

---

### Task 8.3: AppUserDetailsService

**Files:**
- Create: `src/main/java/ro/autobrand/proba/service/AppUserDetailsService.java`

```java
package ro.autobrand.proba.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ro.autobrand.proba.repository.AppUserRepository;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return repository.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
```

---

### Task 8.4: SecurityConfig

**Files:**
- Create: `src/main/java/ro/autobrand/proba/config/SecurityConfig.java`

```java
package ro.autobrand.proba.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/webjars/**", "/error").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### Task 8.5: Remove temporary security exclusion

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Remove** the `spring.autoconfigure.exclude` block adăugat în Task 3.12. Acum SecurityConfig-ul nostru preia rolul.

---

### Task 8.6: Login template

**Files:**
- Create: `src/main/java/ro/autobrand/proba/controller/AuthController.java`
- Create: `src/main/resources/templates/auth/login.html`

```java
@Controller
public class AuthController {
    @GetMapping("/login")
    public String login() { return "auth/login"; }
}
```

```html
<!DOCTYPE html>
<html lang="ro" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Login — Autobrand</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdn.jsdelivr.net/npm/daisyui@4.12.10/dist/full.min.css" rel="stylesheet">
</head>
<body class="min-h-screen flex items-center justify-center bg-base-200" data-theme="corporate">
<div class="card w-96 bg-base-100 shadow-xl">
    <div class="card-body">
        <h1 class="text-3xl font-bold text-center">🚗 Autobrand</h1>
        <p class="text-center opacity-70 text-sm">siguranța mișcării</p>

        <form th:action="@{/login}" method="post" class="space-y-3 mt-4">
            <div th:if="${param.error}" class="alert alert-error text-sm">Invalid credentials</div>
            <div th:if="${param.logout}" class="alert alert-success text-sm">Logged out</div>

            <div class="form-control">
                <label class="label"><span class="label-text">Username</span></label>
                <input type="text" name="username" class="input input-bordered" required autofocus/>
            </div>
            <div class="form-control">
                <label class="label"><span class="label-text">Password</span></label>
                <input type="password" name="password" class="input input-bordered" required/>
            </div>
            <button type="submit" class="btn btn-primary w-full">Sign in</button>
        </form>
    </div>
</div>
</body>
</html>
```

---

### Task 8.7: Test login flow

- [ ] **Step 1: Restart app**
- [ ] **Step 2: Browser → `/products` → redirect la `/login`**
- [ ] **Step 3: Login cu `admin` / `Autobrand2026!` → redirect la `/`**
- [ ] **Step 4: Logout → redirect la `/login?logout`**

---

### Task 8.8: Commit

```powershell
git add src/
git commit -m "feat: Spring Security with form login + BCrypt + DB users

- AppUser entity + repository + UserDetailsService
- SecurityFilterChain protects all routes except /login
- Flyway V3 creates app_user, V4 seeds admin/Autobrand2026!
- Custom login page (Tailwind+DaisyUI styled)"
```

---

# Phase 9 — Filter & Sort with Specifications + HTMX

### Task 9.1: ProductSpecifications

**Files:**
- Create: `src/main/java/ro/autobrand/proba/specification/ProductSpecifications.java`

```java
package ro.autobrand.proba.specification;

import org.springframework.data.jpa.domain.Specification;
import ro.autobrand.proba.model.Product;

import java.math.BigDecimal;

public class ProductSpecifications {

    public static Specification<Product> nameLike(String search) {
        return (root, q, cb) -> search == null || search.isBlank() ? null :
                cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%");
    }

    public static Specification<Product> currencyEquals(String currency) {
        return (root, q, cb) -> currency == null || currency.isBlank() ? null :
                cb.equal(root.get("currency"), currency);
    }

    public static Specification<Product> priceMin(BigDecimal min) {
        return (root, q, cb) -> min == null ? null :
                cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceMax(BigDecimal max) {
        return (root, q, cb) -> max == null ? null :
                cb.lessThanOrEqualTo(root.get("price"), max);
    }
}
```

---

### Task 9.2: ProductService.search()

**Files:**
- Modify: `src/main/java/ro/autobrand/proba/service/ProductService.java`

Add:
```java
public Page<Product> search(String name, String currency, BigDecimal min, BigDecimal max, Pageable pageable) {
    Specification<Product> spec = Specification.where(nameLike(name))
            .and(currencyEquals(currency))
            .and(priceMin(min))
            .and(priceMax(max));
    return repository.findAll(spec, pageable);
}
```

(Adaugă imports.)

---

### Task 9.3: Update ProductController

**Files:**
- Modify: `src/main/java/ro/autobrand/proba/controller/ProductController.java`

Replace `list` method:

```java
@GetMapping
public String list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String currency,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @PageableDefault(size = 20, sort = "name") Pageable pageable,
        Model model,
        @RequestHeader(value = "HX-Request", required = false) String htmxHeader
) {
    Page<Product> page = productService.search(search, currency, minPrice, maxPrice, pageable);
    model.addAttribute("page", page);
    model.addAttribute("search", search);
    model.addAttribute("currency", currency);
    model.addAttribute("minPrice", minPrice);
    model.addAttribute("maxPrice", maxPrice);

    return htmxHeader != null ? "products/list :: products-tbody" : "products/list";
}
```

(Inject `ProductService` în loc de raw `ProductRepository`.)

---

### Task 9.4: Update list.html cu filter UI + HTMX

**Files:**
- Modify: `src/main/resources/templates/products/list.html`

Add filter bar above table:
```html
<div class="mb-4 flex gap-2 flex-wrap items-end" hx-target="#products-tbody" hx-trigger="change from:input">
    <input type="text" name="search" th:value="${search}" placeholder="Search name..." class="input input-bordered input-sm"/>
    <input type="text" name="currency" th:value="${currency}" placeholder="Currency" maxlength="3" class="input input-bordered input-sm w-24"/>
    <input type="number" name="minPrice" th:value="${minPrice}" placeholder="Min" step="0.01" class="input input-bordered input-sm w-24"/>
    <input type="number" name="maxPrice" th:value="${maxPrice}" placeholder="Max" step="0.01" class="input input-bordered input-sm w-24"/>
    <button hx-get="/products" class="btn btn-primary btn-sm">Apply</button>
</div>
```

Wrap `<tbody>` cu Thymeleaf fragment:
```html
<tbody th:fragment="products-tbody" id="products-tbody">
   ...
</tbody>
```

---

### Task 9.5: Commit

```powershell
git add src/
git commit -m "feat: filter/sort products with Specifications + HTMX swap

- ProductSpecifications: nameLike, currencyEquals, priceMin/Max
- ProductService.search() combines with Pageable
- HTMX-driven filter bar swaps tbody fragment"
```

---

# Phase 10 — Exchange Rate (BNR XML)

### Task 10.1: V4 migration + entity (exchange_rate)

**Files:**
- Create: `src/main/resources/db/migration/V4__create_exchange_rate_table.sql`

```sql
CREATE TABLE exchange_rate (
    id BIGSERIAL PRIMARY KEY,
    rate_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    rate_to_ron NUMERIC(12, 6) NOT NULL,
    multiplier INTEGER NOT NULL DEFAULT 1,
    fetched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (rate_date, currency)
);
```

> **Status migrații până aici** (verifică cu comanda de mai jos): V1=product, V2=app_user, V3=seed_admin. Adăugăm acum V4 = exchange_rate.

- [ ] **Step 1: Check applied migrations**

```powershell
docker exec -it autobrand-db psql -U autobrand -d autobrand -c "SELECT version, description FROM flyway_schema_history;"
```

Așteptat: 3 rânduri (V1, V2, V3). După restart-ul aplicației, va apărea și V4.

Entity:
```java
@Entity
@Table(name = "exchange_rate", uniqueConstraints = @UniqueConstraint(columnNames = {"rate_date", "currency"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExchangeRate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "rate_to_ron", nullable = false, precision = 12, scale = 6)
    private BigDecimal rateToRon;
    @Column(nullable = false)
    private int multiplier;
    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}
```

Repository:
```java
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findByRateDateAndCurrency(LocalDate date, String currency);
    boolean existsByRateDate(LocalDate date);
}
```

---

### Task 10.2: BnrRateDto + ExchangeRateService

**Files:**
- Create: `src/main/java/ro/autobrand/proba/service/ExchangeRateService.java`

> Implementare scurtă, fetch + parse Jackson XML + recompute. Vezi spec section 5.3 pentru detalii formulă.

```java
package ro.autobrand.proba.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ro.autobrand.proba.model.ExchangeRate;
import ro.autobrand.proba.repository.ExchangeRateRepository;
import ro.autobrand.proba.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private static final Set<String> SUPPORTED = Set.of("USD", "EUR", "GBP", "CHF", "JPY");

    private final ExchangeRateRepository rateRepo;
    private final ProductRepository productRepo;
    private final RestClient restClient = RestClient.create();

    public void ensureTodayRates() {
        LocalDate today = LocalDate.now();
        if (rateRepo.existsByRateDate(today)) {
            log.debug("Rates for {} already fetched", today);
            return;
        }
        try {
            String xml = restClient.get()
                    .uri("https://www.bnr.ro/nbrfxrates.xml")
                    .retrieve()
                    .body(String.class);
            // Parse XML — vezi schema BNR (Body > Cube > Rate[currency, multiplier])
            // Pentru simplitate: regex pe Rate elements (sau JAXB-style)
            parseAndStore(xml, today);
        } catch (Exception e) {
            log.error("BNR fetch failed", e);
        }
    }

    // Parse manual cu regex (mai simplu decât setup full Jackson XML pentru o tabela mică)
    private void parseAndStore(String xml, LocalDate date) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "<Rate currency=\"([A-Z]{3})\"(?: multiplier=\"(\\d+)\")?[^>]*>([\\d.]+)</Rate>");
        var m = p.matcher(xml);
        while (m.find()) {
            String currency = m.group(1);
            int multiplier = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
            BigDecimal rate = new BigDecimal(m.group(3));
            if (SUPPORTED.contains(currency)) {
                rateRepo.save(ExchangeRate.builder()
                        .rateDate(date)
                        .currency(currency)
                        .rateToRon(rate)
                        .multiplier(multiplier)
                        .fetchedAt(LocalDateTime.now())
                        .build());
            }
        }
    }

    public void recomputeRon() {
        LocalDate today = LocalDate.now();
        productRepo.findAll().forEach(p -> {
            if ("RON".equals(p.getCurrency())) {
                p.setPriceRon(p.getPrice());
            } else {
                rateRepo.findByRateDateAndCurrency(today, p.getCurrency())
                        .ifPresent(r -> {
                            BigDecimal ron = p.getPrice()
                                    .multiply(r.getRateToRon())
                                    .divide(BigDecimal.valueOf(r.getMultiplier()), 2, RoundingMode.HALF_UP);
                            p.setPriceRon(ron);
                        });
            }
            productRepo.save(p);
        });
    }
}
```

---

### Task 10.3: Wire into ScrapingService

**Files:**
- Modify: `src/main/java/ro/autobrand/proba/service/ScrapingService.java`

Adaugă `ExchangeRateService` injection și apel după upsert:
```java
public ProductService.UpsertResult runScrape() {
    // ... existing
    exchangeRateService.ensureTodayRates();
    exchangeRateService.recomputeRon();
    return result;
}
```

---

### Task 10.4: Test multiplier formula

**Files:**
- Create: `src/test/java/ro/autobrand/proba/service/ExchangeRateServiceTest.java`

Test that 100 JPY with multiplier=100 yields correct RON.

---

### Task 10.5: Commit

```powershell
git add src/
git commit -m "feat: BNR exchange rates with multiplier-aware RON conversion

- ExchangeRate entity + Flyway V_n migration
- ExchangeRateService.ensureTodayRates fetches XML, parses, persists
- recomputeRon applies multiplier formula (price * rate / multiplier)
- Triggered from ScrapingService after upsert"
```

---

# Phase 11 — Dashboard + ScrapeRun + Chart

### Task 11.1: ScrapeRun migration V5 + entity + repository

**Files:**
- Create: `src/main/resources/db/migration/V5__create_scrape_run_table.sql`
- Create: `src/main/java/ro/autobrand/proba/model/ScrapeRun.java`
- Create: `src/main/java/ro/autobrand/proba/repository/ScrapeRunRepository.java`

- [ ] **Step 1: Migration:**

```sql
CREATE TABLE scrape_run (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    products_total INTEGER,
    products_new INTEGER,
    products_updated INTEGER,
    error_message TEXT
);
CREATE INDEX idx_scrape_run_started_at ON scrape_run(started_at DESC);
```

- [ ] **Step 2: Entity:**

```java
package ro.autobrand.proba.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scrape_run")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScrapeRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
    @Column(nullable = false, length = 20)
    private String status;          // RUNNING, SUCCESS, FAILED
    @Column(name = "products_total")
    private Integer productsTotal;
    @Column(name = "products_new")
    private Integer productsNew;
    @Column(name = "products_updated")
    private Integer productsUpdated;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
```

- [ ] **Step 3: Repository:**

```java
package ro.autobrand.proba.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.autobrand.proba.model.ScrapeRun;
import java.util.List;
import java.util.Optional;

public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {
    // Spring Data derivation: SELECT * FROM scrape_run ORDER BY started_at DESC LIMIT 5
    List<ScrapeRun> findTop5ByOrderByStartedAtDesc();

    Optional<ScrapeRun> findFirstByStatusOrderByStartedAtDesc(String status);
}
```

---

### Task 11.2: Track scrape runs în ScrapingService

**Files:**
- Modify: `src/main/java/ro/autobrand/proba/service/ScrapingService.java`

- [ ] **Step 1: Inject repository + wrap runScrape() cu lifecycle tracking:**

```java
// adaugă field:
private final ScrapeRunRepository scrapeRunRepo;

public ProductService.UpsertResult runScrape() {
    log.info("Starting scrape run");
    ScrapeRun run = scrapeRunRepo.save(ScrapeRun.builder()
            .startedAt(LocalDateTime.now())
            .status("RUNNING")
            .build());
    try {
        List<ScrapedProductDto> scraped = scraper.scrape();
        ProductService.UpsertResult result = productService.upsertAll(scraped);

        run.setStatus("SUCCESS");
        run.setFinishedAt(LocalDateTime.now());
        run.setProductsTotal(result.total());
        run.setProductsNew(result.inserted());
        run.setProductsUpdated(result.updated());
        scrapeRunRepo.save(run);

        exchangeRateService.ensureTodayRates();
        exchangeRateService.recomputeRon();

        log.info("Scrape run completed: {}", result);
        return result;
    } catch (Exception e) {
        run.setStatus("FAILED");
        run.setFinishedAt(LocalDateTime.now());
        run.setErrorMessage(e.getMessage());
        scrapeRunRepo.save(run);
        log.error("Scrape run failed", e);
        throw e;
    }
}
```

---

### Task 11.3: DashboardController + template

**Files:**
- Create: `src/main/java/ro/autobrand/proba/controller/DashboardController.java`
- Create: `src/main/resources/templates/dashboard.html`
- Modify: `src/main/java/ro/autobrand/proba/repository/ProductRepository.java` (adaugă count methods)

- [ ] **Step 1: Add count methods în ProductRepository:**

```java
long countByManuallyEditedTrue();

// Spring Data deja are count() general — folosim acela pentru total
```

Pentru distribuția per monedă, scriem un query JPQL:
```java
@Query("SELECT p.currency AS currency, COUNT(p) AS count FROM Product p GROUP BY p.currency")
List<CurrencyCount> countByCurrency();

interface CurrencyCount {
    String getCurrency();
    Long getCount();
}
```

- [ ] **Step 2: DashboardController:**

```java
package ro.autobrand.proba.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ro.autobrand.proba.model.ScrapeRun;
import ro.autobrand.proba.repository.ExchangeRateRepository;
import ro.autobrand.proba.repository.ProductRepository;
import ro.autobrand.proba.repository.ScrapeRunRepository;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ProductRepository productRepo;
    private final ScrapeRunRepository scrapeRunRepo;
    private final ExchangeRateRepository rateRepo;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("totalProducts", productRepo.count());
        model.addAttribute("manuallyEditedCount", productRepo.countByManuallyEditedTrue());

        List<ScrapeRun> recent = scrapeRunRepo.findTop5ByOrderByStartedAtDesc();
        model.addAttribute("recentRuns", recent);
        model.addAttribute("lastSuccessAt", scrapeRunRepo
                .findFirstByStatusOrderByStartedAtDesc("SUCCESS")
                .map(ScrapeRun::getFinishedAt).orElse(null));

        Map<String, Long> currencyStats = new LinkedHashMap<>();
        productRepo.countByCurrency().forEach(c -> currencyStats.put(c.getCurrency(), c.getCount()));
        model.addAttribute("currencyStats", currencyStats);

        rateRepo.findByRateDateAndCurrency(LocalDate.now(), "USD")
                .ifPresent(r -> model.addAttribute("usdRate", r.getRateToRon()));

        return "dashboard";
    }
}
```

- [ ] **Step 3: Dashboard template:**

```html
<!DOCTYPE html>
<html lang="ro" xmlns:th="http://www.thymeleaf.org">
<head><title>Dashboard</title></head>
<body>
<th:block th:replace="~{fragments/layout :: layout('Dashboard', ~{::main-content})}">
    <th:block th:fragment="main-content">
        <h1 class="text-2xl font-bold mb-4">Dashboard</h1>

        <!-- Stats cards -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <div class="card bg-base-100 shadow"><div class="card-body">
                <div class="text-3xl font-bold" th:text="${totalProducts}">0</div>
                <div class="opacity-70">Total Products</div>
            </div></div>
            <div class="card bg-base-100 shadow"><div class="card-body">
                <div class="text-3xl font-bold" th:text="${manuallyEditedCount}">0</div>
                <div class="opacity-70">Manually Edited</div>
            </div></div>
            <div class="card bg-base-100 shadow"><div class="card-body">
                <div class="text-lg font-bold"
                     th:text="${lastSuccessAt != null ? #temporals.format(lastSuccessAt, 'dd MMM HH:mm') : 'Never'}">Never</div>
                <div class="opacity-70">Last Scrape</div>
            </div></div>
            <div class="card bg-base-100 shadow"><div class="card-body">
                <div class="text-3xl font-bold" th:text="${usdRate != null ? usdRate + ' RON' : '-'}">-</div>
                <div class="opacity-70">USD → RON</div>
            </div></div>
        </div>

        <!-- Chart -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <div class="card bg-base-100 shadow"><div class="card-body">
                <h2 class="card-title">Currency Distribution</h2>
                <canvas id="currencyChart" class="max-h-64"></canvas>
            </div></div>

            <!-- Recent activity -->
            <div class="card bg-base-100 shadow"><div class="card-body">
                <h2 class="card-title">Recent Scrape Runs</h2>
                <table class="table table-sm">
                    <thead><tr><th>Started</th><th>Status</th><th>Products</th></tr></thead>
                    <tbody>
                    <tr th:each="r : ${recentRuns}">
                        <td th:text="${#temporals.format(r.startedAt, 'dd MMM HH:mm')}"></td>
                        <td>
                            <span th:if="${r.status == 'SUCCESS'}" class="badge badge-success">✓ SUCCESS</span>
                            <span th:if="${r.status == 'FAILED'}" class="badge badge-error">✗ FAILED</span>
                            <span th:if="${r.status == 'RUNNING'}" class="badge badge-info">⏳ RUNNING</span>
                        </td>
                        <td th:text="${r.productsTotal != null ? r.productsTotal + ' (' + r.productsNew + ' new)' : '-'}"></td>
                    </tr>
                    </tbody>
                </table>
            </div></div>
        </div>

        <script th:inline="javascript">
            const data = /*[[${currencyStats}]]*/ {};
            new Chart(document.getElementById('currencyChart'), {
                type: 'doughnut',
                data: {
                    labels: Object.keys(data),
                    datasets: [{ data: Object.values(data) }]
                },
                options: { responsive: true, maintainAspectRatio: false }
            });
        </script>
    </th:block>
</th:block>
</body>
</html>
```

- [ ] **Step 4: Manual test**

Restart, browser → `/` (after login) → vezi dashboard cu stats, donut chart cu produsele per monedă, lista cu recent scrape runs.

Expected: dacă ai rulat scrape de câteva ori, donut-ul arată distribuția (predominant USD pentru web-scraping.dev), recent activity arată ultimele 5 SUCCESS-uri.

---

### Task 11.4: Commit

```powershell
git add src/
git commit -m "feat: dashboard with stats, Chart.js donut, recent scrape activity

- ScrapeRun entity tracks lifecycle (RUNNING → SUCCESS/FAILED)
- ScrapingService wraps run with try/catch + status updates
- DashboardController aggregates: counts, last success time, currency distribution, current USD rate
- Donut chart shows products per currency
- Recent activity shows last 5 runs with status badges"
```

---

# Phase 12 — Polish: Errors, README, Dockerfile, Screenshots

### Task 12.1: GlobalExceptionHandler + error pages

**Files:**
- Create: `src/main/java/ro/autobrand/proba/exception/GlobalExceptionHandler.java`
- Create: `src/main/resources/templates/errors/404.html`
- Create: `src/main/resources/templates/errors/500.html`

- [ ] **Step 1: GlobalExceptionHandler:**

```java
package ro.autobrand.proba.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ProductNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "errors/404";
    }

    @ExceptionHandler(InvalidPdfException.class)
    public String handleInvalidPdf(InvalidPdfException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/invoice";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleUploadTooLarge(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "Fișierul depășește limita de 5MB.");
        return "redirect:/invoice";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(Exception ex, Model model) {
        log.error("Unhandled exception", ex);
        model.addAttribute("message", "A apărut o eroare neașteptată: " + ex.getClass().getSimpleName());
        return "errors/500";
    }
}
```

- [ ] **Step 2: errors/404.html:**

```html
<!DOCTYPE html>
<html lang="ro" xmlns:th="http://www.thymeleaf.org">
<head><title>Not Found</title></head>
<body>
<th:block th:replace="~{fragments/layout :: layout('404', ~{::main-content})}">
    <th:block th:fragment="main-content">
        <div class="hero min-h-[50vh]">
            <div class="hero-content text-center">
                <div>
                    <h1 class="text-6xl font-bold">404</h1>
                    <p class="py-4" th:text="${message}">Resursa nu a fost găsită</p>
                    <a th:href="@{/}" class="btn btn-primary">Înapoi acasă</a>
                </div>
            </div>
        </div>
    </th:block>
</th:block>
</body>
</html>
```

- [ ] **Step 3: errors/500.html (identic dar cu 500):**

(Copy 404.html, înlocuiește `404` cu `500` și textul.)

### Task 12.2: Dockerfile

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Task 12.3: Update docker-compose.yml cu app service

Add `app` service per spec section 8.

Test: `docker compose down -v && docker compose up --build` — totul live la `:8080`.

### Task 12.4: Write README.md

Per spec section 11 — with credentials block visible.

### Task 12.5: Screenshots + GIFs

Use ScreenToGif or LICEcap. Save în `docs/screenshots/` and `docs/gifs/`. Reference in README.

### Task 12.6: Final commit

```powershell
git add .
git commit -m "feat: production polish — Dockerfile, README, error pages, screenshots"
```

---

# Phase 13 — Submission

### Task 13.1: Push to GitHub

```powershell
gh repo create proba-practica-autobrand --public --source=. --remote=origin --push
```

(Sau create manual și `git push -u origin main`.)

### Task 13.2: Fresh-clone smoke test

În alt folder:
```powershell
git clone <url>
cd proba-practica-autobrand
docker compose up --build
```

Verify http://localhost:8080 functional din 1 comandă.

### Task 13.3: Email recruiter

To: `hr@autobrand.ro`  
Subject: `Rezolvare proba practica Boris Pavel`  
Body:
```
Bună ziua,

În atașament/link găsiți soluția probei practice pentru postul Junior Full Stack Developer:
<GitHub URL>

Credențiale demo: admin / Autobrand2026!
Pornire: docker compose up --build

Documentul de design (decisions + trade-offs) este în repo:
docs/superpowers/specs/2026-05-19-autobrand-proba-practica-design.md

Multumesc,
Boris Pavel
[telefon din CV]
```

---

## Done — Final Checklist

- [ ] Toate cerințele principale ✅
  - [ ] Scraping login + extract
  - [ ] Cron orar 12-18
  - [ ] DB cu UNIQUE constraint
  - [ ] UI CRUD
  - [ ] PDF upload → CSV
- [ ] Toate bonusurile ✅
  - [ ] Curs valutar BNR + RON
  - [ ] Filtrare/sortare HTMX
  - [ ] Spring Security
- [ ] Quality
  - [ ] Tests: service unit + repo Testcontainers + controller MockMvc
  - [ ] README cu credentials + screenshots + gifs
  - [ ] `docker compose up --build` rulează clean
  - [ ] No TODOs în cod
- [ ] Submission
  - [ ] Push GitHub
  - [ ] Fresh-clone test
  - [ ] Email trimis
