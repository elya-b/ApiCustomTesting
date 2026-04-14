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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

        // Stub: When the repository finds the token, return a valid record
        when(sessionRepository.find(TOKEN)).thenReturn(Optional.of(record));

        // Assert that the method completes normally without any exceptions
        assertDoesNotThrow(() -> tokenManagerService.validateAuthToken(TOKEN));

        // Verify that the delete method was NEVER called
        verify(sessionRepository, never()).delete(anyString());
    }

    @Test
    @DisplayName("Failed. Token does NOT exist in repository")
    void validateAuthToken_NotFound() {
        // Stub: Return an empty Optional to simulate token not found
        when(sessionRepository.find(TOKEN)).thenReturn(Optional.empty());

        // Assert that TokenValidationException is thrown
        assertThrows(TokenValidationException.class, () -> {
            tokenManagerService.validateAuthToken(TOKEN);
        });

        // Verify that delete was never called for a non-existent token
        verify(sessionRepository, never()).delete(anyString());
    }

    @Test
    @DisplayName("Failed. Token is expired and must be deleted")
    void validateAuthToken_Expired() {
        long pastTime = System.currentTimeMillis() - 60000; // Expired 1 minute ago
        TokenRecord record = new TokenRecord(LOGIN, pastTime);

        // Stub: Return an expired session record
        when(sessionRepository.find(TOKEN)).thenReturn(Optional.of(record));

        // Assert that TokenValidationException is thrown due to expiration
        assertThrows(TokenValidationException.class, () -> {
            tokenManagerService.validateAuthToken(TOKEN);
        });

        // Verify that the expired token was actually deleted from the repository
        verify(sessionRepository, times(1)).delete(TOKEN);
    }

    @ParameterizedTest(name = "[{index}] Testing with token: ''{0}''")
    @CsvSource({
            ",",    // null token
            "''"    // empty token
    })
    @DisplayName("Failed. Token is null or empty")
    void validateAuthToken_InvalidInputs(String invalidToken) {
        // Assert immediate exception without repository interaction
        assertThrows(TokenValidationException.class, () -> {
            tokenManagerService.validateAuthToken(invalidToken);
        });

        // Verify that the repository was not queried for empty/null inputs
        verify(sessionRepository, never()).find(anyString());
    }

    // --- SESSION MANAGEMENT TESTS ---

    @Test
    @DisplayName("Successful. Session is saved with correct expiry time")
    void saveSession_Success() {
        // Calculate expected minimum expiry (Current Time + 3600 seconds)
        long startTime = System.currentTimeMillis();
        long expectedExpiryMin = startTime + 3600 * 1000;

        long resultExpiry = tokenManagerService.saveSession(LOGIN, TOKEN);

        // Verify the expiry time is calculated correctly
        assertTrue(resultExpiry >= expectedExpiryMin,
                "Expiry time should be at least 3600 seconds from now");

        // Verify the repository's 'save' method was called exactly once
        verify(sessionRepository, times(1)).save(eq(TOKEN), any(TokenRecord.class));
    }

    @Test
    @DisplayName("Failed. Session saved with zero expiration time")
    void saveSession_ZeroExpiration() {
        // Simulate config error where expiration is 0 seconds
        ReflectionTestUtils.setField(tokenManagerService, "expirationSeconds", 0);

        long now = System.currentTimeMillis();
        long resultExpiry = tokenManagerService.saveSession(LOGIN, TOKEN);

        // Verify the expiry is roughly equal to 'now' (allowing small execution jitter)
        // We ensure it is not scheduled into the far future
        assertTrue(resultExpiry >= now && resultExpiry <= now + 100,
                "Expiry time should be very close to current time when expiration is zero");

        verify(sessionRepository, times(1)).save(eq(TOKEN), any(TokenRecord.class));
    }
}