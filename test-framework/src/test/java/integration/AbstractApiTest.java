package integration;

import elya.annotations.ApiIntegrationTest;
import elya.api.RestClientApi;
import elya.constants.Role;
import elya.credentials.ApiEmulatorCredentialsService;
import elya.dto.auth.AuthRequest;
import elya.engine.services.emulator.EmulatorLifecycleManager;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@ApiIntegrationTest
public abstract class AbstractApiTest {

    @Autowired
    protected EmulatorLifecycleManager emulator;
    @Autowired
    protected RestClientApi clientApi;
    @Autowired
    protected ApiEmulatorCredentialsService credentialsService;

    @BeforeEach
    protected void setUp() {
        var adminUser = credentialsService.getApiEmulatorCredentials().getUsers().getFirst();
        if (adminUser != null) {
            emulator.start(prepareLoginRequest(adminUser.getLogin(), adminUser.getPassword()));
        } else
            log.error("Can't start emulator because user is NULL!");
    }

    @AfterEach
    protected void tearDown() {
        if (emulator != null) emulator.stop();
    }

    /**
     * Helper to get token for a specific role if different from default admin.
     */
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

    @Step("Verify API response: {description}")
    protected void verify(String description, Runnable assertionBlock) {
        assertionBlock.run();
    }
}
