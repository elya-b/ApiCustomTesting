package elya.services;

import elya.emulator.constants.excpetions.TokenValidationException;
import elya.emulator.objects.TokenRecord;
import elya.repository.SessionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static elya.emulator.constants.excpetions.ExceptionMessage.AUTH_TOKEN_IS_EXPIRED;
import static elya.emulator.constants.excpetions.ExceptionMessage.AUTH_TOKEN_VALIDATION_FAILED;
import static elya.emulator.constants.logs.ApiInfoLogs.TOKEN_VALIDATION_SERVICE_START;
import static elya.emulator.constants.logs.ApiInfoLogs.TOKEN_VALIDATION_SERVICE_STOP;

/**
 * Service for managing the lifecycle of authentication tokens.
 * It handles token generation, session persistence via {@link SessionRepository},
 * and rigorous validation of token expiration and existence.
 */
@Slf4j
@Getter
@RequiredArgsConstructor
@Service
public class TokenManagerService {

    private final SessionRepository sessionRepository;

    /**
     * The duration for which a token remains valid, in seconds.
     * Configured via {@code token.expiration} in application properties (default is 3600).
     */
    @Value("${token.expiration:3600}")
    private long expirationSeconds;

    /**
     * The identifier of the entity that issued the token.
     * Configured via {@code token.issuer}.
     */
    @Value("${token.issuer:api-emulator}")
    private String issuer;

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
     * Validates the provided authentication token against the active sessions.
     * <p>The validation process checks:
     * 1. If the token string is not null or empty.
     * 2. If the token exists in the repository.
     * 3. If the current system time has not exceeded the token's expiry timestamp.</p>
     * * If the token is expired, it is automatically removed from the repository.
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
        long expiryMillis = System.currentTimeMillis() + (expirationSeconds * 1000);
        sessionRepository.save(token, new TokenRecord(login, expiryMillis));
        return expiryMillis;
    }
}