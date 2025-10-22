package elya;

import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class ApiRunner {
    @Getter
    private final int port;
    private ConfigurableApplicationContext appContext;

    public ApiRunner(int port) {
        this.port = port;
    }

    public void start() {
        String[] args = new String[]{"--server.port=" + port};
        this.appContext = SpringApplication.run(Api.class);
    }

    public void stop() {
        if (appContext != null) {
            SpringApplication.exit(appContext);
        }
    }

    public <T> T getBean(Class<T> beanClass) {
        return appContext.getBean(beanClass);
    }

    public boolean isRunning() {
        return appContext != null && appContext.isRunning();
    }
}
