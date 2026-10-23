package emulator.services;

import elya.ApiEmulatorAutoConfiguration;
import elya.emulator.repository.SessionRepository;
import elya.emulator.services.TokenManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Wiring test: token settings from configuration must reach {@link TokenManagerService}.
 * <p>Regression guard for the bug where the service read non-existent property keys via
 * {@code @Value} and silently fell back to its defaults, so changing the yml had no effect.</p>
 */
class TokenPropertiesWiringTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ApiEmulatorAutoConfiguration.class))
            .withBean(SessionRepository.class, () -> mock(SessionRepository.class))
            .withBean(TokenManagerService.class)
            .withPropertyValues(
                    "api.credentials.users[0].login=login",
                    "api.credentials.users[0].password=pass"
            );

    @Test
    @DisplayName("Configured token lifetime and issuer reach TokenManagerService")
    void configuredValuesReachService() {
        contextRunner
                .withPropertyValues(
                        "api.credentials.token.expiration=2h",
                        "api.credentials.token.issuer=custom-issuer"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    TokenManagerService service = context.getBean(TokenManagerService.class);
                    assertThat(service.getExpirationSeconds()).isEqualTo(7200L);
                    assertThat(service.getIssuer()).isEqualTo("custom-issuer");
                });
    }
}