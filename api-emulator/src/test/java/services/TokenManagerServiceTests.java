package services;

import elya.emulator.constants.excpetions.TokenValidationException;
import elya.emulator.objects.TokenRecord;
import elya.repository.SessionRepository;
import elya.services.TokenManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static elya.emulator.constants.excpetions.ExceptionMessage.AUTH_TOKEN_IS_EXPIRED;
import static elya.emulator.constants.excpetions.ExceptionMessage.AUTH_TOKEN_VALIDATION_FAILED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.argThat;

/**
 * Unit tests (Mockito) for {@link elya.services.TokenManagerService}.
 * <ul>
 *   <li>{@code validateAuthToken()} — success: token exists and is not expired</li>
 *   <li>{@code validateAuthToken()} — failure: token not found in the repository</li>
 *   <li>{@code validateAuthToken()} — failure: token is expired; session is deleted</li>
 *   <li>{@code validateAuthToken()} — failure: null or empty token (parameterized)</li>
 *   <li>{@code saveSession()} — session is saved with the correct expiration time</li>
 *   <li>{@code saveSession()} — expiration=0: expiry time is close to the current moment</li>
 *   <li>{@code saveSession()} — negative expiration: expiry time is in the past</li>
 *   <li>{@code saveSession()} — TokenRecord contains the correct login</li>
 *   <li>{@code validateAuthToken()} — boundary case: token expired 1 ms ago is treated as expired</li>
 *   <li>{@code validateAuthToken()} — throws AUTH_TOKEN_VALIDATION_FAILED when token is not found</li>
 *   <li>{@code validateAuthToken()} — throws AUTH_TOKEN_IS_EXPIRED when token is expired</li>
 *   <li>{@code getExpirationSeconds()} — returns the value set via ReflectionTestUtils</li>
 *   <li>{@code getIssuer()} — returns the value set via ReflectionTestUtils</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class TokenManagerServiceTests {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private TokenManagerService tokenManagerService;

    private static final String TOKEN = "token";
    private static final String LOGIN = "login";

    @BeforeEach
    void setUp() {
        // Set the token expiration time (3600 seconds) before each test via Reflection
        ReflectionTestUtils.setField(tokenManagerService, "expirationSeconds", 3600);
    }

    // --- VALIDATION TESTS ---

    @Test
    @DisplayName("Successful. Token is found and not expired")
    void validateAuthToken_Success() {
        long futureTime = System.currentTimeMillis() + 60000; // Expires in 1 minute
        TokenRecord record = new TokenRecord(LOGIN, futureTime);

        when(sessionRepository.find(TOKEN)).thenReturn(Optional.of(record));

        assertDoesNotThrow(() -> tokenManagerService.validateAuthToken(TOKEN));
        verify(sessionRepository, never()).delete(anyString());
    }

    @Test
    @DisplayName("Failed. Token does NOT exist in repository")
    void validateAuthToken_NotFound() {
        when(sessionRepository.find(TOKEN)).thenReturn(Optional.empty());

        assertThrows(TokenValidationException.class, () -> {
            tokenManagerService.validateAuthToken(TOKEN);
        });

        verify(sessionRepository, never()).delete(anyString());
    }

    @Test
    @DisplayName("Failed. Token is expired and must be deleted")
    void validateAuthToken_Expired() {
        long pastTime = System.currentTimeMillis() - 60000; // Expired 1 minute ago
        TokenRecord record = new TokenRecord(LOGIN, pastTime);

        when(sessionRepository.find(TOKEN)).thenReturn(Optional.of(record));

        assertThrows(TokenValidationException.class, () -> {
            tokenManagerService.validateAuthToken(TOKEN);
        });
        verify(sessionRepository, times(1)).delete(TOKEN);
    }

    @ParameterizedTest(name = "[{index}] Testing with token: ''{0}''")
    @CsvSource({
            ",",    // null token
            "''"    // empty token
    })
    @DisplayName("Failed. Token is null or empty")
    void validateAuthToken_InvalidInputs(String invalidToken) {
        assertThrows(TokenValidationException.class, () -> {
            tokenManagerService.validateAuthToken(invalidToken);
        });

        verify(sessionRepository, never()).find(anyString());
    }

    // --- SESSION MANAGEMENT TESTS ---

    @Test
    @DisplayName("Successful. Session is saved with correct expiry time")
    void saveSession_Success() {
        long startTime = System.currentTimeMillis();
        long expectedExpiryMin = startTime + 3600 * 1000;

        long resultExpiry = tokenManagerService.saveSession(LOGIN, TOKEN);

        assertTrue(resultExpiry >= expectedExpiryMin,
                "Expiry time should be at least 3600 seconds from now");

        verify(sessionRepository, times(1)).save(eq(TOKEN), any(TokenRecord.class));
    }

    @Test
    @DisplayName("Failed. Session saved with zero expiration time")
    void saveSession_ZeroExpiration() {
        ReflectionTestUtils.setField(tokenManagerService, "expirationSeconds", 0);

        long now = System.currentTimeMillis();
        long resultExpiry = tokenManagerService.saveSession(LOGIN, TOKEN);

        assertTrue(resultExpiry >= now && resultExpiry <= now + 100,
                "Expiry time should be very close to current time when expiration is zero");

        verify(sessionRepository, times(1)).save(eq(TOKEN), any(TokenRecord.class));
    }

    // --- ADDITIONAL EDGE CASES ---

    @Test
    @DisplayName("Failed. Session saved with negative expiration time — expiry is in the past")
    void saveSession_NegativeExpiration() {
        ReflectionTestUtils.setField(tokenManagerService, "expirationSeconds", -3600L);

        long now = System.currentTimeMillis();
        long resultExpiry = tokenManagerService.saveSession(LOGIN, TOKEN);

        assertTrue(resultExpiry < now,
                "Expiry time must be in the past when expiration is negative");
        verify(sessionRepository, times(1)).save(eq(TOKEN), any(TokenRecord.class));
    }

    @Test
    @DisplayName("Successful. saveSession() persists TokenRecord containing correct login")
    void saveSession_PersistsCorrectLogin() {
        tokenManagerService.saveSession(LOGIN, TOKEN);

        verify(sessionRepository, times(1)).save(eq(TOKEN),
                argThat(record -> LOGIN.equals(record.login())));
    }

    @Test
    @DisplayName("Failed. Token expires exactly at current time — treated as expired (boundary)")
    void validateAuthToken_ExactBoundary_TreatedAsExpired() {
        // expiryMillis == currentTimeMillis means System.currentTimeMillis() > expiryMillis is false
        // We use a past time just 1ms before now to safely model the boundary
        long boundaryTime = System.currentTimeMillis() - 1;
        TokenRecord record = new TokenRecord(LOGIN, boundaryTime);

        when(sessionRepository.find(TOKEN)).thenReturn(Optional.of(record));

        assertThrows(TokenValidationException.class, () ->
                tokenManagerService.validateAuthToken(TOKEN)
        );
        verify(sessionRepository, times(1)).delete(TOKEN);
    }

    @Test
    @DisplayName("Failed. validateAuthToken() throws AUTH_TOKEN_VALIDATION_FAILED when token not found")
    void validateAuthToken_ThrowsCorrectMessage_WhenNotFound() {
        when(sessionRepository.find(TOKEN)).thenReturn(Optional.empty());

        TokenValidationException ex = assertThrows(TokenValidationException.class, () ->
                tokenManagerService.validateAuthToken(TOKEN)
        );

        assertEquals(AUTH_TOKEN_VALIDATION_FAILED, ex.getMessage());
    }

    @Test
    @DisplayName("Failed. validateAuthToken() throws AUTH_TOKEN_IS_EXPIRED when token is expired")
    void validateAuthToken_ThrowsCorrectMessage_WhenExpired() {
        long pastTime = System.currentTimeMillis() - 1000;
        TokenRecord record = new TokenRecord(LOGIN, pastTime);

        when(sessionRepository.find(TOKEN)).thenReturn(Optional.of(record));

        TokenValidationException ex = assertThrows(TokenValidationException.class, () ->
                tokenManagerService.validateAuthToken(TOKEN)
        );

        assertEquals(AUTH_TOKEN_IS_EXPIRED, ex.getMessage());
    }

    @Test
    @DisplayName("Successful. getExpirationSeconds() returns value set via ReflectionTestUtils")
    void getExpirationSeconds_ReturnsConfiguredValue() {
        ReflectionTestUtils.setField(tokenManagerService, "expirationSeconds", 7200L);

        assertEquals(7200L, tokenManagerService.getExpirationSeconds());
    }

    @Test
    @DisplayName("Successful. getIssuer() returns value set via ReflectionTestUtils")
    void getIssuer_ReturnsConfiguredValue() {
        ReflectionTestUtils.setField(tokenManagerService, "issuer", "test-issuer");

        assertEquals("test-issuer", tokenManagerService.getIssuer());
    }
}