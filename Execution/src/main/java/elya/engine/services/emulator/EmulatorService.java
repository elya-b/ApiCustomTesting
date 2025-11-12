package elya.engine.services.emulator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.ApiEmulator;
import elya.ApiEmulatorService;
import elya.objects.ApiEmulatorBankCard;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static elya.engine.services.constants.ErrorLogs.*;
import static elya.engine.services.constants.Exceptions.*;
import static elya.enums.HttpHeaderValues.*;
import static elya.enums.ResponseModel.*;
import static org.springframework.http.HttpStatus.*;

@Slf4j
public class EmulatorService {
    private final String host;
    private final int port;
    private ConfigurableApplicationContext appContext;
    @Getter
    private ApiEmulatorService service;
    @Getter
    private String url;
    @Getter
    private String authToken;

    public EmulatorService(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start(String login, String password) {
        String[] args = new String[]{"--server.port=" + port};

        try {
        this.appContext = SpringApplication.run(ApiEmulator.class, args);
        this.service = appContext.getBean(ApiEmulatorService.class);
        this.url = "http://" + host + ":" + port;

        Thread.sleep(10000);
        generateToken(login, password);

        } catch (InterruptedException e) {
            log.error(LONG_TIME_WAITING_EMULATOR_START + e.getMessage());
            Thread.currentThread().interrupt();
            stop();
        }
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
}
