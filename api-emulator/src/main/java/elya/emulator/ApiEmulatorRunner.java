package elya.emulator;

import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.boot.*;
import org.springframework.context.*;

import java.util.*;
import java.util.stream.*;

/**
 * Low-level runner responsible for managing the Spring Boot application context of the API Emulator.
 * Provides granular control over the application lifecycle, including programmatically starting
 * and stopping the instance within a testing environment.
 */
@Slf4j
public class ApiEmulatorRunner {
    private final int initialPort;

    /**
     * Extra properties passed to the emulator on startup, as command line arguments.
     * They take precedence over the configuration files, so a caller can give each
     * emulator its own settings, for example its own storage files.
     */
    private final Map<String, String> properties;

    /**
     * The actual port the emulator is bound to.
     * This value is resolved from the environment after successful startup
     * and may differ from the initial port if port 0 was requested.
     */
    @Getter
    private int actualPort;

    /**
     * The Spring Application Context of the running emulator.
     * Provides access to the underlying bean container and environment properties.
     */
    @Getter
    private ConfigurableApplicationContext appContext;

    /**
     * Constructs a runner with a specific target port configuration.
     *
     * @param initialPort the preferred port number to bind the emulator to (use 0 for a random available port).
     */
    public ApiEmulatorRunner(int initialPort) {
        this(initialPort, Map.of());
    }

    /**
     * Constructs a runner with a target port and extra startup properties.
     *
     * @param initialPort the preferred port number to bind the emulator to (use 0 for a random available port).
     * @param properties  property names and values that override the configuration files
     *                    of this emulator only, e.g. {@code api.emulator.storage.sessions.path}.
     */
    public ApiEmulatorRunner(int initialPort, Map<String, String> properties) {
        this.initialPort = initialPort;
        this.properties = Map.copyOf(properties);
    }

    /**
     * Launches the Spring Boot application using the {@link ApiEmulator} configuration
     * and the extra properties given to the constructor.
     * If an instance is already active, the request is ignored to prevent context duplication.
     * Upon startup, resolves the local server port and logs the status.
     */
    public void start() {
        if (isRunning()) {
            return;
        }

        this.appContext = SpringApplication.run(ApiEmulator.class, startupArguments());

        if (appContext != null && appContext.isRunning()) {
            this.actualPort = appContext.getEnvironment()
                    .getProperty("local.server.port", Integer.class, initialPort);

            log.info("""
                
                ==========================================
                  🚀 EMULATOR STARTED ON PORT: {}
                ==========================================""", actualPort);
        }
    }

    /**
     * Gracefully shuts down the Spring Boot application context.
     * Releases all bound resources, including the embedded web server and port.
     */
    public void stop() {
        if (appContext != null) {
            SpringApplication.exit(appContext);
            log.info("""
                
                ==========================================
                  🛑 EMULATOR STOPPED, PORT {} RELEASED
                ==========================================""", actualPort);
            this.appContext = null;
        }
    }

    /**
     * Checks the current state of the emulator's application context.
     *
     * @return {@code true} if the context is initialized and active; {@code false} otherwise.
     */
    public boolean isRunning() {
        return appContext != null && appContext.isRunning();
    }

    /**
     * Retrieves a specific bean instance from the running application context.
     * Useful for performing deep assertions or state modifications in integration tests.
     *
     * @param <T>       the type of the bean to retrieve
     * @param beanClass the class of the bean to retrieve
     * @return the bean instance found in the context
     * @throws org.springframework.beans.BeansException if the bean could not be found or initialized
     */
    public <T> T getBean(Class<T> beanClass) {
        return appContext.getBean(beanClass);
    }

    /**
     * Builds the command line arguments: the port first, then the extra properties.
     *
     * @return the arguments in the {@code --name=value} form Spring Boot reads
     */
    private String[] startupArguments() {
        return Stream.concat(
                        Stream.of("--server.port=" + initialPort),
                        properties.entrySet().stream().map(e -> "--" + e.getKey() + "=" + e.getValue()))
                .toArray(String[]::new);
    }
}