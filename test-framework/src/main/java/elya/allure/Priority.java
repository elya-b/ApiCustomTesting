package elya.allure;

import io.qameta.allure.LabelAnnotation;

import java.lang.annotation.*;

/**
 * Marks a test with a business priority visible in the Allure report.
 *
 * <p>Allure reads this via {@link LabelAnnotation} and adds a "priority" label
 * to the test result. The label appears in the report's sidebar filter and
 * in the test details panel, making it easy to sort or filter tests by importance.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * @Priority(PriorityLevel.HIGH)
 * @Test
 * void myTest() { ... }
 * }</pre>
 *
 * <p>Priority levels from highest to lowest:
 * <ul>
 *   <li>{@link PriorityLevel#CRITICAL} — smoke / release-blocking tests</li>
 *   <li>{@link PriorityLevel#HIGH}     — core business logic</li>
 *   <li>{@link PriorityLevel#MEDIUM}   — important but non-blocking scenarios</li>
 *   <li>{@link PriorityLevel#LOW}      — edge cases, cosmetic checks</li>
 * </ul>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@LabelAnnotation(name = "priority")
public @interface Priority {
    PriorityLevel value();
}