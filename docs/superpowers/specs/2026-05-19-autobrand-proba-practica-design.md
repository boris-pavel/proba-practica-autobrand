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
| Build | **Maven** | Vezi justificare detaliată mai jos. |
| Persistență | **Spring Data JPA + Hibernate** | Repository pattern out-of-the-box; reduce boilerplate CRUD. |
| DB runtime | **PostgreSQL 16** (Docker) | DB serios de producție; cunoscut în industrie. |
| DB test | **Testcontainers Postgres** | Teste pe același engine ca producția (vs. H2 care divergează). |
| Migrări | **Flyway** | Schema versionată în git; reproductibilă. |
| Templating | **Thymeleaf** | Integrare nativă Spring; server-side; no build pipeline. |
| Frontend CSS | **Tailwind CSS (Play CDN)** + **DaisyUI** | Look modern fără build pipeline; componente DaisyUI gata. |
| Interactivitate | **HTMX** | SPA-feel fără JS framework; trendy în comunitatea Spring (Thoughtworks Radar 2024). |
| Charting | **Chart.js (CDN)** | Un singur donut chart pe dashboard. |

### Justificare detaliată: Build tool — Maven vs. alternative

**De ce Maven:**

1. **Accesibilitate pentru beginner Java.** `pom.xml` este XML pur — verbos, dar liniar și predictibil. Pentru cineva care învață Java + Spring Boot simultan, nu vrei să adaugi încă un limbaj (Groovy sau Kotlin DSL) doar pentru build script. Un developer junior poate citi un `pom.xml` și înțelege ce face în primele 10 minute, fără să cunoască sintaxa.

2. **Defaults Spring Initializr și ecosistem.** `start.spring.io` generează Maven implicit. ~70% din tutorialele și răspunsurile Stack Overflow pentru Spring Boot folosesc Maven. Asta înseamnă mai puțin "translate from Gradle" în timp de debugging.

3. **Convention over configuration.** Layout standard (`src/main/java`, `src/test/java`), goal-uri predictibile (`mvn clean install`, `mvn test`, `mvn spring-boot:run`). Zero gândire la "ce-i config-ul de build pentru proiectul ăsta?".

4. **Toleranță IDE matură.** IntelliJ IDEA suport native Maven de ani, fără plugin-uri externe.

5. **Context Autobrand.** JD-ul cere Java; companiile auto românești sunt majoritar pe stack Maven (din observații industrie). Maven nu va fi nicicând o "alegere ciudată" la interviu.

**Alternative excluse — de ce nu le folosim:**

| Alternativă | Pro | Contra care domină pentru cazul ăsta |
|---|---|---|
| **Gradle (Groovy DSL)** | ~30% mai concis decât Maven; build incremental rapid; flexibilitate cu plugin-uri custom. | **Groovy e dinamic și magic:** `compile 'org.foo:bar:1.0'` arată ca o metodă, dar e închidere DSL. Pentru beginner, debug-ul e dureros — erorile sunt criptice. **SO answer rot:** multe răspunsuri online folosesc Gradle 4-6 (diferit de 8.x curent). **Câștigul de incremental build = 0** pentru un proiect de ~20 dependențe și 1 modul. La interviu, "Why Gradle?" cere apărarea: ai cunoaște intern doar pentru bonus, nu necesar. |
| **Gradle Kotlin DSL (`build.gradle.kts`)** | Type-safe; autocomplete în IDE; modern, "the future of Gradle"; Android dev standard. | **Stack double:** vrei să înveți Spring + Java + JPA + Hibernate + Spring Security + Thymeleaf, iar acum și Kotlin? Pentru un beginner, e overload. **Doc fragmentate:** Gradle docs au exemple atât Groovy cât și Kotlin, ușor confuz. **Risc explicabilitate:** la interviu, "De ce Kotlin DSL pentru un proiect Java?" cere răspuns nuanțat — beginner riscă "așa am văzut online". |
| **Bazel (Google)** | Foarte rapid, polyglot, hermetic builds, folosit la Google/Uber/Stripe. | **Overkill brutal** pentru single-module Java app. Curve de învățare foarte abruptă (`BUILD` files, `WORKSPACE`, target labels). **Zero integrare Spring Boot** out-of-the-box; ai construi manual rules. **Comunitate redusă** pentru Java standalone — majoritar Go/C++/Kotlin Android. La interviu, "Bazel pentru o probă?" sună a CV-padding. |
| **Apache Ant** | Există de 25 ani, multă documentație istorică. | **Considerat deprecated** în Java modern. Fără central repo pentru dependențe (trebuie download manual). Nu e suportat de Spring Initializr. Folosit doar în proiecte legacy mai vechi de ~2010. La interviu, "Ant?" sună a "n-am cercetat alternativele". |
| **sbt** (Simple Build Tool) | Standard pentru Scala. | **Nu e pentru Java pur.** Sintaxa și conceptele (settings vs tasks, lazy evaluation) sunt specifice Scala. Irelevant aici. |
| **Make / shell scripts** | Familiar, zero dependențe. | **Nu gestionează dependențe Java** (download din Maven Central, transitive deps, versiuni conflicte). Reinventezi roata, prost. **Zero suport Spring Boot plugin.** |
| **No build tool / `javac` manual** | Conceptual minimal. | Imposibil pentru un proiect cu 20+ dependențe Spring tranzitive — ai gestiona manual ~100 JAR-uri. |

**Concluzie:** Maven câștigă pe **simplicitate + ecosistem + risk explicabilitate la interviu**, nu pe features. La interviu, dacă te întreabă "De ce nu Gradle?", răspuns ferm: *"Pentru un proiect de o săptămână, beneficiile Gradle (concisitate, incremental builds) nu compensează cost-ul cognitiv de a învăța Groovy/Kotlin DSL în paralel cu Spring. Dacă mâine aș lucra pe un monorepo cu 50 module, aș reconsidera Gradle."*

---

### Specialized
| Componentă | Alegere | Detalii |
|---|---|---|
| Web scraping | **Jsoup 1.17** (primary) + Selenium fallback | Vezi 2.1 |
| PDF parsing | **Apache PDFBox 3.x** | Vezi 2.2 |
| Frontend templating | **Thymeleaf 3.x** | Vezi 2.3 |
| Frontend interactivitate | **HTMX 2.x** | Vezi 2.4 |
| CSS framework | **Tailwind CSS (Play CDN) + DaisyUI** | Vezi 2.5 |
| Database runtime | **PostgreSQL 16** | Vezi 2.6 |
| ORM | **Spring Data JPA + Hibernate** | Vezi 2.7 |
| Migrări | **Flyway 10.x** | Vezi 2.8 |
| Auth | **Spring Security + BCrypt** | Vezi 2.9 |
| Test DB strategy | **Testcontainers Postgres** | Vezi 2.10 |
| CSV export | **Apache Commons CSV 1.11** | Standard Apache; tested, simplu API. Alternativa OpenCSV are licență mai restrictivă (LGPL în versiuni vechi). |
| HTTP client (BNR) | **Spring `RestClient`** (Spring 6.1+ / Boot 3.2+) | Modern, fluent, înlocuiește `RestTemplate` (în maintenance) și e mai simplu decât `WebClient` (care e reactiv, overkill aici). |
| XML parsing (BNR) | **Jackson Dataformat XML** | Deja în clasa de dependențe Spring; annotation-driven. Alternativa JAXB a fost scoasă din Java SE 11 (JEP 320, mutată la Jakarta EE) — trebuie adăugată ca dependență separată dacă o folosești. |
| Cron scheduler | **Spring `@Scheduled`** | Built-in, zero dependencies. **Quartz exclus**: aduce JobStore configurabil (DB sau RAM), clustering, misfire policies — utile la scale, inutile pentru un cron simplu cu fereastră fixă 12–18. |
| Validation | **Bean Validation (Hibernate Validator)** | JSR 380 standard; declarative; Spring integrare nativă. Singura alternativă realistă (custom validators) duplică efort. |
| Reducere boilerplate | **Lombok** | `@Data`, `@Builder`, `@Slf4j` taie sute de linii de getters/setters. Alternative: records Java 21 (parțial, dar entități JPA nu pot fi records din cauza proxy-urilor Hibernate). |
| Logging | **SLF4J + Logback** | Default Spring Boot. Alternative (Log4j2) ar fi necesar `exclusions` în pom; câștig minim. |

---

### 2.1 Justificare detaliată: Web scraping — Jsoup vs. alternative

**De ce Jsoup primary:**
1. **Cel mai simplu API Java pentru HTML parsing.** Selector CSS familiar: `doc.select("div.product-card")` — exact ca jQuery / DOM browser. Curve de învățare ~30 minute.
2. **Suport built-in pentru form login.** `Jsoup.connect(url).data("username","x").method(POST).execute()` returnează `Response` cu cookies pe care le pasezi mai departe. Zero ceremonie.
3. **Zero binaries externe.** Un singur JAR (~430KB). Nu trebuie ChromeDriver, GeckoDriver, Node, Playwright runtime.
4. **Rapid.** ~50-100ms per pagină (vs. 2-5 secunde Selenium). Pentru un cron care rulează 7x/zi, asta contează la log volume și CI time.
5. **Output predictibil.** Parse error → exception clară. Nu te pune în situație de "timeout, dar nu știu de ce".

**Alternative excluse:**

| Alternativă | Pro | Contra care domină |
|---|---|---|
| **Selenium WebDriver** | Suportă JS-rendered content (SPA, React, etc.). Mature, foarte documentat. | **Necesită ChromeDriver binary** managed manual sau cu WebDriverManager. **Slow** — pornește browser real, ~2-5s per pagină. **Flaky** — pagini cu animații / async pot eșua intermittent. Pentru web-scraping.dev (sandbox simplu), pune complexitate fără câștig. **Fallback documentat:** dacă login-ul cere JS, putem switch la Selenium fără refactor major (interface `Scraper` permite). |
| **Playwright Java** | Mai modern decât Selenium (auto-wait built-in, network interception, multi-browser cu același API). | **API Java mai puțin maturat** decât JS/Python (Java port e port secundar). **Comunitate mai mică** pentru Java specific. **Aceeași încărcătură ca Selenium** (browser process). Nu îmi dă nimic peste Selenium ca fallback. |
| **HtmlUnit** | Browser headless Java pure (fără ChromeDriver). | **Execută JS limitat** (motor Rhino vechi). **Lent.** Compromisul nefericit între Jsoup și Selenium — fără punctele forte ale niciuneia. |
| **HTTP client raw (RestClient/HttpClient) + regex** | Zero dependențe, maximally minimal. | **HTML parsing cu regex e anti-pattern** (vezi celebrul SO answer 1732454). HTML are nesting, atribute, escapes — regex prinde 80% și se rupe la cazuri reale. |
| **Curl + bash + jq** | Lightweight, familiar la sysadmins. | **Nu există în context Java/Spring;** ar trebui invocat din ProcessBuilder. Cross-platform nightmare (Windows vs Linux). |

**Risk acknowledged și mitigation:** dacă la primul test găsesc că `web-scraping.dev/login` necesită JS pentru login (anumite versiuni au CSRF tokens injectate via JS), switch la Selenium prin schimbarea unui singur bean `Scraper`. Interface-ul rămâne identic.

**Interview defense:** *"Am ales Jsoup pentru pagini HTML server-rendered, ceea ce e cazul web-scraping.dev. Selenium e prea greu pentru un cron — un crawler real-world cu Selenium ar trebui să gestioneze pool de browsere, restart-uri, memory leaks. Pentru un sandbox cu format previzibil, Jsoup câștigă pe simplicity, speed și debuggability. Dacă target-ul ar fi fost o aplicație SPA Real-World™ (Amazon, eBay), Selenium ar fi fost alegerea evidentă."*

---

### 2.2 Justificare detaliată: PDF parsing — Apache PDFBox vs. alternative

**De ce Apache PDFBox 3.x:**
1. **Apache 2.0 license** — folosit liber în orice context, inclusiv comercial. Critic pentru un proiect care merge într-o companie.
2. **De-facto standard Java pentru PDF text extraction.** Suportă PDF 1.0 → 2.0; PDFTextStripper extrage text cu poziții (X, Y) — util pentru parser cu coloane.
3. **Maintained activ** de Apache Foundation; ultima release 3.0.2 (mid-2024).
4. **API simplu:**
   ```java
   try (PDDocument doc = Loader.loadPDF(file)) {
       String text = new PDFTextStripper().getText(doc);
   }
   ```
5. **Suportă forms, signatures, annotations** — peste cerința noastră, dar arată profesionalism.

**Alternative excluse:**

| Alternativă | Pro | Contra care domină |
|---|---|---|
| **iText 7 / 8** | API mai bogat (PDF generation, manipulare avansată), foarte folosit comercial. | **AGPL license** la varianta open-source — copyleft viral, încarcă întregul proiect cu obligația de a publica source dacă distribui binar. **Licență comercială pe iText e plătită** (mii €/an). Pentru o probă într-un proiect care va fi public pe GitHub, AGPL = legal headache. **Risk explicabilitate la interviu:** "ai folosit AGPL într-un proiect comercial?" — răspuns greu de apărat. |
| **Apache Tika** | Wrapper peste PDFBox + alte format-uri (Word, Excel, etc.); detectare automată format. | **Overhead masiv** — Tika trage zeci de dependențe (POI pentru Office, parser-i Office, etc.). Pentru DOAR PDF, e PDFBox + 50MB de dead weight. Dacă cerința ar fi "orice tip de fișier", Tika ar fi alegerea. |
| **PDFTron / Aspose** | API comercial, suport excelent, multiple formate, accuracy mai mare la layout-uri complexe. | **Licență comercială scumpă** (~$1000+/an). Nu folosim soluții pay-walled pentru open project. |
| **iText 5 (LGPL)** | Versiune veche, LGPL (mai permisiv decât AGPL). | **End-of-life din 2015**, fără security patches, fără suport PDF modern (PDF 2.0). Risc serios. |
| **Custom parser (PDFBox low-level + manual)** | Control total. | Reinventezi PDFTextStripper. Anti-pattern. |

**Interview defense:** *"PDFBox a câștigat principalele pe licență — Apache 2.0 permite orice utilizare. iText 7 ar fi tehnic puternic, dar AGPL viral ar fi forțat proiectul la AGPL, ceea ce e prohibitiv pentru cod care merge într-o companie. Dacă proiectul ar fi avut licență comercială (companie cu license budget), iText 7 ar fi fost evaluat serios pentru PDF generation features."*

---

### 2.3 Justificare detaliată: Frontend templating — Thymeleaf vs. alternative

**De ce Thymeleaf 3.x:**
1. **Integrare nativă cu Spring Boot.** `spring-boot-starter-thymeleaf` configurează totul; `Model` din controller merge direct în template.
2. **HTML "valid prototype"** — un fișier `.html` Thymeleaf poate fi deschis în browser fără server (atributele `th:*` sunt ignorate). Designeri pot lucra fără să ruleze Java.
3. **Sintaxă declarativă, intuitivă:** `<span th:text="${product.name}">Placeholder</span>` — placeholder-ul rămâne în HTML static.
4. **Fragmente reutilizabile** (`th:fragment`, `th:replace`) — like includes, dar mai puternic (parametrii).
5. **Security integration** — `sec:authorize="hasRole('ADMIN')"` direct în template (cu starter).

**Alternative excluse:**

| Alternativă | Pro | Contra care domină |
|---|---|---|
| **FreeMarker** | Mai rapid la rendering (~2x); sintaxă concisă (`${name}`, `<#if>`); folosit la Yelp, Mailchimp. | **Nu e HTML valid** — `<#if>`, `<#list>` rupe HTML prototyping. Sintaxa de scapă (`?html`, `?json`) e proprie. **Spring Boot starter exists, dar comunitatea Spring favorizează Thymeleaf** (mai multe tutoriale, exemple, plugin-uri IntelliJ). Câștig de performance irelevant la scope-ul nostru. |
| **JSP** | Legacy Java EE standard. | **Deprecated pentru Spring Boot embedded servers** (Tomcat embedded nu suportă JSP fără config special); **scripting Java în template** încurajează rău mixaj logică/view; **fără hot reload** decent. |
| **Mustache / Handlebars Java** | Logic-less template philosophy; folosit cross-language. | **Prea limitat:** fără if-else complex, fără fragments cu parametri. Trebuie multă logică în controller, ceea ce face controller-ul gros. **Spring Boot integration există dar marginală.** |
| **Pebble** | Inspirat din Twig (PHP), foarte concis. | **Comunitate mică în Java.** Risc de "cine altcineva îl folosește?" la interviu. **Spring Boot integration via 3rd party**, nu oficial. |
| **Velocity** | Vechi Apache, încă funcțional (Velocity Engine 2.4 din mai 2024). | **Maintenance ritm foarte scăzut** (release-uri rare, comunitate redusă). Pierde teren în favoarea Thymeleaf și FreeMarker — la interviu, "Velocity?" sună a "n-am cercetat ce e curent". |
| **React / Vue rendered server-side** | Modern, isomorphic. | **Cere Node.js runtime** (Nashorn deprecated, GraalJS overhead). Cross-stack complexity prohibitive pentru beginner Java + 1 săptămână. |

**Interview defense:** *"Thymeleaf e standardul de-facto Spring Boot. FreeMarker e tehnic competitiv, dar Thymeleaf are avantajul HTML-valid prototyping — designerul/PM-ul poate deschide fișierul direct în browser. La scale care contează (mii req/sec), aș reconsidera FreeMarker pentru câștig de throughput."*

---

### 2.4 Justificare detaliată: Frontend interactivitate — HTMX vs. alternative

**De ce HTMX 2.x:**
1. **Server-driven UI fără SPA framework.** Toată logica rămâne pe backend; HTMX doar swap-uiește fragmente HTML.
2. **Atribute HTML declarative:** `<button hx-post="/products/123/delete" hx-target="#row-123" hx-swap="outerHTML">Delete</button>` — fără JS scris manual.
3. **Bundle minim:** ~14KB minified+gzipped. Zero build pipeline.
4. **Sinergie perfectă cu Thymeleaf** — Spring controller returnează un fragment Thymeleaf (`return "products/list :: row(${product})"`), HTMX îl inserează în DOM.
5. **Hot din 2023:** Thoughtworks Tech Radar "Trial" (Vol 30 Apr 2024 & Vol 31 Oct 2024); ecosistemul Hotwire (Basecamp / 37signals) merge pe filosofie identică.

**Alternative excluse:**

| Alternativă | Pro | Contra care domină |
|---|---|---|
| **React + Spring Boot REST API** | Industry standard pentru SPA-uri; ecosistem enorm; "Full Stack" în sens modern. | **Doublezi efortul de învățare:** Spring + React simultan pentru beginner = burnout. **CORS configurat manual.** **Build pipeline necesar:** Node, npm, Vite/webpack — încarcă proiectul. **2 limbaje, 2 deploy units, 2 dependency trees.** Pentru o probă într-o săptămână, ROI negativ. |
| **Vue.js** | Mai prietenos decât React pentru începători; SFC `.vue` files. | Aceleași contra ca React, doar într-un ecosistem mai mic. |
| **Alpine.js** | "jQuery modern", inline directives în HTML (`x-data`, `x-show`). Mic (~15KB). | **Pure client-side state**, nu face request server. Pentru CRUD-uri cu state DB-driven, ai nevoie totuși de logică de fetch — duplica HTMX pe scenariu mai limitat. **HTMX + Alpine** funcționează împreună (folosit împreună), dar fără cerință de state client, e overhead. |
| **Vanilla JS + `fetch()`** | Zero dependențe. | **Boilerplate masiv** pentru fiecare delete/edit: scriu `fetch`, `.then`, DOM manipulation manual, error handling. La 10 endpoints, ai sute de linii JS replicate. **HTMX e exact abstractizarea peste asta.** |
| **Turbo (Hotwire)** | Filosofie similară cu HTMX; folosit la Basecamp. | **Mai cuplat la Rails** convențional. **Spring integration via 3rd party** (turbo-spring), mai puțin maturat decât HTMX integration. |
| **jQuery + AJAX clasic** | Familiar, prevalent în legacy. | **2010 vibes** la interviu. Sintaxa imperativă (manipulare DOM manuală), bundle mare (~85KB), nu mai e considered modern. |
| **Server-side full refresh only** | Simplu, fără JS. | **UX rău:** delete → page reload → flash de încărcare → context pierdut (scroll, focus). Pentru "modern, impresionant", nu trece bara. |

**Interview defense:** *"HTMX e pariul pe filosofia HOWL (Hypermedia On Whatever you'd Like) — server-side rendering modern, în spiritul REST original al lui Roy Fielding (HATEOAS). La un proiect Spring Boot + Thymeleaf, HTMX e mariajul natural: controller-ul returnează HTML fragment, HTMX face swap. Am evitat React/Vue pentru că ar fi dublat scope-ul de învățare fără a aduce valoare la cerințele acestei probe. Pentru un app cu state client complex (drag-drop, animations, offline), aș folosi React."*

---

### 2.5 Justificare detaliată: CSS framework — Tailwind + DaisyUI vs. alternative

**De ce Tailwind CSS (Play CDN) + DaisyUI:**
1. **Tailwind Play CDN = zero build pipeline.** Un singur `<script src="https://cdn.tailwindcss.com">` și ai tot Tailwind în pagină.
2. **Utility-first** — class-uri atomice (`flex items-center gap-2 px-4 py-2 rounded-lg`) eliminate custom CSS pentru 95% din cazuri.
3. **DaisyUI = componente pre-styled** (`btn btn-primary`, `card`, `modal`, `drawer`) peste Tailwind. Reduce verbose-ness.
4. **Teme built-in DaisyUI** — `corporate`, `business`, `dim`, `dark` etc., switchable cu `data-theme="..."` pe `<html>`. Toggle de temă = feature gratis.
5. **Look modern** — sătul de "Bootstrap 5 generic". DaisyUI arată distinct, profesional, 2024.

**Alternative excluse:**

| Alternativă | Pro | Contra care domină |
|---|---|---|
| **Bootstrap 5** | Cel mai cunoscut framework; documentație excelentă; componente bogate. | **Look "Bootstrap-y"** instant recognizable, asociat cu sites din 2015-2020. **Class-uri puține și predictibile dar limitative** (`btn btn-primary` are 5 variante, nu poți customize ușor). Pentru "modern, impresionant" — nu trece bara, e prea common. |
| **Bulma** | CSS pur (no JS), mai concis decât Bootstrap. | **Maintained slab** în ultimii 2 ani; comunitate mai mică. Look e tot "framework genericism". |
| **Tailwind cu build pipeline (PostCSS, JIT)** | Bundle final optimizat (~10KB după purge); production-ready. | **Cere Node + npm + config** pe lângă build Maven. Dublezi setup-ul. **Play CDN are toate clasele oricum la dev**, doar la production ai costul (~3MB ungzipped). Pentru o probă, nu contează. |
| **DaisyUI cu build** | Componente Tailwind plus optimizare. | Same as above — pipeline overhead. |
| **Pico CSS** | Minimal (~10KB), folosește elemente HTML semantic (no class needed). | **Look prea minimalist** pentru "impresionant". Lipsesc componente complexe (drawer, modal complex). |
| **Material UI / Material Design Lite** | Look standardizat Google. | **Massive bundle** (~300KB). Foarte opinionated visual — toate site-urile Material seamănă. |
| **Custom CSS** | Control absolut. | **Time sink masiv** — zile întregi pentru a obține un look modern. Pentru un proiect de 1 săptămână, irațional. |

**Interview defense:** *"Tailwind elimină hartuiala 'unde scriu CSS-ul ăsta?'. La un proiect mic, Play CDN e tradeoff acceptabil — la production cu trafic real, aș configura PostCSS build cu JIT pentru bundle de ~10KB. DaisyUI adaugă componente fără să încalce filosofia utility-first; dă cookie-cutter look out-of-the-box pe care îl pot customiza dacă vreau."*

---

### 2.6 Justificare detaliată: Database runtime — PostgreSQL vs. alternative

**De ce PostgreSQL 16:**
1. **DB serios open-source standard industry.** Folosit la Instagram, Reddit, Stripe (parțial). În Romania, larg adoptat.
2. **Suport robust pentru toate feature-urile noastre:** UNIQUE constraints, indexes (B-tree pentru sort), JSON columns (dacă vrem extindere), Full-Text Search built-in.
3. **Excelent cu Hibernate / JPA** — dialect maturat, fără surprize.
4. **ACID strict** — necesar pentru upsert logic concurrent în scraping.
5. **Pe Docker oficial alpine**: ~80MB image, start în <5s, healthcheck simplu.

**Alternative excluse:**

| Alternativă | Pro | Contra care domină |
|---|---|---|
| **MySQL 8** | Foarte popular în Romania, hosting ieftin (cPanel, etc.). | **Defaults problematice istoric** (utf8 ≠ utf8mb4, MyISAM vs InnoDB pe versiuni vechi). **Suport JSON / indexare avansată mai recent** decât Postgres. **License Oracle ambiguă pentru MySQL Server** (Community vs Enterprise) — MariaDB e fork-ul. Pentru proiecte noi, Postgres e default-ul modern. |
| **MariaDB** | MySQL fork, license GPL clară. | Aceleași limitări tehnice ca MySQL. Comunitate mai mică decât Postgres pentru tutoriale Spring Boot. |
| **H2 in-memory** | Zero install, embeded in app, fast. | **Date pierdute la restart.** **Dialect 95% compatibil cu Postgres** dar 5% diferă (funcții specifice, tipuri). Risc: test trece pe H2, pică în prod pe Postgres. Folosit doar pentru `test` profile cu mock-uri simple. |
| **SQLite** | Single file, zero server, embedded. | **Concurrency slabă** (file locking) — un singur writer la un timp. Cron-ul scraping ar putea bloca UI. **Lipsă tipuri stricte** până recent (3.37+). Folosit pentru aplicații desktop/mobile, nu server web. |
| **MongoDB / DynamoDB / Cassandra** | NoSQL, schema-less, scalează horizontal. | **Cerința menționează SQL explicit** ("Problema web scraping + SQL"). **Schema noastră e relațională puternic** (produse, exchange_rates, users, scrape_runs). NoSQL ar fi anti-pattern. |
| **Oracle DB** | Folosit în mari corporații (auto industry frecvent). | **License costisitoare** (~$47K/proc), instalare grea. Pentru o probă, prohibitiv. |
| **SQL Server** | Foarte folosit în corporații .NET; license Express gratis. | **Mai puțin idiomatic în ecosistem Java/Spring.** Docker image (~1.5GB) prea greu pentru probă. |

**Interview defense:** *"Postgres e default-ul modern pentru proiecte noi Java/Spring în 2026 — comunitatea Spring favorizează Postgres, are dialect Hibernate maturat, license MIT-like (PostgreSQL License) și suport bogat pentru feature-uri pe care le-am putea adăuga (JSON, full-text search, range types). MySQL ar fi fost ales 5-10 ani în urmă; astăzi Postgres e the safe modern choice."*

---

### 2.7 Justificare detaliată: ORM — Spring Data JPA vs. alternative

**De ce Spring Data JPA + Hibernate:**
1. **Default Spring Boot ORM**, integrare zero-config.
2. **Repository pattern out-of-the-box:** scrii o interfață `extends JpaRepository<Product, Long>` și ai CRUD complet plus method query derivation (`findByName(String)` se traduce automat în SQL).
3. **Specifications API** — query dinamic, type-safe, pentru filtering avansat (cerință în spec).
4. **Tranzacții declarative** prin `@Transactional`.
5. **JPQL + native SQL** când ai nevoie de query custom.
6. **Maturat:** Hibernate 6.x stable, folosit la majoritatea companiilor Java enterprise.

**Alternative excluse:**

| Alternativă | Pro | Contra care domină |
|---|---|---|
| **JOOQ** | Type-safe SQL DSL; generate cod din schema DB; control fin asupra SQL. | **Curve de învățare** suplimentară (DSL fluent custom, codegen pipeline). **Licență Dual** — open-source pentru open-source DB-uri (Postgres OK), comercial pentru Oracle/SQL Server. **Setup Maven plugin pentru codegen.** Pentru CRUD simplu, JPA e mai concis. JOOQ shine la reporting / queries complexe cu joins multiple. |
| **MyBatis** | SQL written by hand, mapping XML/annotations; control total. | **Boilerplate XML** semnificativ. **Fără magic** = scrii tu fiecare query. Pentru CRUD repetitiv, e mult code. Folosit prevalent în firme asiatice (Korea, China), mai puțin în Europa. |
| **jdbi** | Light wrapper peste JDBC; mai modern decât MyBatis. | **Manual mapping** ResultSet → entity. Lipsește lazy loading, schimbare detection (dirty checking). Mai puține bell-and-whistles decât JPA. |
| **Plain JDBC + JdbcTemplate** | Maxim control, minim magic. | **Manual mapping** la fiecare query. Tracking changes inexistent. Pentru entități cu relații, devine repetitive. **Spring JdbcClient** (Java 21) e mai prietenos, dar tot mai verbose decât JPA. |
| **Spring Data JDBC** (nou, non-Hibernate) | Mai simplu decât JPA, fără lazy loading magic. | **Specifications API nu există** la Spring Data JDBC (e doar Spring Data JPA feature). Pentru filtering dinamic, n-aș avea unealta principală. |
| **Hibernate fără Spring Data** | Same Hibernate, fără layer-ul Spring Data abstractions. | Scrii manual `EntityManager.createQuery(...)` peste tot. Pierzi tot zahărul Spring Data. |

**Risk acknowledged (Hibernate "magic"):** lazy loading + LazyInitializationException, N+1 queries, dirty checking în tranzacții — toate sunt capcane clasice. **Mitigation:** folosesc `@Transactional` pe service layer (nu controller), evit lazy collections în Thymeleaf templates (fetch eager pe queries explicit cu `JOIN FETCH` sau `EntityGraph`).

**Interview defense:** *"Spring Data JPA pentru viteză de development — repository derivation și Specifications taie 80% din boilerplate. Sunt conștient că Hibernate are 'magic' care poate surprinde — N+1 queries, lazy init, cascade. Pentru fiecare query important folosesc `EntityGraph` sau JPQL explicit. JOOQ ar fi alegere mai serioasă pentru un sistem heavy-reporting; pentru CRUD + scraping, JPA câștigă pe productivity."*

---

### 2.8 Justificare detaliată: Migrări DB — Flyway vs. alternative

**De ce Flyway 10.x:**
1. **Versionare schemă în git** — fiecare migration e un fișier SQL imutabil (`V1__create_product.sql`, `V2__add_index.sql`).
2. **Aplicat automat la startup Spring Boot** — `spring.flyway.enabled=true` (default).
3. **Migration history tracked în DB** (tabela `flyway_schema_history`) — știi exact ce s-a aplicat și când.
4. **SQL pur** — nu trebuie să înveți DSL custom; scrii Postgres SQL nativ.
5. **Simplu de explicat:** "git pentru schemă DB".

**Alternative excluse:**

| Alternativă | Pro | Contra care domină |
|---|---|---|
| **Liquibase** | XML/YAML/JSON changelog (alternativă la SQL); rollback declarative. | **Verbose** — XML pentru un CREATE TABLE e mai mult tipart decât SQL pur. **Database-agnostic** (genereaza SQL diferit per DB) — câștig irelevant când știm că folosim Postgres. **Spring Boot starter există** dar comunitatea Spring favorizează Flyway. **La interviu:** "Liquibase?" — răspuns mai greu de apărat decât Flyway. |
| **Hibernate `ddl-auto: update`** | Zero config; Hibernate generează SQL din entities. | **Periculos în producție** — modificări neașteptate, nu poate face DROP coloane, nu rollback. **No history.** **Tutoriale Spring Boot zic explicit** "don't use in production". Acceptabil DOAR în `test` profile. |
| **Hibernate `ddl-auto: create-drop`** | Util pentru teste. | Distruge totul la fiecare start; doar pentru testing. |
| **Manual SQL scripts** | Zero tooling. | **Manual coordinare** între developeri — cine a aplicat ce, când? Anti-pattern. |
| **Liquibase Pro / Datical** | Enterprise features. | License cost. |

**Convenție folosită:**
- `V1__create_product_table.sql` (V + version + __ + descriere)
- Migrations imutabile — odată committed, NU modifici. Adăugi `V2__alter_product_add_x.sql`.
- Pentru data seed: `V4__seed_admin_user.sql` (versionat, NOT repeatable migration `R__`, ca să fie predictibil).

**Interview defense:** *"Flyway pentru simplitate — SQL pur, versionat în git, applied automatic. Liquibase ar fi alegerea dacă proiectul ar trebui să suporte multiple DB engines paralele, but pentru single-engine (Postgres) e overhead. Hibernate `ddl-auto: update` e tentant pentru dev rapid, dar pierde control + reproducibilitate — într-o echipă de 5 oameni ar fi haos."*

---

### 2.9 Justificare detaliată: Autentificare — Spring Security vs. alternative

**De ce Spring Security + form login + BCrypt:**
1. **Industry standard Java** — orice job listing Java menționează Spring Security.
2. **Filter chain configurabil**, security defaults strong (CSRF on, X-Frame headers, etc.).
3. **BCrypt password encoder built-in** — algoritm slow-by-design, rezistent la GPU brute force.
4. **Session-based via Spring Session** sau in-memory (default) — sustainable la scope-ul nostru.
5. **Method-level security** disponibilă (`@PreAuthorize("hasRole('ADMIN')")`) — extensibil dacă scope-ul crește.

**Alternative excluse:**

| Alternativă | Pro | Contra care domină |
|---|---|---|
| **Custom session-based auth (HttpSession + filter scris de mână)** | Control total, "I know exactly what's happening". | **Securitate prost-implementată e mai rea decât nicio securitate.** Trebuie să gestionezi: CSRF tokens, session fixation, secure cookie flags, timing-safe password compare, password hashing (cost adequat), brute force throttling. **Spring Security le face toate corect, by default.** La interviu, "ai scris auth de la zero?" = red flag major. |
| **JWT (JSON Web Tokens)** | Stateless, scalează horizontal fără session store; popular în SPAs / mobile. | **Anti-pattern pentru server-rendered apps cu Thymeleaf.** JWT shine la API-uri consumed de SPA/mobile, unde browser nu poate folosi cookie session. Pentru web app monolitic cu form login, session-based e mai simplu + revocation imediată (logout = invalidate session). JWT necesită refresh tokens, blacklist pentru logout — complexitate fără câștig. |
| **OAuth2 / OpenID Connect (Keycloak, Auth0, Okta)** | Auth ca service; SSO; user management UI gratis. | **Overkill brutal** pentru un proiect cu 1 user demo. Keycloak self-hosted = încă un container Docker (~700MB). Auth0/Okta = SaaS plătit. La interviu, "OAuth pentru un 1-user demo" = over-engineering. |
| **Spring Security cu OAuth2 social login (Google, GitHub)** | UX modern, fără parole. | **Nu se aplică:** demo trebuie să fie accesibil recruiterului fără ca el să facă login cu contul lui personal. |
| **Apache Shiro** | Alternativ matur la Spring Security. | **Comunitate mult mai mică.** Spring Boot integration mai puțin polished. Risk explicabilitate la interviu. |
| **Basic Auth (HTTP Authorization: Basic)** | Simplu, built-in HTTP. | **UX rău** — browser nu are logout proper, popup de credentials, no remember-me, no friendly login page. Acceptabil pentru endpoint-uri tehnice (admin tools), inacceptabil pentru UI demo. |
| **No auth** | Cel mai simplu. | **Cerința bonus explicită cere auth.** Lipsa = nu îndeplinești cerința. |

**Interview defense:** *"Spring Security e default-ul Spring Boot pentru auth — îmi dă CSRF, BCrypt, session management, secure cookies out-of-the-box. JWT ar fi fost alegerea pentru un SPA + REST API, dar avem server-rendered Thymeleaf — JWT acolo introduce complexitate fără valoare. Pentru un 1-user demo cu posibilitate de extindere la multi-user, Spring Security cu `AppUser` în DB e exact dimensionat."*

---

### 2.10 Justificare detaliată: Test database strategy — Testcontainers vs. alternative

**De ce Testcontainers Postgres:**
1. **Test pe același DB engine ca producția.** Postgres real într-un container Docker, pornit per test session.
2. **API simplu:**
   ```java
   @Container
   static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
   ```
3. **Spring Boot integration** prin `@ServiceConnection` (Spring Boot 3.1+) — Spring detectează container-ul, configurează `DataSource` automat.
4. **Reusable containers** (`testcontainers.reuse.enable=true`) — container pornit o dată, refolosit între run-uri de teste = ~30s overhead per test session devine ~2s per restart.
5. **Reflectă realitatea producției** — funcții Postgres specifice (`jsonb`, full-text search, etc.) pot fi testate.

**Alternative excluse:**

| Alternativă | Pro | Contra care domină |
|---|---|---|
| **H2 in-memory** | Zero startup time (<1s); zero dependențe externe; rulează în CI fără Docker; are `MODE=PostgreSQL` flag care îmbunătățește compatibilitatea. | **Divergențe concrete:** `jsonb`, `ILIKE`, funcții `array_agg`, edge cases la window functions. Funcții Postgres-specifice nu există sau merg diferit. **Risk concret:** test trece pe H2, fails on production Postgres. La proiecte mature, evitarea acestui anti-pattern e standard. **Acceptabil pentru `test` profile dezvoltare locală rapid (`mvn test -Dspring.profiles.active=test-h2`), nu pentru CI gate.** |
| **HSQLDB / Derby** | Same as H2. | Same problems as H2, plus comunități mai mici. |
| **Postgres local instalat** | Real engine. | **Nu reproductibil în CI** (necesită setup separat). **Conflict cu Postgres din Docker** dev (port 5432). |
| **Postgres Docker rulat manual** | Real engine, predictabil. | **Coordinație manuală** — developer trebuie să-l pornească înainte de teste. Testcontainers automatizează asta. |
| **Mock-uri toate (Mockito pentru repository)** | Zero DB. | **Specifications API se aplică pe `EntityManager`** — nu poți testa logica filtering fără DB real. Mock-uri pentru repository ar fi testat doar layer-ul service izolat — dar tot ai nevoie de un test real pentru repository. |
| **In-memory Postgres (de ex. embedded-postgres)** | Real Postgres, fără Docker. | **Necesită native binaries** download per OS. **Maintenance slab.** Pe Windows e flaky. Testcontainers e mai reliable. |

**Strategy combinată:**
- **Unit tests** (service layer cu mock-uri pe repository) → JUnit + Mockito; **fără DB**, rapid.
- **Repository slice tests** (`@DataJpaTest`) → Testcontainers Postgres; testează queries reale.
- **Integration tests** (`@SpringBootTest`) → Testcontainers Postgres; smoke test full stack.

**Interview defense:** *"Testcontainers e tradeoff-ul corect: ~2s overhead la repository tests pentru încrederea că ce văd la teste e ce văd live. H2 ar fi rapid dar divergent — am preferat un singur dialect (Postgres) peste tot. Pentru unit tests pe service layer, mock-uiesc repository-ul cu Mockito — niciun DB necesar, ms-uri per test."*

---

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

Numerotate **cronologic** după ordinea de implementare (Flyway aplică migrațiile în ordinea version-number; ordinea de creare în plan dictează numerotarea):

```
V1__create_product_table.sql       (Phase 2 — core domain)
V2__create_app_user_table.sql      (Phase 8 — auth)
V3__seed_admin_user.sql            (Phase 8 — auth)
V4__create_exchange_rate_table.sql (Phase 10 — bonus #1)
V5__create_scrape_run_table.sql    (Phase 11 — dashboard)
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

**Particularitate BNR:** XML `<Rate currency="JPY" multiplier="100">2.85</Rate>` — atenție la multiplier (validat în test).

**Exemplu numeric (JPY):** dacă produs are `price = 100 JPY` și BNR publică `rate=2.85 multiplier=100` → `price_ron = 100 × 2.85 / 100 = 2.85 RON` (NU `100 × 2.85 = 285 RON`, eroare clasică).

**Exemplu numeric (USD):** `price = 9.99 USD`, `rate=4.5234 multiplier=1` → `price_ron = 9.99 × 4.5234 / 1 = 45.19 RON`.

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
      # web-scraping.dev este un sandbox public; credențialele sunt afișate vizibil
      # pe pagina /login a site-ului. Default-ele de mai jos vor fi confirmate la
      # primul test de login din task-ul 3 al implementării (vezi Implementation Order).
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
