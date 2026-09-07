package emulator.tokens;

import elya.emulator.tokens.UuidTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for {@link elya.emulator.tokens.UuidTokenProvider}.
 * <ul>
 *   <li>{@code generateToken()} — returns a non-null token</li>
 *   <li>{@code generateToken()} — token is a valid UUID format</li>
 *   <li>{@code generateToken()} — token length is 36 characters (standard UUID)</li>
 *   <li>{@code generateToken()} — login is not embedded in the token</li>
 *   <li>{@code generateToken()} — does not throw for a null login</li>
 *   <li>{@code generateToken()} — two consecutive calls return different tokens</li>
 *   <li>{@code generateToken()} — 50 repeated calls produce unique tokens</li>
 * </ul>
 */
public class UuidTokenProviderTests {
    private UuidTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new UuidTokenProvider();
    }

    @Test
    @DisplayName("generateToken() - Should return non-null token")
    void generateToken_ShouldReturnNonNullToken() {
        String token = tokenProvider.generateToken("user");

        assertNotNull(token, "Token must not be null");
    }

    @Test
    @DisplayName("generateToken() - Should return valid UUID format")
    void generateToken_ShouldReturnValidUuidFormat() {
        String token = tokenProvider.generateToken("user");

        assertDoesNotThrow(() -> UUID.fromString(token),
                "Token must be a valid UUID string");
    }

    @Test
    @DisplayName("generateToken() - Should return token of length 36 (standard UUID)")
    void generateToken_ShouldReturnTokenOfLength36() {
        String token = tokenProvider.generateToken("user");

        assertEquals(36, token.length(), "UUID string length must be 36 characters");
    }

    @Test
    @DisplayName("generateToken() - Login parameter is not embedded in token")
    void generateToken_LoginShouldNotBeEmbeddedInToken() {
        String login = "secretLogin";
        String token = tokenProvider.generateToken(login);

        assertFalse(token.contains(login),
                "UUID token must not contain the login value");
    }

    @Test
    @DisplayName("generateToken() - Should work with null login without throwing")
    void generateToken_ShouldWorkWithNullLogin() {
        assertDoesNotThrow(() -> tokenProvider.generateToken(null),
                "UUID generation must not depend on login value");
    }

    @Test
    @DisplayName("generateToken() - Two consecutive calls must return different tokens")
    void generateToken_TwoCallsMustReturnDifferentTokens() {
        String token1 = tokenProvider.generateToken("user");
        String token2 = tokenProvider.generateToken("user");

        assertNotEquals(token1, token2, "Each call must produce a unique token");
    }

    @RepeatedTest(50)
    @DisplayName("generateToken() - Should produce unique tokens across 50 repetitions")
    void generateToken_ShouldProduceUniqueTokensAcross50Calls() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            tokens.add(tokenProvider.generateToken("user" + i));
        }

        assertEquals(50, tokens.size(), "All 50 generated tokens must be unique");
    }
}