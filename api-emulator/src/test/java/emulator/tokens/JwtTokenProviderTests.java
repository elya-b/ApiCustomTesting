package emulator.tokens;

import elya.emulator.tokens.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for {@link elya.emulator.tokens.JwtTokenProvider}.
 * <ul>
 *   <li>{@code generateToken()} — returns a non-null, non-blank token</li>
 *   <li>{@code generateToken()} — JWT structure contains exactly 3 dot-separated parts</li>
 *   <li>{@code generateToken()} — subject claim matches the provided login</li>
 *   <li>{@code generateToken()} — token contains the "iat" (issuedAt) claim</li>
 *   <li>{@code generateToken()} — token contains the "exp" (expiration) claim</li>
 *   <li>{@code generateToken()} — expiration is approximately 1 hour from generation time</li>
 *   <li>{@code generateToken()} — two tokens for the same login generated in different seconds differ</li>
 *   <li>{@code generateToken()} — tokens for different logins differ</li>
 *   <li>{@code generateToken()} — does not throw for an empty login string</li>
 *   <li>{@code generateToken()} — does not throw for a null login</li>
 * </ul>
 */
public class JwtTokenProviderTests {
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
    }

    @Test
    @DisplayName("generateToken() - Should return non-null, non-blank token")
    void generateToken_ShouldReturnNonNullToken() {
        String token = tokenProvider.generateToken("user");

        assertNotNull(token, "Token must not be null");
        assertFalse(token.isBlank(), "Token must not be blank");
    }

    @Test
    @DisplayName("generateToken() - Should return a valid JWT structure (3 parts separated by '.')")
    void generateToken_ShouldReturnValidJwtStructure() {
        String token = tokenProvider.generateToken("user");
        String[] parts = token.split("\\.");

        assertEquals(3, parts.length,
                "JWT must consist of 3 parts: header.payload.signature");
    }

    @Test
    @DisplayName("generateToken() - Subject should match the provided login")
    void generateToken_SubjectShouldMatchLogin() {
        String login = "testUser";
        String token = tokenProvider.generateToken(login);

        // The provider generates a new key each time it's instantiated,
        // so we parse with the same provider instance's key is not directly accessible.
        // We verify the claim by decoding the payload part (Base64) without signature verification.
        String payload = new String(java.util.Base64.getUrlDecoder()
                .decode(token.split("\\.")[1]));

        assertTrue(payload.contains("\"sub\":\"" + login + "\""),
                "JWT payload must contain the subject equal to login");
    }

    @Test
    @DisplayName("generateToken() - Token should contain issuedAt (iat) claim")
    void generateToken_ShouldContainIssuedAtClaim() {
        String token = tokenProvider.generateToken("user");

        String payload = new String(java.util.Base64.getUrlDecoder()
                .decode(token.split("\\.")[1]));

        assertTrue(payload.contains("\"iat\""),
                "JWT must contain 'iat' (issuedAt) claim");
    }

    @Test
    @DisplayName("generateToken() - Token should contain expiration (exp) claim")
    void generateToken_ShouldContainExpirationClaim() {
        String token = tokenProvider.generateToken("user");

        String payload = new String(java.util.Base64.getUrlDecoder()
                .decode(token.split("\\.")[1]));

        assertTrue(payload.contains("\"exp\""),
                "JWT must contain 'exp' (expiration) claim");
    }

    @Test
    @DisplayName("generateToken() - Expiration should be approximately 1 hour from now")
    void generateToken_ExpirationShouldBeApproximatelyOneHourFromNow() {
        long before = System.currentTimeMillis();
        String token = tokenProvider.generateToken("user");
        long after = System.currentTimeMillis();

        String payload = new String(java.util.Base64.getUrlDecoder()
                .decode(token.split("\\.")[1]));

        int expIdx = payload.indexOf("\"exp\":");
        assertNotEquals(-1, expIdx, "exp claim must be present");

        int start = expIdx + 6;
        int end = payload.indexOf(",", start);
        if (end == -1) end = payload.indexOf("}", start);
        long expSeconds = Long.parseLong(payload.substring(start, end).trim());
        long expMillis = expSeconds * 1000L;

        // jjwt truncates timestamps to whole seconds, so expMillis may be up to 999ms
        // less than expectedMin (which is based on before millis). We allow 1s tolerance downward.
        long expectedMin = before + 3600_000L - 1000L;
        long expectedMax = after  + 3600_000L + 1000L;

        assertTrue(expMillis >= expectedMin && expMillis <= expectedMax,
                "Expiration must be within 1 hour from token generation time " +
                        "(got " + expMillis + ", expected [" + expectedMin + ".." + expectedMax + "])");
    }

    @Test
    @DisplayName("generateToken() - Two tokens for same login must differ (unique iat/signature)")
    void generateToken_TwoTokensForSameLoginMustDiffer() throws InterruptedException {
        String token1 = tokenProvider.generateToken("user");
        // jjwt truncates iat/exp to whole seconds — sleep >1s to guarantee a different second
        Thread.sleep(1100);
        String token2 = tokenProvider.generateToken("user");

        assertNotEquals(token1, token2,
                "Tokens generated in different seconds must not be equal");
    }

    @Test
    @DisplayName("generateToken() - Tokens for different logins must differ")
    void generateToken_TokensForDifferentLoginsMustDiffer() {
        String token1 = tokenProvider.generateToken("alice");
        String token2 = tokenProvider.generateToken("bob");

        assertNotEquals(token1, token2,
                "Tokens for different users must not be equal");
    }

    @Test
    @DisplayName("generateToken() - Should handle empty string login without throwing")
    void generateToken_ShouldHandleEmptyLogin() {
        assertDoesNotThrow(() -> tokenProvider.generateToken(""),
                "Provider must not throw for empty login string");
    }

    @Test
    @DisplayName("generateToken() - Should handle null login without throwing")
    void generateToken_ShouldHandleNullLogin() {
        // JwtTokenProvider does not validate login — it passes it as subject.
        // Jwts.builder().subject(null) in jjwt produces a token without the sub claim.
        assertDoesNotThrow(() -> tokenProvider.generateToken(null),
                "Provider must not throw for null login");
    }
}