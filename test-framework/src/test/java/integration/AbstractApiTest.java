package integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.annotations.ApiIntegrationTest;
import elya.api.RestClientApi;
import elya.constants.Role;
import elya.credentials.ApiEmulatorCredentialsService;
import elya.dto.auth.AuthRequest;
import elya.engine.services.emulator.EmulatorLifecycleManager;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class for all API integration tests.
 * <p>Handles the full emulator lifecycle ({@code start}/{@code stop}) around each test method
 * and provides shared helper utilities for authentication and assertion steps.</p>
 * <p>Concrete test classes inherit the following behaviour:</p>
 * <ul>
 *   <li>{@code setUp()} — starts the emulator and authenticates as admin before each test</li>
 *   <li>{@code tearDown()} — stops the emulator after each test</li>
 *   <li>{@code switchUserAndGetToken(Role)} — authenticates as a user with the specified role</li>
 *   <li>{@code prepareLoginRequest(String, String)} — builds an {@link AuthRequest} from credentials</li>
 *   <li>{@code prepareLoginRequest(Role)} — builds an {@link AuthRequest} by role lookup</li>
 *   <li>{@code verify(String, Runnable)} — runs assertions inside a named Allure step;
 *       on failure attaches the error message automatically</li>
 *   <li>{@code attachJson(String, Object)} — serialises any object to pretty JSON
 *       and attaches it to the current Allure step using the provided label</li>
 * </ul>
 */
@Slf4j
@ApiIntegrationTest
public abstract class AbstractApiTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    protected EmulatorLifecycleManager emulator;
    @Autowired
    protected RestClientApi clientApi;
    @Autowired
    protected ApiEmulatorCredentialsService credentialsService;

    @BeforeEach
    @Step("Setup: start emulator and authenticate as admin")
    protected void setUp() {
        var adminUser = credentialsService.getApiEmulatorCredentials().getUsers().getFirst();
        if (adminUser != null) {
            emulator.start(prepareLoginRequest(adminUser.getLogin(), adminUser.getPassword()));
        } else {
            log.error("Can't start emulator because user is NULL!");
        }
    }

    @AfterEach
    @Step("Teardown: stop emulator")
    protected void tearDown() {
        if (emulator != null) emulator.stop();
    }

    protected String switchUserAndGetToken(Role role) {
        var request = prepareLoginRequest(role);
        return clientApi.generateAuthToken(request).getToken();
    }

    @Step("Prepare login request for user: {login}")
    protected AuthRequest prepareLoginRequest(String login, String password) {
        AuthRequest request = new AuthRequest();
        request.setLogin(login);
        request.setPassword(password);
        return request;
    }

    @Step("Prepare login request for user with role: {role}")
    protected AuthRequest prepareLoginRequest(Role role) {
        var testUser = credentialsService.getApiEmulatorCredentials().getUsers().stream()
                .filter(p -> p.getRole() == role)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(role + " user not found in configuration"));

        return prepareLoginRequest(testUser.getLogin(), testUser.getPassword());
    }

    /**
     * Runs {@code assertionBlock} inside a named Allure step.
     *
     * <p>On assertion failure the error message is automatically attached as a
     * plain-text snippet so the cause is visible directly in the report without
     * having to scroll through the full stack trace.
     * Unexpected exceptions are re-thrown unchanged after attaching.</p>
     *
     * <p>For finer granularity inside a single verify call, nest {@link Allure#step}:
     * <pre>{@code
     * verify("Verify card details", () -> {
     *     Allure.step("size is 1",       () -> assertThat(result).hasSize(1));
     *     Allure.step("cardId equals 3", () -> assertThat(result.getLast().getCardId()).isEqualTo(3L));
     * });
     * }</pre>
     *
     * @param description human-readable label shown in the Allure report
     * @param assertionBlock assertion code to execute
     */
    @Step("Verify: {description}")
    protected void verify(String description, Runnable assertionBlock) {
        try {
            assertionBlock.run();
        } catch (AssertionError e) {
            // Attach the failure reason so it is visible in the report
            // without opening the full stack trace.
            Allure.addAttachment(
                    "Assertion failure — " + description,
                    "text/plain",
                    new ByteArrayInputStream(e.getMessage().getBytes(StandardCharsets.UTF_8)),
                    ".txt"
            );
            throw e;
        }
    }

    /**
     * Serialises {@code value} to pretty-printed JSON and attaches it to the
     * current Allure step under the given {@code name} label.
     *
     * <p>Uses {@link Allure#addAttachment} directly instead of {@code @Attachment},
     * because {@code @Attachment(value = "{name}")} relies on AspectJ parameter
     * binding which is unreliable — the {@code name} placeholder is often not
     * substituted and appears literally in the report. Calling the API directly
     * guarantees the label is always exactly what was passed.</p>
     *
     * <pre>{@code
     * attachJson("Request body", cardRequest);
     * List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));
     * attachJson("Response body", result);
     * }</pre>
     *
     * @param name  label shown in the Allure report
     * @param value any object serialisable by Jackson
     */
    protected void attachJson(String name, Object value) {
        String json;
        try {
            json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            json = value.toString();
        }
        Allure.addAttachment(
                name,
                "application/json",
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                ".json"
        );
    }
}