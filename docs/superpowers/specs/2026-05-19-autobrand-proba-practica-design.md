# Proba Practică Autobrand — Design Document

> **Autor:** Boris Pavel  
> **Data:** 2026-05-19  
> **Status:** Approved (pending implementation)  
> **Poziție vizată:** Junior Full Stack Developer @ Autobrand

---

## 1. Goal & Context

Soluționarea probei practice pentru Autobrand, care evaluează atât capacitatea de implementare cât și **modul de gândire, structura soluției și abordarea aleasă** (per textul probei).

Proba are 3 componente:
1. **Web scraping + SQL** — login la web-scraping.dev, scrape de produse, cron orar 12–18, persistență în DB cu constraint UNIQUE pe denumire, UI CRUD.
2. **PDF processing** — upload factură PDF (sample `AD AUTO TOTAL SRL_20241747776_2024_03_01.PDF`) → extragere date → export CSV.
3. **Bonus** — curs valutar BNR + preț RON, filtrare/sortare, autentificare.

Deadline: finalul săptămânii (2026-05-24 informativ). Submission: email la `hr@autobrand.ro` cu titlul `Rezolvare proba practica Boris Pavel`, conținând link GitHub.

---

## 2. Stack & Librării — Decizii

### Core
| Componentă | Alegere | Justificare |
|---|---|---|
| Limbă | **Java 21 LTS** (Eclipse Temurin) | LTS curent. JD-ul cere Java. |
| Framework | **Spring Boot 3.3** | Standard industry pentru Java web; auto-config; ecosistem matur. |
| Build | **Maven** | Mai accesibil pentru beginner decât Gradle Kotlin DSL; `pom.xml` standard. |
| Persistență | **Spring Data JPA + Hibernate** | Repository pattern out-of-the-box; reduce boilerplate CRUD. |
| DB runtime | **PostgreSQL 16** (Docker) | DB serios de producție; cunoscut în industrie. |
| DB test | **Testcontainers Postgres** | Teste pe același engine ca producția (vs. H2 care divergează). |
| Migrări | **Flyway** | Schema versionată în git; reproductibilă. |
| Templating | **Thymeleaf** | Integrare nativă Spring; server-side; no build pipeline. |
| Frontend CSS | **Tailwind CSS (Play CDN)** + **DaisyUI** | Look modern fără build pipeline; componente DaisyUI gata. |
| Interactivitate | **HTMX** | SPA-feel fără JS framework; trendy în comunitatea Spring (Thoughtworks Radar 2024). |
| Charting | **Chart.js (CDN)** | Un singur donut chart pe dashboard. |

### Specialized
| Componentă | Alegere | Justificare |
|---|---|---|
| Web scraping | **Jsoup 1.17** (primary) | Cel mai simplu; suportă form login + cookies; rapid; zero binaries. Fallback Selenium dacă pagina e JS-rendered. |
| PDF parsing | **Apache PDFBox 3.x** | De-facto Java standard; Apache 2.0 license (iText e AGPL/comercial). |
| CSV export | **Apache Commons CSV** | Mic, focused, ușor de explicat. |
| HTTP client (BNR) | **Spring `RestClient`** (Java 21) | Modern (înlocuiește RestTemplate); fluent API. |
| XML parsing (BNR) | **Jackson Dataformat XML** | Riguros, integrat în ecosistem Jackson. |
| Cron scheduler | **Spring `@Scheduled`** | Built-in; zero dependencies. |
| Auth | **Spring Security + BCrypt** | Industry standard; form login + session. |
| Validation | **Bean Validation (Hibernate Validator)** | Annotations declarative pe DTOs. |
| Reducere boilerplate | **Lombok** | `@Data`, `@Builder`, `@Slf4j` pe entități/DTOs. |
| Testing | **JUnit 5 + Mockito + MockMvc + Testcontainers** | Standard Spring testing stack. |
| Logging | **SLF4J + Logback** | Default Spring Boot. |

### Decizii NU făcute (YAGNI)
- ❌ Microservicii / multi-module Maven — overkill pentru scope.
- ❌ React / SPA separată — Thymeleaf + HTMX e suficient și mai impresionant pentru beginner Java.
- ❌ JWT — session-based e mai simplu cu form login.
- ❌ Quartz scheduler — `@Scheduled` ajunge.
- ❌ Audit tables (`product_history`) — overkill.
- ❌ MapStruct — manual mapping e ok la acest volum.

---

## 3. Arhitectură — Modular Monolith

**Abordare:** monolit cu packages clar separate pe responsabilități. **Nu** multi-module Maven (overkill pentru o săptămână).

### Structură proiect

```
proba-practica-autobrand/
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── README.md                    (cu credențiale demo vizibile)
├── docs/superpowers/specs/      (acest document)
├── src/main/java/ro/autobrand/proba/
│   ├── ProbaApplication.java
│   ├── config/                  (SecurityConfig, SchedulerConfig, WebConfig)
│   ├── controller/              (Dashboard, Product, Invoice, Auth, ScrapeAdmin)
│   ├── service/                 (Product, Scraping, PdfInvoice, ExchangeRate, CsvExport)
│   ├── repository/              (Product, ExchangeRate, AppUser, ScrapeRun)
│   ├── model/                   (Entities JPA: Product, ExchangeRate, AppUser, ScrapeRun)
│   ├── dto/                     (ProductDto, InvoiceLineDto, ScrapedProductDto, BnrRateDto)
│   ├── scraper/                 (Scraper interface + WebScrapingDevScraper)
│   ├── pdf/                     (InvoiceParser interface + AdAutoTotalInvoiceParser)
│   └── exception/               (GlobalExceptionHandler, custom exceptions)
├── src/main/resources/
│   ├── application.yml          (config comun)
│   ├── application-dev.yml      (local: Postgres în Docker, cron pe manual trigger)
│   ├── application-docker.yml   (container runtime)
│   ├── application-test.yml     (Testcontainers, cron off)
│   ├── db/migration/            (Flyway: V1__create_product.sql, etc.)
│   ├── templates/               (Thymeleaf: layout, products, invoice, auth, errors)
│   └── static/css/              (eventual custom CSS minimal peste Tailwind)
└── src/test/java/ro/autobrand/proba/
    ├── service/                 (Unit tests cu Mockito)
    ├── repository/              (@DataJpaTest + Testcontainers)
    ├── controller/              (@WebMvcTest + MockMvc)
    ├── scraper/                 (Parser cu HTML fixture)
    ├── pdf/                     (Parser cu PDF sample)
    └── integration/             (@SpringBootTest smoke test)
```

### Pattern-uri cheie
- **Layered architecture**: controller → service → repository → DB. Mapping DTO ↔ entity la limita controller-service.
- **Interface pentru extensibilitate**: `Scraper` și `InvoiceParser` interfaces — demonstrare DIP (Dependency Inversion). Implementări concrete plug-able fără modificarea consumer-ilor.
- **Specification pattern** (Spring Data JPA) pentru filtering dinamic.

---

## 4. Database Schema

### `product` (cerință principală)
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

**Decizii:**
- `UNIQUE(name)` — cerință explicită; constraint la nivel DB pentru a evita race condition.
- `NUMERIC(12,2)` pentru preț — `BigDecimal` în Java (nu `double` — virgulă mobilă imprecisă).
- `manually_edited` — flag pentru a proteja editări manuale de scraping.
- `price_ron` nullable — populat doar pentru produse cu currency suportat de BNR.

**Upsert logic la scraping:**
- Produs nou → `INSERT`
- Produs existent, `manually_edited = false` → `UPDATE` toate câmpurile + `last_scraped`
- Produs existent, `manually_edited = true` → `UPDATE` doar `last_scraped`

### `exchange_rate` (bonus #1)
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

`multiplier` — BNR publică unele rate per 100 unități (JPY de ex.); formula:  
`priceRon = price * rate / multiplier`.

### `app_user` (bonus #3)
```sql
CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

Seed: `admin` / `Autobrand2026!` (BCrypt hash în migration).

### `scrape_run` (pentru dashboard Recent Activity)
```sql
CREATE TABLE scrape_run (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,        -- SUCCESS, FAILED, RUNNING
    products_total INTEGER,
    products_new INTEGER,
    products_updated INTEGER,
    error_message TEXT
);
CREATE INDEX idx_scrape_run_started_at ON scrape_run(started_at DESC);
```

### Migration files
```
V1__create_product_table.sql
V2__create_exchange_rate_table.sql
V3__create_app_user_table.sql
V4__seed_admin_user.sql
V5__create_scrape_run_table.sql
```

---

## 5. Data Flows

### 5.1 Scraping Flow (cron + startup)

**Trigger:**
- `@Scheduled(cron = "0 0 12-18 * * *")` — la fiecare oră fix, între 12 și 18.
- `@EventListener(ApplicationReadyEvent.class)` — rulează scraperul și la pornirea aplicației (util pentru demo).

**Pași:**
1. `ScrapeRun` row creat cu status `RUNNING`.
2. `WebScrapingDevScraper.login()` — Jsoup POST credențiale, capturează session cookies.
3. `WebScrapingDevScraper.fetchProducts(cookies)` — GET pagina produse, parsare cu Jsoup → `List<ScrapedProduct>`.
4. `ProductService.upsertAll(scraped)` — `@Transactional`. Pentru fiecare produs:
   - `findByName(scraped.name)`:
     - **Nu există** → INSERT cu `first_seen = now()`, `last_scraped = now()`.
     - **Există, `manually_edited = false`** → UPDATE `price`, `description`, `image_url`, `source_url`, `currency`, `last_scraped = now()`, `updated_at = now()`.
     - **Există, `manually_edited = true`** → UPDATE **doar** `last_scraped = now()` (păstrează modificările manuale, dar marchează că produsul mai există la sursă).
5. (Bonus) `ExchangeRateService.ensureTodayRates()` → fetch BNR dacă nu există rate pentru ziua curentă → recompute `price_ron`.
6. `ScrapeRun` updated cu status `SUCCESS` + counts.
7. La orice exception: status `FAILED` + `error_message`, log stack trace.

**Credențiale:** în `application.yml` via env vars (`SCRAPER_USERNAME`, `SCRAPER_PASSWORD`). Site-ul web-scraping.dev are credențiale publice documentate.

### 5.2 PDF Upload → CSV Flow

**Endpoint:** `POST /invoice/upload` (multipart/form-data).

**Pași:**
1. `InvoiceController` primește `MultipartFile`.
2. Validare: `Content-Type == application/pdf`, magic bytes `%PDF`, size < 5MB. Eșec → flash error message.
3. `PdfInvoiceService.extract(file.getInputStream())`:
   - `PDDocument.load(stream)`
   - `PDFTextStripper.getText(doc)` → text raw
   - `AdAutoTotalInvoiceParser.parse(text)` — regex/heuristici specifice formatului facturii → `List<InvoiceLine>`.
4. `CsvExportService.toCsv(lines)`:
   - Header: `cod_produs,denumire,pret_unitar,moneda,cantitate`
   - UTF-8 BOM (pentru Excel cu diacritice)
   - Output: `byte[]`.
5. Răspuns:
   ```
   Content-Type: text/csv; charset=UTF-8
   Content-Disposition: attachment; filename="factura-{timestamp}.csv"
   ```

**Notă:** **Nu** persistăm invoice-uri în DB. Procesare in-memory, request-response, simplu.

**Handling pentru PDF cu layout necunoscut:**
- Parser-ul este specific formatului `AD AUTO TOTAL` (factură furnizată ca sample).
- Dacă parser-ul rulează dar **nu extrage nicio linie** (regex-urile nu match-uiesc nimic) → return un CSV gol cu doar header-ul + flash warning user-ului: *"PDF procesat, dar nu am putut extrage linii de factură. Verifică dacă fișierul are formatul așteptat (factură AD AUTO TOTAL)."*
- Dacă PDFBox aruncă exception (PDF corupt / cifrat / parolat) → flash error + redirect.
- La interviu: *"Am ales să nu eșuez la PDF cu layout necunoscut — dau user-ului feedback clar și CSV gol. Defensive, dar observabil."*

### 5.3 Exchange Rate Flow (bonus #1)

**Trigger:**
- Din scraping flow (după upsert produse).
- La pornirea aplicației (ApplicationReadyEvent).

**Pași:**
1. `ExchangeRateService.ensureTodayRates()`:
   - Check DB: există rate pentru `today`? Da → skip.
   - Nu → GET `https://www.bnr.ro/nbrfxrates.xml` cu `RestClient`.
   - Parse XML cu Jackson XML.
   - INSERT rate-uri pentru un set fixat de monede: **USD, EUR, GBP, CHF, JPY** (suficient pentru web-scraping.dev care folosește USD; restul, defensive). Alte monede din XML sunt ignorate.
2. `ProductService.recomputeRon()`:
   - Pentru fiecare produs cu `currency` ≠ `RON` și rate disponibil:
     `price_ron = price * rate / multiplier`, rotunjit la 2 zecimale, `HALF_UP`.

**Particularitate BNR:** XML `<Rate currency="JPY" multiplier="100">` — atenție la multiplier (validat în test).

---

## 6. UI / Frontend

### Stack
- **Tailwind Play CDN** + **DaisyUI CDN** — un singur script tag, zero build.
- **HTMX** — interactivitate (filter, sort, delete, edit) fără refresh.
- **Chart.js CDN** — un singur donut chart pe dashboard.
- **Theme toggle**: DaisyUI `corporate` ↔ `dim` (light/dark).

### Layout
- Sidebar stânga: Dashboard, Products, Upload Invoice, Scrape Log.
- Top bar: titlu pagină + user info + logout + theme toggle.
- Main: conținut.

### Pagini & rute

| Rută | Method | Descriere |
|---|---|---|
| `/login`, `/logout` | GET, POST | Autentificare |
| `/` | GET | Dashboard |
| `/products` | GET | Listă + filter/sort |
| `/products/{id}/edit` | GET | Form editare |
| `/products/{id}` | POST | Update |
| `/products/{id}/delete` | POST (HTMX) | Delete |
| `/products/{id}/reset` | POST | Set `manually_edited = false` (la următorul cron run, produsul va fi rescris cu datele scraped) |
| `/invoice` | GET | Formular upload |
| `/invoice/upload` | POST | Procesare + download CSV |
| `/admin/scrape` | POST | Trigger manual scrape (pentru demo) |

### Dashboard (`/`)
Carduri statistici:
- Total produse
- Produse manually edited
- Last scrape time
- Next scheduled run
- Curs USD→RON curent

Donut chart Chart.js: distribuție produse per monedă.

Tabel "Recent Activity": ultimele 5 `scrape_run` cu status, count, durată.

### Filtering & Sorting (bonus #2)

**Server-side cu Spring Data Specifications + Pageable:**
- Search by name (LIKE, case-insensitive)
- Filter by currency
- Filter by price range (min, max)
- Sort by name / price / last_scraped (asc/desc)
- Pagination (default 20/pagină)

URL exemple:
- `/products?search=apple&currency=USD&sort=price,desc&page=0&size=20`

HTMX swap: schimbarea oricărui filter face GET `/products` cu params noi, swap `<tbody>` cu rezultatul.

### Form editare
Toate câmpurile editabile (`name`, `description`, `price`, `currency`, `image_url`, `source_url`). Validare Bean Validation. La save → `manually_edited = true` automatic.

Buton "Reset to scraped" → setează `manually_edited = false`. **Nu** declanșează re-scrape direct (overhead inutil); la următorul cron run (sau la trigger manual din admin), produsul va fi rescris cu datele scraped curente.

---

## 7. Authentication

### Spring Security config

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/webjars/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error"))
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### `AppUserDetailsService`
Custom service care încarcă `AppUser` din DB by username și-l mapează la Spring `UserDetails`.

### Credențiale default
| Username | Parolă | Rol |
|---|---|---|
| `admin` | `Autobrand2026!` | ADMIN |

Hash BCrypt seedat în `V4__seed_admin_user.sql`. **Credențialele sunt afișate vizibil în README.md** pentru recruiter.

### CSRF
Activ default Spring Security. Thymeleaf `th:action` adaugă automat token-ul.

### YAGNI (auth)
- ❌ Remember-me, password reset, registration, OAuth, JWT, multi-user.

---

## 8. Setup & Deployment

### Docker (toate componentele)

`docker-compose.yml`:
```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: autobrand
      POSTGRES_USER: autobrand
      POSTGRES_PASSWORD: dev_password
    ports: ["5432:5432"]
    volumes: ["db_data:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "autobrand"]
      interval: 5s

  app:
    build: .
    ports: ["8080:8080"]
    depends_on:
      db: { condition: service_healthy }
    environment:
      SPRING_PROFILES_ACTIVE: docker
      # Default credentials match the publicly documented web-scraping.dev login
      # (Verifică la implementare — site-ul oferă demo creds vizibile)
      SCRAPER_USERNAME: ${SCRAPER_USERNAME:-user123}
      SCRAPER_PASSWORD: ${SCRAPER_PASSWORD:-password}

volumes:
  db_data:
```

`Dockerfile` multi-stage:
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
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Comandă demo:** `docker compose up --build` → totul live la `http://localhost:8080`.

### Spring profiles

| Profile | Use case | DB | Cron |
|---|---|---|---|
| `dev` | Local dev (`mvn spring-boot:run`) | Postgres Docker | Off (manual trigger only) |
| `docker` | Containerized run | Postgres în compose | On |
| `test` | Test suite | Testcontainers Postgres | Off |

---

## 9. Testing Strategy

| Tip | Scope | Tools |
|---|---|---|
| **Unit (service)** | `ProductService.upsertAll` (manually_edited logic), `ExchangeRateService.recompute` (multiplier BNR), `AdAutoTotalInvoiceParser` (sample PDF), `CsvExportService` | JUnit 5 + Mockito |
| **Repository (slice)** | `ProductRepository.search` cu Specifications | `@DataJpaTest` + Testcontainers |
| **Controller (slice)** | Authorization, redirects, flash messages | `@WebMvcTest` + MockMvc |
| **Scraper** | Parser pe HTML fixture (`src/test/resources/fixtures/products-page.html`) | JUnit + Jsoup |
| **Integration (smoke)** | App pornește, Flyway aplicat, endpoints răspund | `@SpringBootTest` + Testcontainers |

**Excluse explicit:**
- ❌ E2E cu Selenium pe propriul UI (overkill).
- ❌ Tests pentru cron real (în loc, test pe `@Scheduled` config).
- ❌ Tests pentru network calls reale la BNR / web-scraping.dev (fixtures locale).

---

## 10. Error Handling

### `@ControllerAdvice` global
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ProductNotFoundException.class) → 404 page
    @ExceptionHandler(InvalidPdfException.class) → redirect cu flash error
    @ExceptionHandler(MaxUploadSizeExceededException.class) → flash error "file too large"
    @ExceptionHandler(Exception.class) → 500 page + log error
}
```

### Pagini eroare custom
`templates/errors/404.html`, `errors/500.html` cu același layout DaisyUI.

### Scraping resilience
- Network timeout → retry 3x cu backoff (manual, fără Spring Retry — YAGNI).
- Login fail → log + `scrape_run.status = FAILED` cu `error_message`.

### Upload validation
- File != PDF → flash error
- File > 5MB → flash error (config `spring.servlet.multipart.max-file-size=5MB`)

---

## 11. README structure

```markdown
# Proba Practică Autobrand — Boris Pavel

[Banner / screenshot dashboard]

> Rezolvare proba practică pentru Junior Full Stack Developer @ Autobrand

## 🔐 Credențiale demo
| Username | Parolă |
|---|---|
| `admin` | `Autobrand2026!` |

## 🚀 Pornire rapidă
\`\`\`bash
docker compose up --build
\`\`\`
→ http://localhost:8080

## 📋 Funcționalități
[Lista cu ✅ pentru cerințe + bonus]

## 🏗️ Stack
[Tabel librării + justificări]

## 🧭 Arhitectură
[Diagrama / packages]

## 🤔 Decizii & Trade-offs
- Jsoup vs Selenium
- Monolit vs multi-module
- Server-side vs client-side filtering
- Thymeleaf+HTMX vs React
- Testcontainers vs H2

## 🎬 Demo
[Screenshots: dashboard, lista produse, edit, upload PDF, login]
[GIF: scraping live, upload PDF + CSV download, filtering HTMX]

## 🧪 Testare
\`\`\`bash
mvn test
\`\`\`

## 📦 Posibile îmbunătățiri viitoare
[Lista YAGNI items]

## 📧 Contact
Boris Pavel — [CV email]
```

### Asset-uri demo
- Screenshot-uri PNG în `docs/screenshots/`
- GIF-uri în `docs/gifs/` (înregistrate cu ScreenToGif sau LICEcap)
- Linkuri inline în README cu paths relative.

---

## 12. Submission Process

1. Repo public pe GitHub (sau privat cu invitație la `hr@autobrand.ro`).
2. Verificare pe mașină fresh: `docker compose up --build` funcționează clean.
3. Verificare README complet, screenshots/gifs incluse.
4. Email la `hr@autobrand.ro`:
   - Subject: `Rezolvare proba practica Boris Pavel`
   - Body: link GitHub + credențiale demo (deși sunt și în README).

---

## 13. Out-of-Scope (YAGNI confirmate)

Pentru documentare la interviu — lucruri **conștient excluse**:

| Feature | De ce nu | Cum adresez la interviu |
|---|---|---|
| Multi-user | Single ADMIN suficient pentru proba | "Arhitectura permite extindere — `AppUser.role`, dar 1 user e suficient pentru scope" |
| OAuth / JWT | Session-based + form login mai simplu cu Thymeleaf | "JWT relevant pentru SPA / API; aici e SSR" |
| Persistență invoice-uri | Cerința e doar upload→CSV | "Adăugare facilă: 2 tabele `invoice` + `invoice_line`" |
| Audit log produse | Overkill | "Pattern Hibernate Envers dacă ar fi nevoie" |
| Microservicii | 1-week project, scope mic | "Premature optimization; modulul scraper extras ușor la nevoie" |
| Retry policy structurat (Spring Retry) | Manual retry simplu e suficient | "Spring Retry pattern matur pentru producție" |
| CI/CD pipeline | Out of scope | "GitHub Actions ar fi pasul natural — Maven build + Docker image push" |

---

## 14. Risks & Mitigation

| Risc | Probabilitate | Mitigation |
|---|---|---|
| Login la web-scraping.dev cere JavaScript | Medium | Switch la Selenium WebDriver (documentat ca fallback). |
| Layout factură PDF parser nu prinde toate liniile | High | Test pe sample-ul oferit; logică defensivă (skip lines fără regex match); manual review CSV. |
| Beginner Java, ritm încet | High | Stack ales conservator (Spring Boot standard); explicații în comentarii cod + README. |
| BNR XML schimbă formatul | Low | Test cu XML mock-uit; defensive parsing. |
| Time overrun | Medium | Prioritate strictă: cerințe → bonus 3 (auth) → bonus 2 (filter/sort) → bonus 1 (curs valutar) → polish (screenshots, etc.). |

---

## 15. Implementation Order (high-level)

Va fi detaliat în planul de implementare separat (writing-plans skill), dar la nivel macro:

1. Setup mediu: install JDK 21, IntelliJ, Docker, init Spring Boot project.
2. Database: Postgres în Docker, Flyway, schema produse + scrape_run.
3. Scraping: Jsoup login + parse, ProductService upsert, primul scrape manual.
4. UI base: layout Thymeleaf + Tailwind/DaisyUI, lista produse.
5. CRUD: edit, delete, manually_edited flag.
6. Cron: `@Scheduled` + ApplicationReadyEvent.
7. PDF: PDFBox parse + CSV export endpoint.
8. Auth (bonus #3): Spring Security + login page + seed user.
9. Filter/sort (bonus #2): Specifications + HTMX swap.
10. Curs valutar (bonus #1): BNR XML + price_ron compute.
11. Dashboard: stats + Chart.js donut + scrape_run table.
12. Tests: unit + repository + controller + integration.
13. Polish: README, screenshots, gifs, error pages.
14. Submission email.
