package elya;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api.token")
@Getter
@Setter
public class ApiTokenConfig {
    private int expiration;
    private String issuer;
}
