package elya;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ApiEmulatorHttpService {
    private final Map<String, String> authTokens = new ConcurrentHashMap<>();
    @Setter
    private Map<String, Object> mockedResponse = null;

    public void clearMockedResponse() {
        this.mockedResponse = null;
    }

    public Map<String, String> generateAuthToken(String login, String password) {
        String authToken = "";

        if ((login != null) || !login.isEmpty() || (password != null) || !password.isEmpty()) {
            authToken = UUID.randomUUID().toString();
        } else log.error("Invalid or missing credentials");

        authTokens.put(authToken, login);
        return authTokens;
    }

}
