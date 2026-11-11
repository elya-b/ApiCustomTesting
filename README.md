# ApiCustomTesting

A pet project with two parts:

- **api-emulator**, a Spring Boot service that emulates a small bank API: token authentication, bank card endpoints, RFC 9457 errors and an OpenAPI contract.
- **A test automation framework** on JUnit 5 that tests the emulator as a black box over HTTP. It uses its own client layer on Java's `HttpClient` instead of RestAssured.

Everything runs in one JVM: the integration tests start the emulator themselves, so no Docker or external server is needed.

[Quick Start](#quick-start) | [Architecture](#architecture) | [Writing a Test](#writing-a-test) | [Emulator Features](#emulator-features) | [API Reference](#api-reference) | [Configuration](#configuration) | [Running the Emulator](#running-the-emulator) | [Running Tests](#running-tests) | [Test Reports](#test-reports) | [Design Decisions](#design-decisions) | [Known Limitations](#known-limitations)

## Quick Start

You need JDK 21, available through `JAVA_HOME` or the `PATH`. Maven is not required: the project includes the Maven Wrapper. Run the commands from the project root. On Windows use `mvnw.cmd` in cmd, `.\mvnw.cmd` in PowerShell, or `./mvnw` in Git Bash.

```bash
./mvnw verify                            # build all modules and run all tests
./mvnw allure:serve -pl test-framework   # open the Allure report in the browser
```

The first run downloads Maven and the dependencies. Stop the report server with Ctrl+C.

## Architecture

The build has five Maven modules.

| Module | Purpose | Main classes |
|---|---|---|
| `domain-objects` | Domain model used by the tests and the client | `BankCard`, `Token`, `CardType`, `Currency` |
| `api-contracts` | The API contract shared by the server and the client: endpoints, DTOs, contract interfaces, OpenAPI annotations | `ApiEndpoints`, `IAuthApi`, `IBankCardApi`, `IMockControlApi`, `AuthResponse`, `BankCardRequest` |
| `api-emulator` | The Spring Boot service | `ApiEmulatorController`, `AuthenticationService`, `MockService`, `GlobalExceptionHandler`, `JsonSnapshotStore` |
| `rest-client` | HTTP client: an engine on `java.net.http.HttpClient` and typed clients for each part of the API | `RestClientApiEngine`, `AuthClient`, `BankCardClient`, `MockClient`, `RestClientApi` |
| `test-framework` | Test infrastructure and the API integration tests | `EmulatorLifecycleManager`, `EmulatorSession`, `EmulatorTestConfig`, `@ApiIntegrationTest`, `AbstractApiTest` |

```mermaid
flowchart TD
    TF[test-framework] --> EM[api-emulator]
    TF --> RC[rest-client]
    EM --> AC[api-contracts]
    RC --> AC
    AC --> DO[domain-objects]
```

The arrows are compile dependencies. `domain-objects` is also declared directly by the modules that use its classes.

Dependency rules:

1. `api-contracts` and `domain-objects` do not depend on Spring Web. The server (`AuthenticationService`, `MockService`) and the client (`AuthClient`, `BankCardClient`, `MockClient`) implement the same contract interfaces, so a change to the contract breaks compilation on both sides.
2. `rest-client` does not depend on `api-emulator`. It knows only the contract and can call any server that implements it.
3. The integration tests check behaviour only through HTTP. They start the emulator in the same JVM but never read its beans or repositories.

### Integration test lifecycle

Every integration test gets its own `EmulatorSession`: a fresh emulator on a free port, a client bound to that emulator and a storage directory of its own. Tests share no state and do not depend on their order, so test classes run in parallel.

```mermaid
sequenceDiagram
    participant T as Test
    participant M as EmulatorLifecycleManager
    participant E as api-emulator
    participant C as rest-client
    T->>M: start() in @BeforeEach
    M->>E: start on a free port with its own storage directory
    loop until UP, at most 30 s
        M->>E: GET /actuator/health
    end
    M->>E: POST /auth/token as admin
    E-->>M: session token
    M-->>T: EmulatorSession with URL, token and client
    T->>C: seed, read or delete cards
    C->>E: HTTP request with Bearer token
    E-->>C: JSON or RFC 9457 problem detail
    C-->>T: domain object or exception
    T->>E: session.close() in @AfterEach
```

### Client failure model

`RestClientApiEngine` keeps three kinds of failure apart, so a negative test can assert on exactly the one it expects.

```mermaid
flowchart TD
    A[Send request] --> B{Response received?}
    B -- No --> X[ApiTransportException]
    B -- Yes --> C{Status 2xx?}
    C -- No --> Y["ApiHttpStatusException<br/>statusCode and responseBody"]
    C -- Yes --> D{Body is JSON?}
    D -- No --> Z[RestClientException]
    D -- Yes --> OK[JsonNode mapped to a DTO]
```

`ApiHttpStatusException` carries the real status code, including non-standard ones such as 520. Both specific exceptions extend `RestClientException`, so code that does not care about the reason can catch the base type.

## Writing a Test

A shortened version of [`GetBankCardsDataByIdIT`](test-framework/src/test/java/integration/cards/GetBankCardsDataByIdIT.java):

```java
@Epic("Bank Card API")
@Feature("GET /bank-cards/data/{id}")
class GetBankCardsDataByIdIT extends AbstractApiTest {

    @Test
    @Story("Card found")
    void getApiBankCardById_ShouldReturnSpecificCard_WithAllParameters() {
        String token = emulator.getAuthToken();
        List<BankCard> seededCards = emulator.addBankCards(token, List.of(
                BankCard.builder(4444555566667777L, CardType.DEBIT, Currency.USD).build(),
                BankCard.builder(1111222233334444L, CardType.CREDIT, Currency.EUR).build()));
        BankCard target = seededCards.getFirst();

        BankCard actualCard = clientApi.getApiBankCardById(token, target.getCardId());
        attachJson("Response", actualCard);

        verify("All fields match the seeded card", () ->
                assertThat(actualCard).usingRecursiveComparison().isEqualTo(target));
    }

    @Test
    @Story("Card not found")
    void getApiBankCardById_ShouldReturn404_WhenCardIdDoesNotExist() {
        String token = emulator.getAuthToken();

        var exception = assertThrows(ApiHttpStatusException.class,
                () -> clientApi.getApiBankCardById(token, 999999L));

        verify("A non-existent card ID is answered with 404", () ->
                assertEquals(404, exception.getStatusCode()));
    }
}
```

- `AbstractApiTest` starts a new `EmulatorSession` and logs in as admin before each test, and closes it afterwards. `emulator` is that session, and `emulator.getAuthToken()` returns its token.
- `emulator.addBankCards(...)` seeds test data through the API, and `clientApi` is the typed client under test, bound to the test's own emulator.
- `verify(...)` runs the assertions as a named Allure step and attaches the failure message to the report. `attachJson(...)` adds a request or response body.
- A negative test asserts on `ApiHttpStatusException`, which holds the status code and the response body.

## Tech Stack

| Area | Tools |
|---|---|
| Language and build | Java 21, Maven (wrapper included), surefire and failsafe 3.5.5 |
| Service | Spring Boot 3.5.7: Web, Validation, Actuator; springdoc-openapi 2.8.5; jjwt 0.13 |
| HTTP client | `java.net.http.HttpClient` (HTTP/2), Jackson |
| Testing | JUnit 5, AssertJ, Mockito, Spring Boot Test (`MockMvc`, `@WebMvcTest`, `ApplicationContextRunner`) |
| Reporting | Allure 2.33 with AspectJ |
| Other | Lombok |

## Emulator Features

- `POST /auth/token` checks the login and password against the configured users and returns a session token with its lifetime and expiry time.
- Each session token has its own list of cards, and one session never sees or changes the cards of another.
- `POST /bank-cards/data` appends cards to the existing list. Each new card gets the highest existing `cardId` plus one. Concurrent requests to one session lose no card and never repeat a `cardId`.
- Every card endpoint checks the token. A missing header, a scheme other than `Bearer`, an unknown token and an expired token all end with `401`, and an expired session is removed.
- Required fields, the 16-digit card number, enum values and JSON syntax are validated before the request reaches the service.
- Every error, including Spring MVC ones such as `405` and `415`, is an RFC 9457 problem detail. Unexpected failures return a generic `500` message without internal details.
- Error details never repeat the rejected value, tokens are masked in the logs, and passwords are left out of `toString()`.
- Sessions and cards are kept in memory and written to JSON files. The retention setting decides whether the data survives a restart.
- The OpenAPI document is at `/v3/api-docs` and Swagger UI at `/swagger-ui/index.html`. Actuator exposes `health`, `info`, `env` and `loggers`.

## API Reference

Base path: `/api/v1/elya-bank`. Every endpoint except `/auth/token` needs an `Authorization: Bearer <token>` header.

| Method | Path | Description | Errors |
|---|---|---|---|
| `POST` | `/auth/token` | Check the login and password and issue a session token | `400`, `401` |
| `POST` | `/bank-cards/data` | Add cards to the session and return all its cards | `400`, `401` |
| `GET` | `/bank-cards/data` | Get all cards of the session (an empty list if there are none) | `401` |
| `GET` | `/bank-cards/data/{cardId}` | Get one card | `400`, `401`, `404` |
| `DELETE` | `/bank-cards/data` | Remove all cards of the session | `401` |
| `DELETE` | `/bank-cards/data/{cardId}` | Remove one card and return its `cardId` | `400`, `401`, `404` |

### Test users

The emulator ships with two users for local runs, defined in [`emulator-credentials.yml`](api-emulator/src/main/resources/emulator-credentials.yml): `admin`/`admin` (role `ADMIN`) and `qa`/`qa` (role `QA`). They are test fixtures, not secrets. The test framework imports the same file, so the tests log in with the same users. Roles are loaded from the configuration but do not restrict access yet: any valid user can call every endpoint.

### Getting a token

```http
POST /api/v1/elya-bank/auth/token
Content-Type: application/json

{"login": "admin", "password": "admin"}
```

```json
{
  "Data": {
    "token": "a9cf409c-5db8-4d54-ab3d-5afa81d53652",
    "ttl": "3600",
    "expires": "2026-03-07T23:00:00Z",
    "issuer": "api-emulator"
  },
  "Success": true,
  "Message": "Success"
}
```

`ttl` is the lifetime in seconds, and `expires` is an ISO-8601 UTC instant with second precision.

Tokens are UUIDs. `JwtTokenProvider` implements the same `TokenProvider` interface, but the provider is chosen in code, not in the configuration: `UuidTokenProvider` is marked `@Primary`, and switching to JWT means moving that annotation.

### Cards

| Field | Type | Rules |
|---|---|---|
| `cardId` | number | Response only, set by the emulator |
| `cardNumber` | number | Required, exactly 16 digits |
| `cardType` | string | Required: `debit` or `credit` |
| `currency` | string | Required: `USD`, `EUR` or `JPY` |
| `cardStatus` | boolean | Optional |
| `balance` | number | Optional |

```http
POST /api/v1/elya-bank/bank-cards/data
Authorization: Bearer a9cf409c-5db8-4d54-ab3d-5afa81d53652
Content-Type: application/json

{"cards": [{"cardNumber": 4444333322221111, "cardType": "debit", "currency": "USD", "cardStatus": true, "balance": 9999.99}]}
```

```json
{
  "response": {
    "size": 1,
    "cards": [
      {"cardId": 1, "cardNumber": 4444333322221111, "cardType": "debit", "cardStatus": true, "currency": "USD", "balance": 9999.99}
    ]
  }
}
```

### Errors

Errors are RFC 9457 problem details (`application/problem+json`). A `401` also has the `WWW-Authenticate: Bearer` header.

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Token is invalid, expired or missing",
  "instance": "/api/v1/elya-bank/bank-cards/data"
}
```

| Status | When |
|---|---|
| `400` | Missing or malformed body, a field with a wrong type or value, a failed validation rule, a non-numeric `cardId` |
| `401` | Wrong login or password; missing, malformed, unknown or expired token |
| `404` | No card with this `cardId` in the session, or an unknown path |
| `405`, `415` | Unsupported HTTP method or `Content-Type` |
| `500` | Unexpected failure; the response has a generic message only |

## Configuration

| Property | Default | Meaning |
|---|---|---|
| `server.port` | `8080` | Port of the standalone emulator. The tests start it on a free port. |
| `api.credentials.users` | from `emulator-credentials.yml` | Users with `login`, `password` and `role`. The emulator does not start with an empty list. |
| `api.credentials.token.expiration` | `1h` | Token lifetime. A plain number means seconds; `30m` and `1h` also work. Must be positive. |
| `api.credentials.token.issuer` | `api-emulator` | Value of `issuer` in the token response. Can be set with the `TOKEN_ISSUER` environment variable. |
| `api.emulator.storage.sessions.path` | `session.json` | File that stores the sessions |
| `api.emulator.storage.mocks.path` | `mock_response.json` | File that stores the cards |
| `api.emulator.storage.*.retention` | `process` | `process`: data lives while the emulator runs and the file is deleted on shutdown. `persistent`: data is kept across restarts. |

Invalid token settings stop the emulator at startup instead of producing tokens that expire at once. Each change rewrites the storage file through a temporary file, so a crash cannot leave a broken file behind. With `persistent` retention the session tokens are kept on disk in plain text, so use it only for manual work.

## Running the Emulator

```bash
# Build all modules and install them into the local Maven repository
./mvnw install -DskipTests

# Start the emulator on port 8080
./mvnw spring-boot:run -pl api-emulator
```

`install` comes first because `-pl api-emulator` takes the other modules from the local repository.

Check that it works:

```bash
curl http://localhost:8080/actuator/health

curl -X POST http://localhost:8080/api/v1/elya-bank/auth/token \
  -H "Content-Type: application/json" \
  -d '{"login": "admin", "password": "admin"}'
```

## Running Tests

| Level | Where | Naming | Maven phase | How |
|---|---|---|---|---|
| Unit | all modules | `*Tests` | `test` (surefire) | JUnit 5, Mockito, AssertJ, `ApplicationContextRunner` for configuration |
| Web layer | `api-emulator` | `*Tests` | `test` (surefire) | `@WebMvcTest` with `MockMvc` |
| Emulator integration and OpenAPI contract | `api-emulator` | `*Tests` | `test` (surefire) | full application context with `MockMvc`; the contract tests check `/v3/api-docs` against the real behaviour |
| API integration | `test-framework` | `*IT` | `verify` (failsafe) | a real emulator over HTTP through `rest-client`, with Allure steps and attachments |

```bash
# Unit and emulator tests
./mvnw test

# Everything, including the API integration tests
./mvnw verify

# One integration test class (after ./mvnw install -DskipTests)
./mvnw verify -pl test-framework -Dit.test=PostTokenIT

# One unit test class (after ./mvnw install -DskipTests)
./mvnw test -pl rest-client -Dtest=RestClientApiEngineTests
```

Integration test classes run in parallel, three at a time, and the methods of one class run one after another. The settings are in [`junit-platform.properties`](test-framework/src/test/resources/junit-platform.properties). To compare with a sequential run:

```bash
# after ./mvnw install -DskipTests
./mvnw verify -pl test-framework -Djunit.jupiter.execution.parallel.enabled=false
```

Several emulators can start at the same time in one JVM, and Spring Boot re-initializes the JVM-wide Logback configuration on every start. The failsafe configuration turns that off, so the logs of emulators that start together stay intact. When the tests are started from the IDE, the log lines of parallel tests can interleave; each line shows its thread.

The Mockito agent is passed to the test JVM with `-javaagent` instead of being attached at runtime, as JEP 451 and the Mockito documentation recommend for Java 21.

## Test Reports

The tests of the `test-framework` module, both unit and integration, write Allure results to `test-framework/target/allure-results`. The tests are grouped by `@Epic`, `@Feature` and `@Story` and use `@Severity` and the project's own `@Priority`. Request and response bodies are attached to the steps.

```bash
./mvnw verify
./mvnw allure:serve -pl test-framework
```

`allure:serve` builds the report and opens it in the browser. It runs the Allure command line as a separate process, which needs `JAVA_HOME` to point to a JDK. The server keeps running until you stop it with Ctrl+C.

## Design Decisions

| Decision | Why | Trade-off |
|---|---|---|
| Own HTTP client instead of RestAssured | Transport errors, non-2xx statuses and non-JSON bodies are separate exceptions, and nothing is converted or retried behind the test's back | More code to maintain and no fluent given/when/then DSL |
| One contract module for the server and the client | Both implement the same interfaces with the same DTOs, so a contract change breaks compilation on both sides | The client depends on Java types; checking the published OpenAPI document is left to the contract tests |
| Black-box tests with a fresh emulator per test | Tests see only HTTP, as a real consumer would, and share no state | A Spring context starts for every test, which makes each test slower |
| A session per test instead of shared singletons | The emulator, its client and its storage directory belong to one test, so JUnit runs test classes in parallel without locks | Logback is global to the JVM, so Spring Boot's logging re-initialization is turned off for the integration tests |
| Emulator started inside the test JVM | One command runs everything, with no Docker or deployed environment | It does not cover what a deployed service adds: real network, real environment configuration |
| Explicit Spring contexts | The emulator scans only its own packages, and `EmulatorTestConfig` lists the test beans without component scanning, so the two contexts never mix | A new test-side bean has to be registered by hand |
| RFC 9457 errors and OpenAPI contract tests | Clients get one error format, and Swagger UI cannot drift from the code unnoticed | Every API change also needs a change in the contract tests |

## Known Limitations

- Security is minimal on purpose: passwords are compared as plain text (`NoOpPasswordEncoder`), and roles do not restrict access. This is acceptable for a test emulator, not for a real service.
- Token validation is stateful: a token is valid only while its session is in the session store, and the JWT signature is not checked.
- Only test classes run in parallel; the methods of one class run one after another. Each test still starts a full emulator.
- There is no CI pipeline yet. The next step is a GitHub Actions workflow that runs `./mvnw verify`.
- `JwtTokenProviderTests` waits for token expiry with `Thread.sleep`. Injecting a `java.time.Clock` would make the test instant and deterministic.
- `ApiEmulatorRunnerTests` uses the fixed port 8081 and fails if that port is busy. It should pick a free port instead.
