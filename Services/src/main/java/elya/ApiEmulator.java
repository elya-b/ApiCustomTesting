package elya;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class ApiEmulator {
    private final String host;
    private final int port;
    private ConfigurableApplicationContext appContext;
    private ApiHttpService service;
    private String url;
    private String authToken;
    private String login;
    private String password;

    public ApiEmulator(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        String[] args = new String[]{"--server.port=" + port};
        this.appContext = SpringApplication.run(Api.class, args);
        this.service = appContext.getBean(ApiHttpService.class);
        this.url = "http://" + host + ":" + port;

        //generate authToken
    }


}
