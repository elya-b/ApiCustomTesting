package elya;

import elya.credentials.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.*;

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
        ApiTokenProperties.class,
        EmulatorStorageProperties.class,
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