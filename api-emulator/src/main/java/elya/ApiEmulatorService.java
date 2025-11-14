package elya;

import elya.emulator.interfaces.ApiEmulatorStatusInfoGenerator;
import elya.services.ApiEmulatorCredentialsService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static elya.emulator.constants.logs.ApiErrorLogs.*;
import static elya.general.enums.HttpHeaderValues.*;
import static elya.general.enums.responsemodel.ApiBankCards.*;
import static elya.general.enums.responsemodel.AuthToken.*;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@Service
public class ApiEmulatorService implements ApiEmulatorStatusInfoGenerator {
    private final Map<String, String> authTokens = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private ApiEmulatorCredentialsService credentialsService;

    @Setter
    private Map<String, Object> mockedResponse = null;

    public void clearMockedResponse() {
        this.mockedResponse = null;
    }

    public Map<String, String> generateAuthToken(String login, String password) {
        boolean credentialsMatch = credentialsService.getApiEmulatorCredentials()
                .getUsers().stream()
                .anyMatch(cred -> login.equals(cred.getLogin()) &&
                        passwordEncoder.matches(password, cred.getPassword()));

        if ((login == null) || login.isEmpty() || (password == null) || password.isEmpty() || !credentialsMatch) {
            log.error(INVALID_OR_MISSING_CREDENTIALS);
            return generateHttpStatusInfo(UNAUTHORIZED);
        }

        String authToken = UUID.randomUUID().toString();
        authTokens.put(login, authToken);

        return Map.of(
                TOKEN.toString(), authToken,
                TYPE.toString(), BEARER.toString().trim(),
                EXPIRES_IN.toString(), "3600"
        );
    }

    protected boolean validateAuthToken(String token) {
        if (token == null || !token.startsWith(BEARER.toString())) return false;

        String generatedToken = token.substring(7);
        return authTokens.containsValue(generatedToken);
    }

    public Map<String, Object> getApiBankCards(String token) {
        if (!validateAuthToken(token)) {
            return (Map<String, Object>) (Map<?, ?>) generateHttpStatusInfo(UNAUTHORIZED);
        }

        if (mockedResponse != null) {
            return mockedResponse;
        } else {
            List<Map<String, String>> cards = new ArrayList<>();
            return
                    Map.of(
                            RESPONSE.toString(), Map.of(
                                    SIZE.toString(), 0,
                                    CARDS.toString(), cards
                            )
                    );
        }
    }
}
