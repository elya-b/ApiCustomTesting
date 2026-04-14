package elya.engine.services.emulator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.ApiEmulator;
import elya.ApiEmulatorService;
import elya.emulator.objects.ApiEmulatorBankCard;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static elya.engine.services.emulator.constants.Exceptions.*;
import static elya.engine.services.emulator.constants.InfoLogs.*;
import static elya.general.enums.HttpHeaderValues.*;
import static elya.general.enums.responsemodel.ApiBankCards.*;
import static org.springframework.http.HttpStatus.*;

@Slf4j
public class EmulatorLifecycleManager {
    private final int port;
    private ConfigurableApplicationContext appContext;
    @Getter
    private ApiEmulatorService service;
    @Getter
    private String url;
    @Getter
    private String authToken;

    public EmulatorLifecycleManager(int port) {
        this.port = port;
    }

    public void start(String login, String password) {
        String[] args = new String[]{"--server.port=" + port};
        this.appContext = SpringApplication.run(ApiEmulator.class, args);

        waitUntilReady(Duration.ofSeconds(30));
        generateToken(login, password);
    }

    public void stop() {
        if (appContext != null) {
            SpringApplication.exit(appContext, () -> 0);
        }
    }

    private void generateToken(String login, String password) {
        Map<String, String> authTokenResponse = service.generateAuthToken(login, password);

        if (authTokenResponse != null && authTokenResponse.containsKey(UNAUTHORIZED.getReasonPhrase())) {
            throw new IllegalStateException(FAILED_TO_GENERATE_AUTH_TOKEN);
        }

        this.authToken = authTokenResponse.get(login);
    }

    public List<ApiEmulatorBankCard> getBankCards(String authToken) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ApiEmulatorBankCard> bankCards;

        Map<String, Object> response = service.getApiBankCards(BEARER + authToken);
        Map<String, Object> responseData = (Map<String, Object>) response.get(RESPONSE.toString());

        if (responseData == null) {
            return Collections.emptyList();
        }

        Object cards = responseData.get(CARDS.toString());

        bankCards = cards != null ?
                objectMapper.convertValue(cards, new TypeReference<>() {}) : Collections.emptyList();

        return bankCards;
    }

    private void waitUntilReady(Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        RestTemplate restTemplate = new RestTemplate();
        String healthUrl = url + "/actuator/health";
        while (Instant.now().isBefore(deadline)) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl,
                        String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    log.info(EMULATOR_IS_READY);
                    return;
                }
            } catch (Exception e) {
                // Not ready yet
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(INTERRUPTED_EMULATOR_STARTUP, e);
            }
        }
        throw new RuntimeException(FAILED_TO_START_EMULATOR + timeout);
    }
}
