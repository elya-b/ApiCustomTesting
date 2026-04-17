package elya.credentials;

import elya.constants.Role;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

import static elya.constants.logs.InfoLogs.CONFIG_LOADED_SUCCESSFULLY;

/**
 * Configuration mapping class for API Emulator security credentials.
 * <p>Binds properties prefixed with {@code api.credentials} from configuration files
 * (e.g., application.yml) directly into Java objects with automatic validation.</p>
 */
@Slf4j
@Getter
@Setter
@ToString
@Validated
@ConfigurationProperties(prefix = "api.credentials")
public class ApiEmulatorCredentialsStructure {

    /** * List of authorized users.
     * Must contain at least one entry to ensure the emulator is not left unprotected.
     */
    @NotEmpty(message = "Config error: users list cannot be empty")
    @Valid
    private List<Credential> users = new ArrayList<>();

    /**
     * Post-initialization callback to confirm successful configuration loading.
     * Logs the total count of recognized user accounts.
     */
    @PostConstruct
    public void validateConfig() {
        log.info(CONFIG_LOADED_SUCCESSFULLY, users.size());
    }

    /**
     * Data transfer object representing a single user's security profile.
     */
    @Getter
    @Setter
    @ToString
    public static class Credential {

        /** User identifier used for authentication. */
        @NotBlank(message = "Config error: invalid credentials or missing fields in YAML")
        private String login;

        /** Plain-text or encrypted password for the user. */
        @NotBlank(message = "Config error: invalid credentials or missing fields in YAML")
        private String password;

        /** Assigned authorization level. Defaults to null if not specified in config. */
        private Role role;
    }
}