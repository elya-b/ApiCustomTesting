import elya.ApiEmulatorAutoConfiguration;
import elya.ApiTokenConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApiEmulatorAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ApiEmulatorAutoConfiguration.class))
            .withPropertyValues(
                    "api.credentials.users[0].login=login",
                    "api.credentials.users[0].password=pass"
            );

    @Test
    @DisplayName("Should create PasswordEncoder when property is missing (default behavior)")
    void shouldCreateBeanByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PasswordEncoder.class);
            assertThat(context).hasSingleBean(ApiTokenConfig.class);
        });
    }

    @Test
    @DisplayName("Should NOT create PasswordEncoder when emulator is disabled")
    void shouldNotCreateBeanWhenDisabled() {
        contextRunner.withPropertyValues("api.emulator.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PasswordEncoder.class);
                });
    }

    @Test
    @DisplayName("Should correctly bind properties to ApiTokenConfig")
    void shouldBindProperties() {
        contextRunner.withPropertyValues(
                "api.token.expiration=3600",
                "api.token.issuer=issuer"
        ).run(context -> {
            ApiTokenConfig config = context.getBean(ApiTokenConfig.class);
            assertEquals(3600, config.getExpiration());
            assertEquals("issuer", config.getIssuer());
        });
    }

    @Test
    @DisplayName("Should fail to start when users list is empty")
    void shouldFailWhenUsersMissing() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ApiEmulatorAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context).getFailure()
                            .hasRootCauseInstanceOf(BindValidationException.class);
                });
    }
}
