package elya.emulator.services;

import elya.ApiTokenProperties;
import elya.emulator.exceptions.TokenValidationException;
import elya.emulator.objects.TokenRecord;
import elya.emulator.repository.SessionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static elya.emulator.constants.logs.EmulatorInfoLogs.TOKEN_VALIDATION_SERVICE_START;
import static elya.emulator.constants.logs.EmulatorInfoLogs.TOKEN_VALIDATION_SERVICE_STOP;
import static elya.emulator.constants.messages.ExceptionMessages.AUTH_TOKEN_IS_EXPIRED;
import static elya.emulator.constants.messages.ExceptionMessages.AUTH_TOKEN_VALIDATION_FAILED;

/**
 * Service for managing the lifecycle of authentication tokens.
 * <p>Persists sessions via {@link SessionRepository} and validates token existence and expiry.
 * Token lifetime and issuer come from {@link ApiTokenProperties}, the single source of truth
 * for token settings.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenManagerService {

    private final SessionRepository sessionRepository;
    private final ApiTokenProperties tokenProperties;

    /**
     * Logs the service initialization status.
     */
    @PostConstruct
    public void init() {
        log.info(TOKEN_VALIDATION_SERVICE_START);
    }

    /**
     * Logs the service shutdown status.
     */
    @PreDestroy
    public void shutdown() {
        log.info(TOKEN_VALIDATION_SERVICE_STOP);
    }

    /**
     * Returns the configured token lifetime.
     *
     * @return the token lifetime in seconds.
     */
    public long getExpirationSeconds() {
        return tokenProperties.expiration().toSeconds();
    }

    /**
     * Returns the configured token issuer.
     *
     * @return the issuer identifier included in token responses.
     */
    public String getIssuer() {
        return tokenProperties.issuer();
    }

    /**
     * Validates the provided authentication token against the active sessions.
     * <p>The validation process checks:
     * 1. If the token string is not null or empty.
     * 2. If the token exists in the repository.
     * 3. If the current system time has not exceeded the token's expiry timestamp.</p>
     * <p>If the token is expired, it is automatically removed from the repository.</p>
     *
     * @param actualToken the raw token string to validate.
     * @throws TokenValidationException if the token is missing, invalid, or expired.
     */
    public void validateAuthToken(String actualToken) {
        if (actualToken == null || actualToken.isEmpty()) {
            throw new TokenValidationException(AUTH_TOKEN_VALIDATION_FAILED);
        }

        TokenRecord record = sessionRepository.find(actualToken)
                .orElseThrow(() -> new TokenValidationException(AUTH_TOKEN_VALIDATION_FAILED));

        if (System.currentTimeMillis() > record.expiryMillis()) {
            sessionRepository.delete(actualToken);
            throw new TokenValidationException(AUTH_TOKEN_IS_EXPIRED);
        }
    }

    /**
     * Registers a new user session by calculating the expiration time and saving the record.
     *
     * @param login the user login associated with the session.
     * @param token the unique session token string.
     * @return the expiration timestamp in milliseconds (epoch time).
     */
    public long saveSession(String login, String token) {
        long expiryMillis = System.currentTimeMillis() + tokenProperties.expiration().toMillis();
        sessionRepository.save(token, new TokenRecord(login, expiryMillis));
        return expiryMillis;
    }
}