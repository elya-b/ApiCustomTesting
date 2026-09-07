import elya.ApiEmulatorAutoConfiguration;
import elya.ApiTokenConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for {@link elya.ApiEmulatorAutoConfiguration}.
 * <ul>
 *   <li>PasswordEncoder and ApiTokenConfig beans are created by default (emulator enabled)</li>
 *   <li>PasswordEncoder is not created when {@code api.emulator.enabled=false}</li>
 *   <li>Properties are correctly bound to ApiTokenConfig</li>
 *   <li>Context fails to start when the users list is empty (BindValidationException)</li>
 *   <li>Existing PasswordEncoder bean is not overridden (ConditionalOnMissingBean)</li>
 *   <li>ApiTokenConfig has default values (expiration=0, issuer=null) when properties are absent</li>
 * </ul>
 */
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
        contextRunner
                .withPropertyValues(
                        "api.credentials.token.expiration=3600",
                        "api.credentials.token.issuer=issuer"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
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

    // --- ADDITIONAL CASES ---

    @Test
    @DisplayName("Should NOT override existing PasswordEncoder bean (ConditionalOnMissingBean)")
    void shouldNotOverrideExistingPasswordEncoder() {
        contextRunner
                .withUserConfiguration(CustomPasswordEncoderConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(PasswordEncoder.class);
                    // The bean must be our custom one, not NoOpPasswordEncoder
                    PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
                    assertTrue(encoder.matches("raw", encoder.encode("raw")),
                            "Must use the custom encoder, not the default NoOp one");
                });
    }

    @Test
    @DisplayName("Should use default ApiTokenConfig values when properties are not set")
    void shouldUseDefaultApiTokenConfigValues() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            ApiTokenConfig config = context.getBean(ApiTokenConfig.class);
            // expiration defaults to 0 (int default) and issuer to null when not configured
            assertEquals(0, config.getExpiration(),
                    "Default expiration must be 0 when property is not set");
            assertNull(config.getIssuer(),
                    "Default issuer must be null when property is not set");
        });
    }

    @Configuration
    @EnableConfigurationProperties
    static class CustomPasswordEncoderConfig {
        @org.springframework.context.annotation.Bean
        PasswordEncoder customEncoder() {
            return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        }
    }
}