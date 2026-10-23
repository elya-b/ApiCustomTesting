package emulator.tokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.ApiTokenProperties;
import elya.emulator.tokens.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JwtTokenProvider}.
 * <ul>
 *   <li>{@code generateToken()} — returns a non-null, non-blank token</li>
 *   <li>{@code generateToken()} — JWT structure contains exactly 3 dot-separated parts</li>
 *   <li>{@code generateToken()} — subject claim matches the provided login</li>
 *   <li>{@code generateToken()} — token contains the "iat" (issuedAt) claim</li>
 *   <li>{@code generateToken()} — issuer claim comes from ApiTokenProperties</li>
 *   <li>{@code generateToken()} — exp - iat equals the configured lifetime (1h and 2h)</li>
 *   <li>{@code generateToken()} — two tokens for the same login generated in different seconds differ</li>
 *   <li>{@code generateToken()} — tokens for different logins differ</li>
 *   <li>{@code generateToken()} — does not throw for an empty or null login</li>
 * </ul>
 * <p>Claims are read by decoding the payload without signature verification:
 * the signing key is private to the provider instance.</p>
 */
public class JwtTokenProviderTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ISSUER = "api-emulator";
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = providerWith(ONE_HOUR);
    }

    private static JwtTokenProvider providerWith(Duration expiration) {
        return new JwtTokenProvider(new ApiTokenProperties(expiration, ISSUER));
    }

    private static JsonNode payload(String token) throws IOException {
        return MAPPER.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
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
        String[] parts = tokenProvider.generateToken("user").split("\\.");

        assertEquals(3, parts.length, "JWT must consist of 3 parts: header.payload.signature");
    }

    @Test
    @DisplayName("generateToken() - Subject should match the provided login")
    void generateToken_SubjectShouldMatchLogin() throws IOException {
        String login = "testUser";

        assertEquals(login, payload(tokenProvider.generateToken(login)).path("sub").asText());
    }

    @Test
    @DisplayName("generateToken() - Token should contain issuedAt (iat) claim")
    void generateToken_ShouldContainIssuedAtClaim() throws IOException {
        assertTrue(payload(tokenProvider.generateToken("user")).has("iat"),
                "JWT must contain 'iat' (issuedAt) claim");
    }

    @Test
    @DisplayName("generateToken() - Issuer claim should come from ApiTokenProperties")
    void generateToken_IssuerShouldComeFromProperties() throws IOException {
        assertEquals(ISSUER, payload(tokenProvider.generateToken("user")).path("iss").asText());
    }

    @Test
    @DisplayName("generateToken() - exp - iat should equal the default lifetime (1h)")
    void generateToken_LifetimeShouldBeOneHour() throws IOException {
        JsonNode claims = payload(tokenProvider.generateToken("user"));

        assertEquals(ONE_HOUR.toSeconds(), claims.path("exp").asLong() - claims.path("iat").asLong());
    }

    @Test
    @DisplayName("generateToken() - exp - iat should follow the configured lifetime (2h)")
    void generateToken_LifetimeShouldFollowConfiguration() throws IOException {
        Duration twoHours = Duration.ofHours(2);
        JsonNode claims = payload(providerWith(twoHours).generateToken("user"));

        assertEquals(twoHours.toSeconds(), claims.path("exp").asLong() - claims.path("iat").asLong(),
                "Lifetime must come from configuration, not a hardcoded constant");
    }

    @Test
    @DisplayName("generateToken() - Two tokens for same login must differ (unique iat/signature)")
    void generateToken_TwoTokensForSameLoginMustDiffer() throws InterruptedException {
        String token1 = tokenProvider.generateToken("user");
        // jjwt truncates iat/exp to whole seconds — sleep >1s to guarantee a different second
        Thread.sleep(1100);
        String token2 = tokenProvider.generateToken("user");

        assertNotEquals(token1, token2, "Tokens generated in different seconds must not be equal");
    }

    @Test
    @DisplayName("generateToken() - Tokens for different logins must differ")
    void generateToken_TokensForDifferentLoginsMustDiffer() {
        assertNotEquals(tokenProvider.generateToken("alice"), tokenProvider.generateToken("bob"),
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
        // jjwt omits the sub claim when the subject is null
        assertDoesNotThrow(() -> tokenProvider.generateToken(null),
                "Provider must not throw for null login");
    }
}