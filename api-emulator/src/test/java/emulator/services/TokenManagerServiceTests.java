package emulator.services;

import elya.emulator.config.*;
import elya.emulator.exceptions.*;
import elya.emulator.objects.*;
import elya.emulator.repository.*;
import elya.emulator.services.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.time.*;
import java.util.*;

import static elya.emulator.constants.messages.ExceptionMessages.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests (Mockito) for {@link TokenManagerService}.
 * <ul>
 *   <li>{@code validateAuthToken()} — success: token exists and is not expired</li>
 *   <li>{@code validateAuthToken()} — failure: token not found in the repository</li>
 *   <li>{@code validateAuthToken()} — failure: token is expired; session is deleted</li>
 *   <li>{@code validateAuthToken()} — failure: null or empty token (parameterized)</li>
 *   <li>{@code validateAuthToken()} — boundary case: token expired 1 ms ago is treated as expired</li>
 *   <li>{@code validateAuthToken()} — exception messages for not found / expired</li>
 *   <li>{@code saveSession()} — expiry is one configured lifetime from now</li>
 *   <li>{@code saveSession()} — a different configured lifetime changes the expiry</li>
 *   <li>{@code saveSession()} — persisted record carries the returned expiry and the login</li>
 *   <li>{@code getExpirationSeconds()} / {@code getIssuer()} — reflect ApiTokenProperties</li>
 * </ul>
 * <p>Invalid lifetimes (zero, negative) cannot reach this service: {@link ApiTokenProperties}
 * rejects them at startup and is tested separately.</p>
 */
@ExtendWith(MockitoExtension.class)
public class TokenManagerServiceTests {

    private static final String TOKEN = "token";
    private static final String LOGIN = "login";
    private static final String ISSUER = "api-emulator";
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    @Mock
    private SessionRepository sessionRepository;

    private TokenManagerService tokenManagerService;

    @BeforeEach
    void setUp() {
        tokenManagerService = serviceWith(ONE_HOUR);
    }

    private TokenManagerService serviceWith(Duration expiration) {
        return new TokenManagerService(sessionRepository, new ApiTokenProperties(expiration, ISSUER));
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

        assertThrows(TokenValidationException.class,
                () -> tokenManagerService.validateAuthToken(TOKEN));

        verify(sessionRepository, never()).delete(anyString());
    }

    @Test
    @DisplayName("Failed. Token is expired and must be deleted")
    void validateAuthToken_Expired() {
        long pastTime = System.currentTimeMillis() - 60000; // Expired 1 minute ago
        TokenRecord record = new TokenRecord(LOGIN, pastTime);

        when(sessionRepository.find(TOKEN)).thenReturn(Optional.of(record));

        assertThrows(TokenValidationException.class,
                () -> tokenManagerService.validateAuthToken(TOKEN));
        verify(sessionRepository, times(1)).delete(TOKEN);
    }

    @ParameterizedTest(name = "[{index}] Testing with token: ''{0}''")
    @CsvSource({
            ",",    // null token
            "''"    // empty token
    })
    @DisplayName("Failed. Token is null or empty")
    void validateAuthToken_InvalidInputs(String invalidToken) {
        assertThrows(TokenValidationException.class,
                () -> tokenManagerService.validateAuthToken(invalidToken));

        verify(sessionRepository, never()).find(anyString());
    }

    @Test
    @DisplayName("Failed. Token expired 1 ms ago is treated as expired (boundary)")
    void validateAuthToken_ExactBoundary_TreatedAsExpired() {
        long boundaryTime = System.currentTimeMillis() - 1;
        TokenRecord record = new TokenRecord(LOGIN, boundaryTime);

        when(sessionRepository.find(TOKEN)).thenReturn(Optional.of(record));

        assertThrows(TokenValidationException.class,
                () -> tokenManagerService.validateAuthToken(TOKEN));
        verify(sessionRepository, times(1)).delete(TOKEN);
    }

    @Test
    @DisplayName("Failed. validateAuthToken() throws AUTH_TOKEN_VALIDATION_FAILED when token not found")
    void validateAuthToken_ThrowsCorrectMessage_WhenNotFound() {
        when(sessionRepository.find(TOKEN)).thenReturn(Optional.empty());

        TokenValidationException ex = assertThrows(TokenValidationException.class,
                () -> tokenManagerService.validateAuthToken(TOKEN));

        assertEquals(AUTH_TOKEN_VALIDATION_FAILED, ex.getMessage());
    }

    @Test
    @DisplayName("Failed. validateAuthToken() throws AUTH_TOKEN_IS_EXPIRED when token is expired")
    void validateAuthToken_ThrowsCorrectMessage_WhenExpired() {
        long pastTime = System.currentTimeMillis() - 1000;
        TokenRecord record = new TokenRecord(LOGIN, pastTime);

        when(sessionRepository.find(TOKEN)).thenReturn(Optional.of(record));

        TokenValidationException ex = assertThrows(TokenValidationException.class,
                () -> tokenManagerService.validateAuthToken(TOKEN));

        assertEquals(AUTH_TOKEN_IS_EXPIRED, ex.getMessage());
    }

    // --- SESSION MANAGEMENT TESTS ---

    @Test
    @DisplayName("Successful. Session expiry is one configured lifetime from now")
    void saveSession_Success() {
        long before = System.currentTimeMillis();
        long resultExpiry = tokenManagerService.saveSession(LOGIN, TOKEN);
        long after = System.currentTimeMillis();

        long lifetime = ONE_HOUR.toMillis();
        assertTrue(resultExpiry >= before + lifetime && resultExpiry <= after + lifetime,
                "Expiry must be exactly one configured lifetime after the call");
        verify(sessionRepository, times(1)).save(eq(TOKEN), any(TokenRecord.class));
    }

    @Test
    @DisplayName("Successful. A different configured lifetime changes the session expiry")
    void saveSession_UsesConfiguredLifetime() {
        Duration ninetyMinutes = Duration.ofMinutes(90);
        TokenManagerService service = serviceWith(ninetyMinutes);

        long before = System.currentTimeMillis();
        long resultExpiry = service.saveSession(LOGIN, TOKEN);
        long after = System.currentTimeMillis();

        long lifetime = ninetyMinutes.toMillis();
        assertTrue(resultExpiry >= before + lifetime && resultExpiry <= after + lifetime,
                "Expiry must follow the configured lifetime, not a hardcoded value");
    }

    @Test
    @DisplayName("Successful. Persisted record carries the same expiry that is returned")
    void saveSession_PersistsReturnedExpiry() {
        ArgumentCaptor<TokenRecord> captor = ArgumentCaptor.forClass(TokenRecord.class);

        long resultExpiry = tokenManagerService.saveSession(LOGIN, TOKEN);

        verify(sessionRepository).save(eq(TOKEN), captor.capture());
        assertEquals(resultExpiry, captor.getValue().expiryMillis());
    }

    @Test
    @DisplayName("Successful. saveSession() persists TokenRecord containing correct login")
    void saveSession_PersistsCorrectLogin() {
        tokenManagerService.saveSession(LOGIN, TOKEN);

        verify(sessionRepository, times(1)).save(eq(TOKEN),
                argThat(record -> LOGIN.equals(record.login())));
    }

    // --- CONFIGURATION ACCESSORS ---

    @Test
    @DisplayName("Successful. getExpirationSeconds() returns the configured lifetime in seconds")
    void getExpirationSeconds_ReturnsConfiguredValue() {
        assertEquals(7200L, serviceWith(Duration.ofHours(2)).getExpirationSeconds());
    }

    @Test
    @DisplayName("Successful. getIssuer() returns the configured issuer")
    void getIssuer_ReturnsConfiguredValue() {
        TokenManagerService service = new TokenManagerService(sessionRepository,
                new ApiTokenProperties(ONE_HOUR, "test-issuer"));

        assertEquals("test-issuer", service.getIssuer());
    }
}