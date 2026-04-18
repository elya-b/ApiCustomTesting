package elya;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for authentication token generation.
 * <p>Maps properties with the prefix {@code api.token} from application configuration files
 * to define the security parameters of the emulator's session management.</p>
 */
@ConfigurationProperties(prefix = "api.credentials.token")
@Getter
@Setter
public class ApiTokenConfig {

    /**
     * The duration in seconds until the generated token expires.
     */
    private int expiration;

    /**
     * The identifier of the authority that issues the authentication tokens.
     * This value is included in the {@code issuer} field of the token response.
     */
    private String issuer;
}