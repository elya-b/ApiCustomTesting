package elya;

import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Low-level runner responsible for managing the Spring Boot application context of the API Emulator.
 * It provides direct control over starting and stopping the application instance.
 */
public class ApiEmulatorRunner {
    private final int initialPort;

    /**
     * The actual port the emulator is running on.
     * Available after the {@link #start()} method is successfully executed.
     */
    @Getter
    private int actualPort;

    /**
     * The Spring Application Context of the running emulator.
     * Used to retrieve internal beans for testing purposes.
     */
    @Getter
    private ConfigurableApplicationContext appContext;

    /**
     * Constructs a runner with a target port.
     *
     * @param initialPort The preferred port to start the emulator on.
     */
    public ApiEmulatorRunner(int initialPort) {
        this.initialPort = initialPort;
    }

    /**
     * Launches the Spring Boot application.
     * If the application is already running, the call is ignored to prevent memory leaks.
     */
    public void start() {
        if (isRunning()) {
            return;
        }

        String[] args = new String[]{"--server.port=" + initialPort};
        this.appContext = SpringApplication.run(ApiEmulator.class, args);

        if (appContext != null && appContext.isRunning()) {
            this.actualPort = appContext.getEnvironment()
                    .getProperty("local.server.port", Integer.class, initialPort);

            logBanner();
        }
    }

    /**
     * Gracefully shuts down the Spring Boot application context.
     */
    public void stop() {
        if (appContext != null) {
            SpringApplication.exit(appContext);
            System.out.println("🛑 Emulator on port " + actualPort + " stopped.");
            this.appContext = null;
        }
    }

    /**
     * Checks if the emulator's application context is active and running.
     *
     * @return true if the context is non-null and running, false otherwise.
     */
    public boolean isRunning() {
        return appContext != null && appContext.isRunning();
    }

    /**
     * Prints a visual confirmation of the emulator startup to the console.
     */
    private void logBanner() {
        System.out.println("\n##########################################");
        System.out.println("#   🚀 EMULATOR STARTED ON PORT: " + actualPort + "    #");
        System.out.println("##########################################\n");
    }

    /**
     * Retrieves a bean from the context.
     */
    public <T> T getBean(Class<T> beanClass) {
        return appContext.getBean(beanClass);
    }
}
