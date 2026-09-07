import elya.ApiEmulatorRunner;
import elya.repository.SessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for {@link elya.ApiEmulatorRunner}.
 * <ul>
 *   <li>{@code start()} launches the application, sets port and context</li>
 *   <li>{@code start()} ignores a second call when the server is already running</li>
 *   <li>{@code start()} assigns a random port when port=0</li>
 *   <li>{@code start()} throws an exception when the port is already in use</li>
 *   <li>{@code stop()} shuts down the application; context becomes null</li>
 *   <li>{@code getBean()} returns a bean by type when context is active</li>
 *   <li>{@code getBean()} throws NullPointerException when context is null</li>
 * </ul>
 */
public class ApiEmulatorRunnerTests {

    private ApiEmulatorRunner runner;
    private final int port = 8081;

    @BeforeEach
    void setRunner() {
        runner = new ApiEmulatorRunner(port);
    }

    @AfterEach
    void stop() {
        runner.stop();
    }

    @Test
    @DisplayName("start() - Should should launch application")
    void start_ShouldLaunchApplication() {
        assertFalse(runner.isRunning());

        runner.start();

        assertTrue(runner.isRunning());
        assertNotNull(runner.getAppContext());
        assertEquals(port, runner.getActualPort());
    }

    @Test
    @DisplayName("start() - Should ignore second call if already running")
    void start_ShouldIgnoreSecondCallIfAlreadyRunning() {
        runner.start();
        assertTrue(runner.isRunning());
        var context_1 = runner.getAppContext();

        runner.start();

        assertSame(context_1, runner.getAppContext());
    }

    @Test
    @DisplayName("start() - Should assign random port when initial port is zero")
    void start_ShouldAssignRandomPort_WhenInitialPortIsZero() {
        runner = new ApiEmulatorRunner(0);
        runner.start();

        assertTrue(runner.isRunning());
        assertTrue(runner.getActualPort() > 0);
    }

    @Test
    @DisplayName("start() - Should handle port already in use")
    void start_ShouldHandlePortAlreadyInUse() {
        runner.start();
        assertTrue(runner.isRunning());

        ApiEmulatorRunner competitorRunner = new ApiEmulatorRunner(port);

        assertThrows(Exception.class, () -> competitorRunner.start());

        assertFalse(competitorRunner.isRunning());
        assertNull(competitorRunner.getAppContext());
    }

    @Test
    @DisplayName("stop() - Should shutdown application")
    void stop_ShouldShutdownApplication() {
        runner.start();
        assertTrue(runner.isRunning());

        runner.stop();

        assertFalse(runner.isRunning());
        assertNull(runner.getAppContext());
    }

    @Test
    @DisplayName("getBean() - Should return requested bean")
    void getBean_ShouldReturnRequestedBean() {
        runner.start();
        assertTrue(runner.isRunning());

        assertNotNull(runner.getBean(SessionRepository.class));
    }

    @Test
    @DisplayName("getBean() - Should throw Exception if context is null")
    void getBean_ShouldThrowExceptionIfContextIsNull() {
        assertFalse(runner.isRunning());

        assertThrows(NullPointerException.class, () -> runner.getBean(SessionRepository.class));
    }
}