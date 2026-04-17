package elya.annotations;

import elya.engine.services.emulator.config.EmulatorTestConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.*;

/**
 * Composite annotation for API Integration testing.
 * <p>This meta-annotation encapsulates the following configuration logic:</p>
 * <ul>
 * <li><b>{@link ExtendWith}:</b> Integrates the Spring TestContext Framework into JUnit 5,
 * enabling dependency injection (@Autowired) within test classes.</li>
 * * <li><b>{@link SpringBootTest}:</b> Starts the full Spring application context for integration testing.
 * <ul>
 * <li><i>classes:</i> Explicitly points to {@link EmulatorTestConfig} to avoid heavy full-classpath
 * scanning and speed up startup.</li>
 * <li><i>properties:</i> Sets {@code allow-bean-definition-overriding=true} to resolve
 * conflicts when a test needs to replace a production bean with a Mock/Stub.</li>
 * </ul>
 * </li>
 * * <li><b>{@link TestPropertySource}:</b> Ensures the test runner uses the actual {@code application.yml},
 * preventing "Missing Property" errors for credentials or server ports during execution.</li>
 * * <li><b>{@link Target} & {@link Retention}:</b> Standard meta-annotations defining that this
 * marker is used on classes (TYPE) and remains available during test execution (RUNTIME).</li>
 * * <li><b>{@link Inherited}:</b> Allows subclasses of a base test class to automatically inherit
 * this configuration without re-declaring it.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = EmulatorTestConfig.class,
        properties = {
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@TestPropertySource(locations = "classpath:application.yml")
public @interface ApiIntegrationTest {
}