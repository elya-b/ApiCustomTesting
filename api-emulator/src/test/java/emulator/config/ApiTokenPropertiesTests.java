package emulator.config;

import elya.emulator.config.*;
import elya.emulator.exceptions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.context.properties.*;
import org.springframework.boot.test.context.runner.*;
import org.springframework.context.annotation.*;

import java.time.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ApiTokenProperties}.
 * <ul>
 *   <li>Token expiration binds from a bare number (seconds) and from a unit suffix</li>
 *   <li>Falls back to 1h / "api-emulator" when properties are absent</li>
 *   <li>Context fails to start when the token expiration is not positive</li>
 *   <li>Rejects a null expiration and a blank issuer</li>
 * </ul>
 */
class ApiTokenPropertiesTests {

    /** Binds only the token settings, so the tests need no other emulator properties. */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ApiTokenProperties.class)
    static class TokenPropertiesConfig {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TokenPropertiesConfig.class);

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
                    assertThat(properties.expiration()).isEqualTo(Duration.ofHours(1));
                    assertThat(properties.issuer()).isEqualTo("issuer");
                });
    }

    @Test
    @DisplayName("Should bind a duration with a unit suffix")
    void shouldBindDurationWithUnitSuffix() {
        contextRunner
                .withPropertyValues("api.credentials.token.expiration=90m")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(ApiTokenProperties.class).expiration())
                            .isEqualTo(Duration.ofMinutes(90));
                });
    }

    @Test
    @DisplayName("Should use default values when properties are not set")
    void shouldUseDefaultValues() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            ApiTokenProperties properties = context.getBean(ApiTokenProperties.class);
            assertThat(properties.expiration())
                    .as("Default expiration must be 1 hour when property is not set")
                    .isEqualTo(Duration.ofHours(1));
            assertThat(properties.issuer())
                    .as("Default issuer must be 'api-emulator' when property is not set")
                    .isEqualTo("api-emulator");
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
        assertThatThrownBy(() -> new ApiTokenProperties(null, "issuer"))
                .isInstanceOf(ApiEmulatorConfigurationException.class);
    }

    @ParameterizedTest(name = "issuer=''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Should reject a blank issuer")
    void shouldRejectBlankIssuer(String issuer) {
        assertThatThrownBy(() -> new ApiTokenProperties(Duration.ofHours(1), issuer))
                .isInstanceOf(ApiEmulatorConfigurationException.class);
    }
}