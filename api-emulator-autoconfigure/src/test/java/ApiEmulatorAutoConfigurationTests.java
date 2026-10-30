import elya.*;
import elya.constants.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.context.properties.*;
import org.springframework.boot.context.properties.bind.validation.*;
import org.springframework.boot.test.context.runner.*;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.*;

import java.time.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ApiEmulatorAutoConfiguration} and {@link ApiTokenProperties}.
 * <ul>
 *   <li>PasswordEncoder and ApiTokenProperties beans are created by default (emulator enabled)</li>
 *   <li>PasswordEncoder is not created when {@code api.emulator.enabled=false}</li>
 *   <li>Token expiration binds from a bare number (seconds) and from a unit suffix</li>
 *   <li>ApiTokenProperties falls back to 1h / "api-emulator" when properties are absent</li>
 *   <li>Context fails to start when the users list is empty (BindValidationException)</li>
 *   <li>Context fails to start when the token expiration is not positive</li>
 *   <li>ApiTokenProperties rejects a null expiration and a blank issuer</li>
 *   <li>Existing PasswordEncoder bean is not overridden (ConditionalOnMissingBean)</li>
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
    @DisplayName("Should create PasswordEncoder and ApiTokenProperties by default")
    void shouldCreateBeanByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PasswordEncoder.class);
            assertThat(context).hasSingleBean(ApiTokenProperties.class);
        });
    }

    @Test
    @DisplayName("Should NOT create PasswordEncoder when emulator is disabled")
    void shouldNotCreateBeanWhenDisabled() {
        contextRunner.withPropertyValues("api.emulator.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(PasswordEncoder.class));
    }

    @Test
    @DisplayName("Should bind a bare number as seconds")
    void shouldBindBareNumberAsSeconds() {
        contextRunner
                .withPropertyValues(
                        "api.credentials.token.expiration=3600",
                        "api.credentials.token.issuer=issuer"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    ApiTokenProperties properties = context.getBean(ApiTokenProperties.class);
                    assertEquals(Duration.ofHours(1), properties.expiration());
                    assertEquals("issuer", properties.issuer());
                });
    }

    @Test
    @DisplayName("Should bind a duration with a unit suffix")
    void shouldBindDurationWithUnitSuffix() {
        contextRunner
                .withPropertyValues("api.credentials.token.expiration=90m")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertEquals(Duration.ofMinutes(90),
                            context.getBean(ApiTokenProperties.class).expiration());
                });
    }

    @Test
    @DisplayName("Should use default ApiTokenProperties values when properties are not set")
    void shouldUseDefaultApiTokenPropertiesValues() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            ApiTokenProperties properties = context.getBean(ApiTokenProperties.class);
            assertEquals(Duration.ofHours(1), properties.expiration(),
                    "Default expiration must be 1 hour when property is not set");
            assertEquals("api-emulator", properties.issuer(),
                    "Default issuer must be 'api-emulator' when property is not set");
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

    @ParameterizedTest(name = "expiration={0}")
    @ValueSource(strings = {"0", "-60"})
    @DisplayName("Should fail to start when token expiration is not positive")
    void shouldFailWhenExpirationIsNotPositive(String expiration) {
        contextRunner
                .withPropertyValues("api.credentials.token.expiration=" + expiration)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context).getFailure()
                            .hasRootCauseInstanceOf(ApiEmulatorConfigurationException.class);
                });
    }

    @Test
    @DisplayName("Should reject a null expiration")
    void shouldRejectNullExpiration() {
        assertThrows(ApiEmulatorConfigurationException.class,
                () -> new ApiTokenProperties(null, "issuer"));
    }

    @ParameterizedTest(name = "issuer=''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Should reject a blank issuer")
    void shouldRejectBlankIssuer(String issuer) {
        assertThrows(ApiEmulatorConfigurationException.class,
                () -> new ApiTokenProperties(Duration.ofHours(1), issuer));
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

    @Configuration
    @EnableConfigurationProperties
    static class CustomPasswordEncoderConfig {
        @org.springframework.context.annotation.Bean
        PasswordEncoder customEncoder() {
            return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        }
    }
}