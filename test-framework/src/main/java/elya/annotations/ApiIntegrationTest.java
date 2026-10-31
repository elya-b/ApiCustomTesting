package elya.annotations;

import elya.engine.services.emulator.config.*;
import org.springframework.boot.test.context.*;

import java.lang.annotation.*;

/**
 * Composite annotation for API integration tests.
 * <ul>
 * <li><b>{@link SpringBootTest}</b> starts the test context from {@link EmulatorTestConfig} only,
 * so it holds the test-side beans and nothing else. It also registers the Spring extension for JUnit 5.</li>
 * <li>{@code config/application.yml} of the test resources imports the shared users from
 * {@code emulator-credentials.yml} and points the in-process emulator storage to {@code target/test-data}.</li>
 * <li><b>{@link Inherited}</b> lets subclasses of a base test class inherit this configuration.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest(classes = EmulatorTestConfig.class)
public @interface ApiIntegrationTest {
}