package elya.allure;

/**
 * Priority levels for the {@link Priority} annotation.
 *
 * <p>Values are intentionally lowercase so they render cleanly in Allure's
 * sidebar filter (e.g. "priority: critical") without any extra transformation.</p>
 */
public enum PriorityLevel {
    /** Release-blocking: must pass before any deployment. */
    CRITICAL,
    /** Core business logic: failures are high-impact. */
    HIGH,
    /** Important but non-blocking: should be fixed in the current sprint. */
    MEDIUM,
    /** Edge cases or cosmetic checks: low business impact. */
    LOW
}