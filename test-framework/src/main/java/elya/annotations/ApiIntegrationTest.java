package elya.annotations;

import elya.engine.services.emulator.config.EmulatorTestConfig;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.annotation.*;

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
