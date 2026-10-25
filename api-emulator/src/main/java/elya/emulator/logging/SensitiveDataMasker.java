package elya.emulator.logging;

/**
 * Utility for masking sensitive values before they are written to the logs.
 * <p>Keeps only the last four characters of a token: enough to correlate the log lines
 * of one session without exposing a usable credential, the same idea as PAN masking
 * for card numbers. Values no longer than the visible tail are hidden completely.</p>
 */
public final class SensitiveDataMasker {

    /** Placeholder that replaces the hidden part of a value. */
    private static final String MASK = "****";

    /** Marker logged instead of a token that is absent. */
    private static final String ABSENT = "<none>";

    /** Number of trailing characters left visible after masking. */
    private static final int VISIBLE_TAIL = 4;

    /**
     * Prevents instantiation of this utility class.
     */
    private SensitiveDataMasker() {
    }

    /**
     * Returns a log-safe representation of an authentication token.
     * <p>Example: a UUID token ending in {@code 1a2b} is logged as {@code ****1a2b}.</p>
     *
     * @param token the raw token to mask, may be {@code null}.
     * @return the mask followed by the last four characters for values longer than four
     *         characters, the mask alone for shorter values, or {@code <none>} for {@code null}.
     */
    public static String maskToken(String token) {
        if (token == null) {
            return ABSENT;
        }
        if (token.length() <= VISIBLE_TAIL) {
            return MASK;
        }
        return MASK + token.substring(token.length() - VISIBLE_TAIL);
    }
}