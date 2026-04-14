package elya.emulator.objects;

/**
 * Record is used as a lightweight, immutable container for internal session metadata
 */
public record TokenRecord (String login, long expiryMillis) {}