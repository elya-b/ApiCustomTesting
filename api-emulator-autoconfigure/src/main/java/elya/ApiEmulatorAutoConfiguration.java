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

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
