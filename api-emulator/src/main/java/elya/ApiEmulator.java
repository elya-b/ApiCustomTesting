package elya;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

@Slf4j
@SpringBootApplication(scanBasePackages = "elya") // scans all project modules starting with 'elya' to register controllers and handlers for proper error processing
public class ApiEmulator {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ApiEmulator.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "0"));
        app.run(args);
    }
}