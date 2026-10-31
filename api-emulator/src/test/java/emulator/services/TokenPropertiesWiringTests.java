package emulator.services;

import elya.emulator.config.*;
import elya.emulator.repository.*;
import elya.emulator.services.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.runner.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Wiring test: token settings from configuration must reach {@link TokenManagerService}.
 * <p>Regression guard for the bug where the service read non-existent property keys via
 * {@code @Value} and silently fell back to its defaults, so changing the yml had no effect.</p>
 */
class TokenPropertiesWiringTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EmulatorConfig.class)
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