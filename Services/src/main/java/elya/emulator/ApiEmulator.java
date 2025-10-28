package elya.emulator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.Api;
import elya.ApiBankCard;
import elya.ApiHttpService;
import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ApiEmulator {
    private final String host;
    private final int port;
    private ConfigurableApplicationContext appContext;
    @Getter
    private ApiHttpService service;
    @Getter
    private String url;
    @Getter
    private String authToken;

    public ApiEmulator(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start(String login, String password) {
        String[] args = new String[]{"--server.port=" + port};

        try {
        this.appContext = SpringApplication.run(Api.class, args);
        this.service = appContext.getBean(ApiHttpService.class);
        this.url = "http://" + host + ":" + port;

        Thread.sleep(10000);
        generateToken(login, password);

        } catch (InterruptedException e) {
            System.out.println("Emulator starting is stopped because of lon time waiting: " + e.getMessage());
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

        if (authTokenResponse != null && authTokenResponse.containsKey("error")) {
            throw new IllegalStateException("Failed to generate auth token");
        }

        this.authToken = authTokenResponse.get(login);
    }

    public List<ApiBankCard> getBankCards(String authToken) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ApiBankCard> bankCards;

        Map<String, Object> response = service.getApiBankCards("Bearer " + authToken);
        Map<String, Object> responseData = (Map<String, Object>) response.get("response");

        if (responseData == null) {
            return Collections.emptyList();
        }

        Object cards = responseData.get("cards");

        bankCards = cards != null ?
                objectMapper.convertValue(cards, new TypeReference<>() {}) : Collections.emptyList();

        return bankCards;
    }
}
