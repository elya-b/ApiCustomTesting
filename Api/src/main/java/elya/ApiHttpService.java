package elya;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

        authTokens.put(authToken, login);
        return authTokens;
    }




}
