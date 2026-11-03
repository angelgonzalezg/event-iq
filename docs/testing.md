# EventIQ - Testing

How EventIQ is tested. Each layer has one tool, so every test is fast, needs nothing that CI lacks, and fails for one reason. New logic and new endpoints arrive with their tests in the same pull request: it is part of the [Definition of Done](ways-of-working.md#definition-of-done). The reasoning behind these choices is in [ADR-009](adr/009-layered-testing-strategy.md).

---

## What to test, and with what

| Layer | Tool | Runs with | Reference |
|---|---|---|---|
| Business rules in services | JUnit 5 + Mockito, without Spring | `./mvnw test` | [Services](#services) |
| Small classes with no dependencies (converters, mappers) | JUnit 5 | `./mvnw test` | `events/RegistrationStatusConverterTests` |
| View and API controllers | `@WebMvcTest` + `@MockitoBean` | `./mvnw test` | `config/HomeControllerTests`, [Testing views](view-conventions.md#testing-views) |
| Security rules | `spring-security-test` | `./mvnw test` | [Security](#security) |
| Repositories: custom queries and schema constraints | `@DataJpaTest` + Testcontainers with Oracle Database Free | `./mvnw verify` | `events/RegistrationRepositoryIT` |
| Clients of Gemini and of the analytics service | A mocked `ChatModel`; `MockRestServiceServer` | `./mvnw test` | [External services](#external-services) |
| Analytics service | pytest + FastAPI's `TestClient` | `uv run pytest` | `analytics-service/tests/test_health.py` |
| The running API, end to end | Postman, with `pm.test` assertions | By hand | [Postman](#postman) |

JUnit 5, Mockito, AssertJ, MockMvc and `spring-security-test` come with the Spring Boot test starters in `pom.xml`. Testcontainers is `spring-boot-testcontainers` plus `testcontainers-oracle-free`. Spring Boot manages all of their versions.

---

## Running the tests

```bash
./mvnw test                      # *Tests: unit and controller tests, in seconds, no Docker needed
./mvnw verify                    # also *IT: repository tests against Oracle, needs Docker running
cd analytics-service && uv run pytest
```

- To run one class: `./mvnw test -Dtest=RegistrationStatusConverterTests` or `./mvnw verify -Dit.test=RegistrationRepositoryIT`.
- The first `./mvnw verify` downloads the Oracle test image (`gvenzl/oracle-free:23.26.3-slim-faststart`), which takes a few minutes; later runs reuse it.
- In CI, the `java` check runs `./mvnw -B verify` and the `python` check runs `uv run pytest`. Both checks are required to merge into `main`.
- Before opening a pull request, run `./mvnw verify`, and `uv run pytest` if you changed the analytics service.

---

## Conventions

- **Location:** tests mirror the package of the code they test, in `src/test/java/com/eventiq/<module>/`; analytics service tests go in `analytics-service/tests/`. `CODEOWNERS` gives each module's test folder to the module's owner.
- **Names:** unit tests end in `Tests`; integration tests end in `IT`.
- **Method names describe the behavior:** `rejectsRegistrationWhenEventIsFull`, `test_health_answers_503_when_the_database_is_down`.
- **One behavior per test**, arranged as given / when / then. Assert results and effects with AssertJ; checking that a mock was called is not enough on its own.
- **No external services:** no test calls Gemini, the analytics service or your local Oracle container. CI has none of them, which is also why `EventIqApplicationTests.contextLoads` is disabled.
- **Each test builds its own data.** Never rely on `seed_data.py` or on rows that happen to be in a database.

---

## Services

A service test creates the service with mocked repositories and runs without Spring, so it takes milliseconds. Test each business rule and its edge cases:

```java
@ExtendWith(MockitoExtension.class)
class RegistrationServiceTests {

    @Mock
    private EventRepository events;

    @Mock
    private RegistrationRepository registrations;

    @InjectMocks
    private RegistrationService service;

    @Test
    void rejectsRegistrationWhenEventIsFull() {
        Event event = new Event("Java Day", null, LocalDate.now().plusDays(7), "Online", 2);
        when(this.events.findById(1L)).thenReturn(Optional.of(event));
        when(this.registrations.countByEventEventIdAndStatus(1L, RegistrationStatus.CONFIRMED)).thenReturn(2L);

        assertThatThrownBy(() -> this.service.register(1L, 10L)).isInstanceOf(EventFullException.class);
        verify(this.registrations, never()).save(any());
    }
}
```

`RegistrationService`, `EventFullException` and the count query are illustrative; the capacity story (#26) defines the real ones. The rules that need tests first are registration capacity (#26) and the payment status lifecycle (#39).

---

## Controllers

`@WebMvcTest` starts only the web layer, and `@MockitoBean` replaces the services. View controllers follow [Testing views](view-conventions.md#testing-views), which also shows how to test the `serviceWarning` alert when an external service fails.

REST controllers use the same pattern: assert the status code and the JSON with `jsonPath`, including validation errors (400) and missing resources (404). `POST`, `PUT` and `DELETE` requests need `.with(csrf())`, because these tests run with Spring Boot's default security, not the application's.

---

## Security

Security tests import the real configuration and do not use `@WithMockUser`, so they check the rules described in [ADR-004](adr/004-form-login-for-views-jwt-for-api.md). Use test values for the secrets:

```java
@WebMvcTest(EventApiController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = { "JWT_SECRET=a-test-secret-of-at-least-32-characters", "ADMIN_PASSWORD=test" })
class ApiSecurityTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EventService eventService;

    @Test
    void rejectsRequestsWithoutAToken() throws Exception {
        this.mvc.perform(get("/api/events")).andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsAValidToken() throws Exception {
        this.mvc.perform(get("/api/events").with(jwt())).andExpect(status().isOk());
    }
}
```

`SecurityConfig` stands for the configuration that the security story (#29) creates. Cover at least:

- `/api/**` without a token answers 401, and with `.with(jwt())` it answers 200.
- `POST /api/auth/login` with valid credentials returns a token, and with invalid ones answers 401.
- A view without a session redirects to `/login`, while `/login`, `/css/**` and `/error` answer without one.
- A view form submitted without `.with(csrf())` answers 403.

---

## Repositories

Repository tests run against a real Oracle Database Free, never H2: the schema relies on Oracle types, regular expressions and constraints that an in-memory database would not reproduce (ADR-009). `OracleTestDatabase` starts a disposable Oracle container with Testcontainers and creates the tables with `db/schema.sql`. Every test class that imports it shares the same container:

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OracleTestDatabase.class)
class RegistrationRepositoryIT {

    @Autowired
    private RegistrationRepository registrations;

    // this.event and this.attendee are saved in @BeforeEach; see the full class

    @Test
    void anAttendeeRegistersOnlyOncePerEvent() {
        this.registrations.saveAndFlush(new Registration(this.event, this.attendee));

        assertThatThrownBy(() -> this.registrations.saveAndFlush(new Registration(this.event, this.attendee)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

- **What deserves an IT:** custom queries (`@Query`, native SQL, the report aggregations) and the constraints in `db/schema.sql`. Methods that Spring Data derives from their name (`findBy…`) do not need one.
- **Isolation:** each test runs in a transaction that is rolled back at the end, so tests do not see each other's rows. Use `saveAndFlush`, so the insert reaches Oracle and its constraints run inside the test.
- **The image:** `gvenzl/oracle-free:23.26.3-slim-faststart`, the image the Testcontainers Oracle Free module is built for. It runs the same Oracle version as the application's container, natively on `arm64` and `amd64`, and starts faster. The tag is pinned in `OracleTestDatabase` (ADR-008).
- **The schema:** `db/schema.sql` is a SQL*Plus script, so `OracleTestDatabase` runs it with SQL*Plus inside the container, the same way the README does. A change to the schema is therefore tested by every IT.

---

## External services

**Gemini.** Mock `ChatModel`, not `ChatClient`, whose chained API is awkward to mock, and build the service with `ChatClient.builder(chatModel)`. The test checks what the code controls: the prompt that retrieval builds.

```java
@ExtendWith(MockitoExtension.class)
class AssistantServiceTests {

    @Mock
    private ChatModel chatModel;

    @Mock
    private EventRepository eventRepository;

    @Test
    void promptIncludesUpcomingEventsAndTheQuestion() {
        when(this.eventRepository.findByEventDateGreaterThanEqualOrderByEventDateAsc(any()))
            .thenReturn(List.of(new Event("Java Day", null, LocalDate.now().plusDays(7), "Online", 120)));
        when(this.chatModel.call(any(Prompt.class)))
            .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Java Day is next week.")))));

        AssistantService service = new AssistantService(ChatClient.builder(this.chatModel), this.eventRepository);

        assertThat(service.answer("What is coming up?")).isEqualTo("Java Day is next week.");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(this.chatModel).call(prompt.capture());
        assertThat(prompt.getValue().getContents()).contains("Java Day", "capacity: 120", "What is coming up?");
    }
}
```

**The analytics service.** `MockRestServiceServer` answers in place of the Python service. It binds to a `RestClient.Builder`, so the client takes one in a second constructor that only the tests use:

```java
@Autowired
public AnalyticsClientService(@Value("${analytics.base-url}") String baseUrl) {
    this(RestClient.builder().baseUrl(baseUrl));
}

AnalyticsClientService(RestClient.Builder builder) {   // the tests bind MockRestServiceServer to this builder
    this.restClient = builder.build();
}
```

```java
class AnalyticsClientServiceTests {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("http://analytics");

    private final MockRestServiceServer server = MockRestServiceServer.bindTo(this.builder).build();

    private final AnalyticsClientService service = new AnalyticsClientService(this.builder);

    @Test
    void readsRevenueByEvent() {
        this.server.expect(requestTo("http://analytics/analytics/revenue-by-event"))
            .andRespond(withSuccess("""
                [{"name": "Java Day", "revenue": 1500}]
                """, MediaType.APPLICATION_JSON));

        assertThat(this.service.revenueByEvent()).extracting(row -> row.get("name")).containsExactly("Java Day");
    }

    @Test
    void propagatesServiceErrors() {
        this.server.expect(requestTo("http://analytics/analytics/revenue-by-event")).andRespond(withServerError());

        assertThatThrownBy(this.service::revenueByEvent).isInstanceOf(RestClientException.class);
    }
}
```

The client lets the exception propagate; the controller catches it and adds `serviceWarning`, which [Testing views](view-conventions.md#testing-views) tests.

---

## Analytics service

pytest runs from `analytics-service/`; its settings are in `pyproject.toml` (`[tool.pytest.ini_options]`), and the tests import the service modules (`main`, `prediction`) directly. Replace the database access with `monkeypatch` and build the data inside the test:

```python
import pandas as pd

import prediction


def test_forecast_follows_the_capacity_trend(monkeypatch):
    df = pd.DataFrame({"capacity": [50, 100, 150], "event_month": [1, 2, 3], "revenue": [500.0, 1000.0, 1500.0]})
    monkeypatch.setattr(prediction, "query_to_dataframe", lambda sql: df)

    assert prediction.train_and_forecast(200) == {"capacity": 200, "estimated_revenue": 2000.0}
```

- Endpoints are tested with FastAPI's `TestClient`; `tests/test_health.py` is the reference.
- Patch the name where it is used: `prediction.query_to_dataframe`, not `database.query_to_dataframe`, because each module imports its own reference.
- Test the sentiment thresholds with sentences whose result is obvious, and the model with small DataFrames whose trend is known.
- `pytest` and `httpx2` (the HTTP client that `TestClient` uses) are development dependencies, added with `uv add --dev`. `uv sync` installs them, the Docker image leaves them out (`uv sync --no-dev`), and `.dockerignore` leaves out `tests/`.

---

## Postman

The collections in `postman/` document the API and serve as a manual smoke test against the running application. Each request has at least one test, so a run of the collection checks the responses instead of only sending them:

```javascript
pm.test("returns 201", () => pm.response.to.have.status(201));
```

Postman complements the automated tests but does not replace them: it needs the application and Oracle running, and it does not run in CI.

---

## What we avoid

- **H2 or another in-memory database:** tests could pass without testing what runs in Oracle (ADR-009).
- **Real external services in tests:** Gemini spends quota and answers differently each time, and neither it nor the analytics service exists in CI.
- **A coverage percentage as a merge gate:** a number invites tests written to reach it. The review asks instead whether the acceptance criteria are covered.
- **Mocking the class under test, or tests that only verify calls on mocks:** they pass without testing any behavior.
