package elya.emulator.objects;

/**
 * A lightweight, immutable container for internal session metadata.
 * <p>This record encapsulates the association between a user's login and
 * their session's expiration timestamp, ensuring thread-safe access
 * to security-critical data.</p>
 *
 * @param login        the unique identifier or username of the session owner.
 * @param expiryMillis the absolute expiration time in milliseconds since the Unix epoch.
 */
public record TokenRecord(String login, long expiryMillis) {
    // Records automatically provide private final fields,
    // a canonical constructor, accessors, equals, hashCode, and toString.
}