package elya;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

import static elya.emulator.constants.logs.ApiInfoLogs.*;

/**
 * Main entry point for the Bank API Emulator application.
 * This class initializes the Spring Boot context and configures the embedded web server.
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "elya")
public class ApiEmulator {

    /**
     * Bootstraps the application.
     * <p>Note: By default, the server port is set to 8080 via {@code application.yml}.
     * If you need dynamic port allocation for parallel testing, change the default property to "0".</p>
     *
     * @param args command line arguments passed during startup.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ApiEmulator.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "8080"));
        app.run(args);
        log.info(EMULATOR_STARTED_ON_PORT_SUCCESSFULLY);
    }
}