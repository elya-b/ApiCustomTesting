package elya;

import elya.credentials.ApiEmulatorCredentialsStructure;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Boot auto-configuration for the API Emulator.
 * <p>Activates only if the property {@code api.emulator.enabled} is set to {@code true}
 * (defaults to {@code true} if missing). It initializes required configuration properties
 * and provides a default security setup for the emulator environment.</p>
 */
@Configuration
@ConditionalOnClass(PasswordEncoder.class)
@ConditionalOnProperty(
        name = "api.emulator.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties({
        ApiTokenConfig.class,
        ApiEmulatorCredentialsStructure.class
})
public class ApiEmulatorAutoConfiguration {

    /**
     * Provides a default {@link PasswordEncoder} bean if no other encoder is defined in the context.
     * <p>Uses {@link NoOpPasswordEncoder} to simplify local testing and emulation,
     * as performance and simplicity are prioritized over hashing in this specific mock context.</p>
     *
     * @return a plain-text password encoder instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}