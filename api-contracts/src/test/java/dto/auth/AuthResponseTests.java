package dto.auth;

import elya.dto.auth.*;
import org.junit.jupiter.api.*;

import java.time.*;

import static elya.enums.responsemodel.AuthToken.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AuthResponse}.
 * <ul>
 *   <li>{@code success()} — marks the response successful and carries the token and issuer</li>
 *   <li>{@code success()} — writes the lifetime in seconds</li>
 *   <li>{@code success()} — writes the expiry as an ISO-8601 UTC instant</li>
 *   <li>{@code success()} — drops the fraction of a second from the expiry</li>
 *   <li>{@code success()} — rejects a missing expiry</li>
 * </ul>
 */
public class AuthResponseTests {

    private static final String TOKEN = "token";
    private static final String ISSUER = "issuer";
    private static final long TTL_SECONDS = 3600;
    private static final Instant EXPIRES_AT = Instant.parse("2026-03-07T23:00:00Z");
    private static final String EXPIRES_AT_TEXT = "2026-03-07T23:00:00Z";

    @Test
    @DisplayName("success() - Should mark the response successful and carry the token and issuer")
    void success_ShouldCarryTokenAndIssuer() {
        AuthResponse response = AuthResponse.success(TOKEN, TTL_SECONDS, EXPIRES_AT, ISSUER);

        assertAll("Successful response",
                () -> assertTrue(response.getSuccess(), "Success flag"),
                () -> assertEquals(SUCCESS.toString(), response.getMessage(), "Message"),
                () -> assertEquals(TOKEN, response.getData().getToken(), "Token"),
                () -> assertEquals(ISSUER, response.getData().getIssuer(), "Issuer"));
    }

    @Test
    @DisplayName("success() - Should write the lifetime in seconds")
    void success_ShouldWriteTtlInSeconds() {
        AuthResponse response = AuthResponse.success(TOKEN, TTL_SECONDS, EXPIRES_AT, ISSUER);

        assertEquals("3600", response.getData().getTtl());
    }

    @Test
    @DisplayName("success() - Should write the expiry as an ISO-8601 UTC instant")
    void success_ShouldWriteExpiresAsIsoUtcInstant() {
        AuthResponse response = AuthResponse.success(TOKEN, TTL_SECONDS, EXPIRES_AT, ISSUER);

        assertEquals(EXPIRES_AT_TEXT, response.getData().getExpires());
    }

    @Test
    @DisplayName("success() - Should drop the fraction of a second from the expiry")
    void success_ShouldDropFractionOfSecond() {
        AuthResponse response = AuthResponse.success(TOKEN, TTL_SECONDS, EXPIRES_AT.plusMillis(437), ISSUER);

        assertEquals(EXPIRES_AT_TEXT, response.getData().getExpires(),
                "Expiry must have second precision, so its format does not change with the milliseconds");
    }

    @Test
    @DisplayName("success() - Should reject a missing expiry")
    void success_ShouldRejectNullExpiry() {
        assertThrows(NullPointerException.class,
                () -> AuthResponse.success(TOKEN, TTL_SECONDS, null, ISSUER));
    }
}