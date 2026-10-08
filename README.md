# ApiCustomTesting

> A multi-module QA Automation framework built with Java 21 + Spring Boot 3.
> The REST client validates the emulator's business logic by sending HTTP requests and asserting the responses — without any external dependencies or test servers.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [API Reference](#api-reference)
- [How It Works](#how-it-works)
- [Getting Started](#getting-started)
- [Running Tests](#running-tests)
- [Test Reports](#test-reports)
- [Known Limitations](#known-limitations)
- [Project Goals](#project-goals)

---

## Overview

**ApiCustomTesting** is a self-contained integration testing infrastructure for a simulated bank card API.
The project demonstrates a layered multi-module architecture where:

- the **emulator** acts as a real HTTP server with authentication, session management, and mock data storage
- the **REST client** communicates with the emulator using Java's native `HttpClient` (HTTP/2)
- the **test framework** manages the full emulator lifecycle — start, seed data, assert, teardown — within a single JUnit 5 test context

The entire environment runs **in-process** during testing: no Docker, no external servers, no infrastructure setup required.

---

## Architecture

```
ApiCustomTesting/
│
├── domain-objects/                         # Pure domain layer — no Spring dependency
│   └── src/main/java/elya/
│       ├── authentication/
│       │   └── Token.java                  # Auth token entity
│       └── card/
│           ├── BankCard.java               # Core domain model
│           └── constants/
│               ├── CardType.java           # Enum: DEBIT, CREDIT, etc.
│               ├── Currency.java           # Enum: USD, EUR, RUB, etc.
│               ├── Identifiable.java       # Interface for ID-bearing entities
│               ├── Nameable.java           # Interface for named entities
│               ├── GetById.java            # Lookup-by-ID contract
│               └── GetByName.java          # Lookup-by-name contract
│
├── api-contracts/                          # Shared HTTP contracts between emulator and client
│   └── src/main/java/elya/
│       ├── apicontracts/
│       │   ├── IAuthApi.java               # Auth endpoint interface
│       │   ├── IBankCardApi.java           # Bank card CRUD interface
│       │   └── IMockControlApi.java        # Mock management interface
│       ├── constants/
│       │   ├── ApiEndpoints.java           # Centralized URL constants
│       │   └── enums/
│       │       ├── HttpHeaderValues.java   # Bearer, application/json, etc.
│       │       ├── JsonProperty.java       # JSON field name constants
│       │       └── StatusInfo.java         # HTTP status descriptors
│       ├── dto/
│       │   ├── auth/
│       │   │   ├── AuthRequest.java        # Login + password payload
│       │   │   ├── AuthResponse.java       # Token + expiry response
│       │   │   └── AuthResponseData.java   # Inner token data
│       │   └── bankcard/
│       │       ├── BankCardRequest.java    # Single card request
│       │       ├── BankCardListRequest.java # Batch card request
│       │       ├── BankCardResponse.java   # Single card response
│       │       └── BankCardListResponse.java # Paginated card list response
│       ├── enums/responsemodel/
│       │   ├── ApiBankCards.java           # JSON path keys for card responses
│       │   └── AuthToken.java              # JSON path keys for token responses
│       └── ApiEmulatorHttpStatusInfoGenerator.java # HTTP status metadata enrichment
│
├── properties/                             # Spring configuration & credentials
│   └── src/main/java/elya/
│       ├── constants/
│       │   ├── Role.java                   # Enum: ADMIN, QA
│       │   ├── exceptions/
│       │   │   ├── ApiEmulatorConfigurationException.java
│       │   │   └── ExceptionMessage.java
│       │   └── logs/
│       │       ├── ErrorLogs.java
│       │       └── InfoLogs.java
│       └── credentials/
│           ├── ApiEmulatorCredentialsService.java   # Resolves user by role
│           └── ApiEmulatorCredentialsStructure.java # @ConfigurationProperties binding
│   └── src/main/resources/
│       └── application.yml                 # Server port, user credentials, token config, Actuator
│
├── http-support/                           # Shared HTTP infrastructure
│   └── src/main/java/elya/
│       ├── emulator/constants/
│       │   ├── excpetions/
│       │   │   ├── ApiEmulatorException.java
│       │   │   ├── ExceptionMessage.java
│       │   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│       │   │   └── TokenValidationException.java
│       │   ├── formats/
│       │   │   ├── DataTransformer.java    # Generic type conversion
│       │   │   ├── DateTimeConstants.java  # Date/time format patterns
│       │   │   ├── TimeConverter.java      # Instant ↔ LocalDateTime
│       │   │   └── TypeConverter.java      # String ↔ Enum / numeric types
│       │   ├── logs/
│       │   │   ├── ApiErrorLogs.java
│       │   │   ├── ApiInfoLogs.java
│       │   │   └── ApiWarnLogs.java
│       │   └── messages/
│       │       └── ResponseMessages.java   # Reusable response strings
│       ├── emulator/objects/
│       │   └── TokenRecord.java            # Session token storage entry
│       ├── emulator/tokens/
│       │   ├── TokenProvider.java          # Token generation interface
│       │   ├── JwtTokenProvider.java       # JWT implementation (JJWT 0.13)
│       │   └── UuidTokenProvider.java      # UUID-based simple token
│       └── restclient/
│           ├── constants/logs/
│           │   ├── ErrorLogs.java
│           │   └── ExceptionMessage.java
│           ├── exceptions/
│           │   ├── RestClientException.java      # Base unchecked client failure
│           │   └── ApiHttpStatusException.java   # Non-2xx response: carries status + body
│           └── objects/response/
│               └── RestClientApiResponse.java  # Wrapper for HTTP response + status info
│
├── api-emulator-autoconfigure/             # Spring Boot Auto-configuration
│   └── src/main/java/elya/
│       ├── ApiEmulatorAutoConfiguration.java   # @ConditionalOnProperty / @ConditionalOnClass
│       └── ApiTokenConfig.java                 # Token expiry & issuer @ConfigurationProperties
│   └── src/main/resources/META-INF/spring/
│       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
├── api-emulator/                           # Spring Boot emulator application
│   └── src/main/java/elya/
│       ├── ApiEmulator.java                # @SpringBootApplication entry point
│       ├── ApiEmulatorController.java      # @RestController — all endpoints
│       ├── ApiEmulatorRunner.java          # Programmatic Spring context start/stop
│       ├── repository/
│       │   ├── MockRepository.java         # ConcurrentHashMap + JSON file persistence
│       │   └── SessionRepository.java      # ConcurrentHashMap + JSON file persistence
│       └── services/
│           ├── AuthenticationService.java  # Credential validation + token issuance
│           ├── MockService.java            # CRUD operations on mock card data
│           └── TokenManagerService.java    # Token lifecycle: generate, validate, expire
│   └── src/main/resources/
│       └── application.properties          # Auto-configuration exclusions (JPA, Mongo, Security OAuth2)
│
├── rest-client/                            # HTTP client library
│   └── src/main/java/elya/
│       ├── RestClientApiHelper.java        # Jackson-based JSON utility (@UtilityClass)
│       ├── api/
│       │   ├── RestClientApi.java          # Facade aggregating all sub-clients
│       │   ├── RestClientApiEngine.java    # Core engine: Java HttpClient (HTTP/2)
│       │   ├── AuthClient.java             # POST /auth/token
│       │   ├── BankCardClient.java         # GET/POST/DELETE /bank-cards/data
│       │   └── MockClient.java             # Mock data seeding & cleanup
│       └── interfaces/
│           ├── IRestClientApi.java         # Low-level HTTP methods contract
│           ├── IRestClientApiEngine.java   # Engine configuration contract
│           ├── IReadOnlyClient.java        # GET operations
│           ├── IWriteClient.java           # POST operations
│           └── IRemoveClient.java          # DELETE operations
│
└── test-framework/                         # Integration test runner
    └── src/main/java/elya/
    │   ├── annotations/
    │   │   └── ApiIntegrationTest.java     # Meta-annotation: @SpringBootTest + @ExtendWith
    │   └── engine/services/emulator/
    │       ├── EmulatorLifecycleManager.java # @PostConstruct start / @PreDestroy stop
    │       └── config/
    │           └── EmulatorTestConfig.java  # @Configuration: beans for test context
    └── src/test/java/
        ├── integration/                     # *IT — run by failsafe during `mvn verify`
        │   ├── AbstractApiTest.java         # @BeforeEach start emulator / @AfterEach stop
        │   ├── auth/
        │   │   └── PostTokenIT.java         # Auth flow tests
        │   └── cards/
        │       ├── GetBankCardsDataIT.java     # GET list — happy path + edge cases
        │       ├── GetBankCardsDataByIdIT.java # GET by ID — valid / not found
        │       ├── PostBankCardsDataIT.java    # POST — validation, duplicates, limits
        │       ├── DeleteBankCardsDataIT.java  # DELETE all — idempotency
        │       └── DeleteBankCardsDataByIdIT.java # DELETE by ID — valid / not found
        └── unit/                            # *Tests — run by surefire during `mvn test`
            └── engine/sevices/emulator/
                └── EmulatorLifecycleManagerTests.java
```

---

## Tech Stack

| Layer | Technology | Details |
|---|---|---|
| Language | Java 21 | Records, pattern matching for `instanceof` |
| Framework | Spring Boot 3.5.7 | Web, Actuator, Validation, Auto-configuration |
| HTTP Client | Java `HttpClient` (HTTP/2) | Native JDK client, `Duration` timeouts |
| HTTP Server | Spring MVC (`@RestController`) | Embedded Tomcat via spring-boot-starter-web |
| Security | Spring Security Crypto | `PasswordEncoder` / `NoOpPasswordEncoder` |
| Token Auth | JJWT 0.13 + UUID | `JwtTokenProvider`, `UuidTokenProvider` |
| Serialization | Jackson (`jackson-databind`, `jackson-annotations`) | ObjectMapper, JsonNode, TypeReference |
| Validation | Jakarta Validation API + Spring Validation | `@Valid`, `@NotNull`, `@NotBlank` on DTOs |
| API Docs | SpringDoc OpenAPI 2.8.5 | Swagger UI at `/swagger-ui.html` |
| Testing | JUnit 5 (`junit-jupiter-api`, `junit-jupiter-params`) | Parameterized tests, extensions |
| Test Context | Spring Boot Test (`@SpringBootTest`) | Full context, `SpringExtension` |
| Test Reporting | Allure 2.33.0 + AspectJ 1.9.25.1 | `@Step`, `@Feature`, `@Story` annotations |
| Concurrency | `ConcurrentHashMap` | Thread-safe in-memory storage |
| Logging | SLF4J + Logback (via Spring Boot) | `@Slf4j` (Lombok) |
| Code Generation | Lombok 1.18.30 | `@Builder`, `@Slf4j`, `@RequiredArgsConstructor`, `@UtilityClass` |
| Configuration | Spring `@ConfigurationProperties` + `@Value` | Type-safe property binding |
| Persistence | JSON file-based (Jackson) | Survives emulator restarts within a test session |
| Build | Maven 3.9+ (multi-module) | `surefire 3.5.5` (unit), `failsafe 3.5.5` (integration), `dependency 3.10.0` |
| AOP | AspectJ Weaver | Required by Allure for `@Step` interception |

---

## API Reference

All endpoints require a `Bearer` token in the `Authorization` header (except `/auth/token`).

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/elya-bank/auth/token` | ❌ | Authenticate and receive a session token |
| `POST` | `/api/v1/elya-bank/bank-cards/data` | ✅ | Seed mock bank card data into the session |
| `GET` | `/api/v1/elya-bank/bank-cards/data` | ✅ | Retrieve all cards for the current session |
| `GET` | `/api/v1/elya-bank/bank-cards/data/{cardId}` | ✅ | Retrieve a specific card by its ID |
| `DELETE` | `/api/v1/elya-bank/bank-cards/data` | ✅ | Clear all mock data for the session |
| `DELETE` | `/api/v1/elya-bank/bank-cards/data/{cardId}` | ✅ | Delete a specific card by its ID |

**Default credentials** (configured in `application.yml`):

| Role | Login | Password |
|---|---|---|
| `ADMIN` | `admin` | `admin` |
| `QA` | `qa` | `qa` |

**Token lifetime:** 3600 seconds

---

## How It Works

```
Test JVM
│
├─► @BeforeEach  AbstractApiTest.setUp()
│       │
│       └─► EmulatorLifecycleManager.start(AuthRequest)
│               ├── ApiEmulatorRunner.start()          # Starts Spring Boot context
│               ├── Actuator /health polling           # Waits until emulator is UP
│               ├── POST /auth/token                   # Authenticates and stores session token
│               └── Injects baseUrl into RestClientApiEngine
│
├─► Test method executes
│       ├── emulator.addBankCards(token, cards)        # POST /bank-cards/data
│       ├── clientApi.getApiBankCards(token)           # GET /bank-cards/data
│       └── assertions on the returned domain objects
│
└─► @AfterEach   AbstractApiTest.tearDown()
        └─► EmulatorLifecycleManager.stop()
                ├── Clears SessionRepository + MockRepository
                └── ApiEmulatorRunner.stop()           # Closes Spring context
```

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+

### Build all modules

```bash
mvn clean install
```

### Run the emulator as a standalone server

```bash
mvn spring-boot:run -pl api-emulator
```

Swagger UI → `http://localhost:8080/swagger-ui.html`
Actuator health → `http://localhost:8080/actuator/health`

### Run with a specific Spring profile

```bash
mvn spring-boot:run -pl api-emulator -Dspring-boot.run.profiles=dev
```

---

## Running Tests

Unit and integration tests are separated by Maven phase, using the standard
surefire / failsafe split.

| Command | What runs | Plugin | Class naming |
|---|---|---|---|
| `mvn test` | Unit tests only — fast, no server started | surefire | `*Tests` |
| `mvn verify` | Unit tests **and** integration tests against the emulator | surefire + failsafe | `*Tests` + `*IT` |

```bash
# Fast feedback loop — unit tests only
mvn test

# Full suite, including integration tests against the live emulator
mvn verify

# Single integration test class
mvn verify -Dit.test=PostTokenIT -pl test-framework -am

# Single unit test class
mvn test -Dtest=RestClientApiEngineTests -pl rest-client -am
```

Integration tests start a real Spring Boot emulator per test method, so
`mvn verify` is significantly slower than `mvn test`. Use `mvn test` while
developing and `mvn verify` before pushing.

---

## Test Reports

Allure results are written to `target/allure-results` of each module
(configured in `allure.properties`).

```bash
# Run the full suite and generate Allure results
mvn verify

# Start the Allure report server
allure serve test-framework/target/allure-results
```

The report includes step-level breakdowns (`@Step`), request/response details, and test categorization by `@Feature` / `@Story`.

---

## Known Limitations

| # | Issue | Impact |
|---|---|---|
| 1 | **No persistence across JVM restarts** | All mock data is in-memory + local JSON files; data is lost on full restart |
| 2 | **`NoOpPasswordEncoder` in auth** | Passwords stored and compared as plain text — intentional for test simplicity, not production-safe |
| 3 | **Single-node only** | `ConcurrentHashMap`-based storage does not support distributed test runs |
| 4 | **Fixed emulator port** | `EmulatorLifecycleManager` binds port 8080; parallel runs would also collide on the shared JSON storage paths |
| 5 | **Token validation is stateful** | Even with the JWT provider active, tokens are validated against `SessionRepository`; the signature is not verified, so there is no true stateless mode |
| 6 | **No token refresh** | Expired tokens require full re-authentication; no refresh flow implemented |
| 7 | **Auto-configuration exclusions are broad** | JPA, MongoDB, Liquibase, and OAuth2 are excluded globally via `application.properties` |
| 8 | **Full Spring context per test method** | `AbstractApiTest` starts and stops the emulator in `@BeforeEach`/`@AfterEach` — maximum isolation, at the cost of run time |

---

## Project Goals

This project was built to practice and demonstrate:

- **Multi-module Maven project** design and inter-module dependency management
- **Spring Boot Auto-configuration** with `@ConditionalOnProperty` / `@ConditionalOnClass`
- **Custom meta-annotations** (`@ApiIntegrationTest`) composing multiple Spring test annotations
- **Programmatic Spring context management** (`ApiEmulatorRunner`) for in-process server lifecycle
- **Interface-driven design** separating HTTP contracts from implementation
- **Allure + AspectJ** integration for rich test reporting with `@Step` interception
- **Health-check-based readiness polling** before test execution begins
- **Unit / integration test separation** by Maven phase (surefire + failsafe)