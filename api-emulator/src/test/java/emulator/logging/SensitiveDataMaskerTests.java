package emulator.logging;

import elya.emulator.logging.SensitiveDataMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link elya.emulator.logging.SensitiveDataMasker}.
 * <ul>
 *   <li>{@code maskToken()} — a long token keeps only its last four characters</li>
 *   <li>{@code maskToken()} — the shortest token that shows its tail (5 characters, boundary)</li>
 *   <li>{@code maskToken()} — tokens up to 4 characters are hidden completely (parameterized)</li>
 *   <li>{@code maskToken()} — a null token is marked instead of failing</li>
 * </ul>
 */
public class SensitiveDataMaskerTests {

    @Test
    @DisplayName("maskToken() - Should keep only the last four characters of a long token")
    void maskToken_ShouldKeepLastFourCharacters() {
        String token = "3f2b8c1e-9d4a-4f6b-8e2a-7c5d1a0b1a2b";

        String masked = SensitiveDataMasker.maskToken(token);

        assertEquals("****1a2b", masked, "Only the last four characters must stay visible");
    }

    @Test
    @DisplayName("maskToken() - Should show the tail from 5 characters on (boundary)")
    void maskToken_ShouldShowTailAtBoundary() {
        String token = "abcde";

        String masked = SensitiveDataMasker.maskToken(token);

        assertEquals("****bcde", masked, "A 5-character token is the shortest one that shows its tail");
    }

    @ParameterizedTest(name = "[{index}] Testing with token: ''{0}''")
    @ValueSource(strings = {"", "abcd"})
    @DisplayName("maskToken() - Should hide tokens up to 4 characters completely")
    void maskToken_ShouldHideShortTokenCompletely(String token) {
        String masked = SensitiveDataMasker.maskToken(token);

        assertEquals("****", masked, "Tokens up to 4 characters must not reveal any character");
    }

    @Test
    @DisplayName("maskToken() - Should mark a null token instead of failing")
    void maskToken_ShouldMarkNullToken() {
        String masked = SensitiveDataMasker.maskToken(null);

        assertEquals("<none>", masked, "A null token must be logged as <none>, not cause an NPE");
    }
}