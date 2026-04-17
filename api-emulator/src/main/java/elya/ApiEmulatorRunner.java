package elya;

import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Low-level runner responsible for managing the Spring Boot application context of the API Emulator.
 * Provides granular control over the application lifecycle, including programmatically starting
 * and stopping the instance within a testing environment.
 */
public class ApiEmulatorRunner {
    private final int initialPort;

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
        this.initialPort = initialPort;
    }

    /**
     * Launches the Spring Boot application using the {@link ApiEmulator} configuration.
     * If an instance is already active, the request is ignored to prevent context duplication.
     * Upon startup, resolves the local server port and logs the status.
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
     * Releases all bound resources, including the embedded web server and port.
     */
    public void stop() {
        if (appContext != null) {
            SpringApplication.exit(appContext);
            System.out.println("🛑 Emulator on port " + actualPort + " stopped.");
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
     * Prints a visual startup confirmation to the system console.
     * Displays the final bound port for debugging purposes.
     */
    private void logBanner() {
        System.out.println("\n##########################################");
        System.out.println("#   🚀 EMULATOR STARTED ON PORT: " + actualPort + "    #");
        System.out.println("##########################################\n");
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
}