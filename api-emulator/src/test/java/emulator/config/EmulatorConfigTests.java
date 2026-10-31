package emulator.config;

import elya.emulator.config.*;
import elya.emulator.credentials.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.context.properties.bind.validation.*;
import org.springframework.boot.test.context.runner.*;
import org.springframework.security.crypto.password.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link EmulatorConfig}.
 * <ul>
 *   <li>Creates the password encoder and binds the token, storage and credentials settings</li>
 *   <li>Fails to start when the users list is empty (BindValidationException)</li>
 * </ul>
 */
class EmulatorConfigTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EmulatorConfig.class);

    @Test
    @DisplayName("Should create PasswordEncoder and bind all emulator settings")
    void shouldCreateBeansAndBindSettings() {
        contextRunner
                .withPropertyValues(
                        "api.credentials.users[0].login=login",
                        "api.credentials.users[0].password=pass"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(PasswordEncoder.class);
                    assertThat(context).hasSingleBean(ApiTokenProperties.class);
                    assertThat(context).hasSingleBean(EmulatorStorageProperties.class);
                    assertThat(context).hasSingleBean(ApiEmulatorCredentialsStructure.class);
                });
    }

    @Test
    @DisplayName("Should fail to start when users list is empty")
    void shouldFailWhenUsersMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context).getFailure()
                    .hasRootCauseInstanceOf(BindValidationException.class);
        });
    }
}