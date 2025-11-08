package elya;

import elya.credentials.ApiEmulatorCredentialsService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ApiHttpService implements ApiHttpStatusesGenerator {
    private final Map<String, String> authTokens = new ConcurrentHashMap<>();
    @Setter
    private Map<String, Object> mockedResponse = null;
    private final ApiEmulatorCredentialsService credentialsService;

    public ApiHttpService(ApiEmulatorCredentialsService credentialsService) {
        this.credentialsService = credentialsService;
    }

    public void clearMockedResponse() {
        this.mockedResponse = null;
    }

    public Map<String, String> generateAuthToken(String login, String password) {
        boolean credentialsMatch = credentialsService.getApiEmulatorCredentials()
                .getUsers().stream()
                .anyMatch(cred -> login.equals(cred.getLogin()) && password.equals(cred.getPassword()));

        if ((login == null) || login.isEmpty() || (password == null) || password.isEmpty() || !credentialsMatch) {
            log.error("Invalid or missing credentials.");
            return generateHttpStatus(HttpStatus.UNAUTHORIZED);
        }

        String authToken = UUID.randomUUID().toString();

        authTokens.put(login, authToken);
        return authTokens;
    }

    protected boolean validateAuthToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return false;

        String generatedToken = authorization.substring(7);
        return authTokens.containsValue(generatedToken);
    }

    public Map<String, Object> getApiBankCards(String authorization) {
        if (!validateAuthToken(authorization)) {
            generateHttpStatus(HttpStatus.UNAUTHORIZED);
        }

        if (mockedResponse != null) {
            return mockedResponse;
        } else {
            List<Map<String, String>> cards = new ArrayList<>();
            return
                    Map.of(
                            "response", Map.of(
                                    "size", 0,
                                    "cards", cards
                            )
                    );
        }
    }
}
