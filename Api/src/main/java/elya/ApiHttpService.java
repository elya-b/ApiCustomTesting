package elya;

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
public class ApiHttpService {
    private final Map<String, String> authTokens = new ConcurrentHashMap<>();
    @Setter
    private Map<String, Object> mockedResponse = null;

    public void clearMockedResponse() {
        this.mockedResponse = null;
    }

    public Map<String, String> generateAuthToken(String login, String password) {
        if ((login == null) || login.isEmpty() || (password == null) || password.isEmpty()) {
            log.error("Invalid or missing credentials.");
            return Map.of("error", "Invalid or missing credentials.");
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
            HttpStatus status = HttpStatus.UNAUTHORIZED;
            return Map.of("status", status.value(), status.series().toString(), status.getReasonPhrase());
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
