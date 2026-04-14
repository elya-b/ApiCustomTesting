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

@Slf4j
@Getter
@Setter
@ToString
@Validated
@ConfigurationProperties(prefix = "api.credentials")
public class ApiEmulatorCredentialsStructure {

    @NotEmpty(message = "Config error: users list cannot be empty")
    @Valid
    private List<Credential> users = new ArrayList<>();

    @PostConstruct
    public void validateConfig() {
        log.info(CONFIG_LOADED_SUCCESSFULLY, users.size());
    }

    @Getter
    @Setter
    @ToString
    public static class Credential {
        @NotBlank(message = "Config error: invalid credentials or missing fields in YAML")
        private String login;

        @NotBlank(message = "Config error: invalid credentials or missing fields in YAML")
        private String password;

        private Role role;
    }
}
