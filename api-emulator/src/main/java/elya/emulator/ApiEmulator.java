package elya.emulator;

import lombok.extern.slf4j.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;

import static elya.emulator.constants.logs.EmulatorInfoLogs.*;

/**
 * Main entry point for the Bank API Emulator application.
 * <p>Lives in the root package of the emulator, so component scanning covers the emulator's
 * own packages only. The server port and other runtime settings come from {@code application.yml}.</p>
 */
@Slf4j
@SpringBootApplication
public class ApiEmulator {

    /**
     * Bootstraps the application.
     *
     * @param args command line arguments passed during startup.
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiEmulator.class, args);
        log.info(EMULATOR_STARTED_ON_PORT_SUCCESSFULLY);
    }
}