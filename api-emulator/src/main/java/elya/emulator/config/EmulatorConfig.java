package elya.emulator.config;

import elya.emulator.credentials.*;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.*;

/**
 * Configuration of the API Emulator.
 * <p>Binds the emulator settings: token ({@code api.credentials.token.*}),
 * storage ({@code api.emulator.storage.*}) and users ({@code api.credentials.users}),
 * and provides the password encoder used to check user credentials.</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        ApiTokenProperties.class,
        EmulatorStorageProperties.class,
        ApiEmulatorCredentialsStructure.class
})
public class EmulatorConfig {

    /**
     * Compares passwords as plain text.
     * <p>The emulator users are test accounts declared in configuration, so hashing gives no protection here.</p>
     *
     * @return a plain-text password encoder instance.
     */
    @Bean
    @SuppressWarnings("deprecation") // deprecated only as a warning about plain-text passwords; intended here
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}