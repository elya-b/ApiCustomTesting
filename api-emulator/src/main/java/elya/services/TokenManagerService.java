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

@Slf4j
@Getter
@RequiredArgsConstructor
@Service
public class TokenManagerService {
    private final SessionRepository sessionRepository;
    /**
     * picks value from yml; fallback default == 3600
     */
    @Value("${token.expiration:3600}")
    private long expirationSeconds;

    @Value("${token.issuer:api-emulator}")
    private String issuer;

    @PostConstruct
    public void init() {
        log.info(TOKEN_VALIDATION_SERVICE_START);
    }

    @PreDestroy
    public void shutdown() {
        log.info(TOKEN_VALIDATION_SERVICE_STOP);
    }

    /**
     * Validates if the token exists and has not expired.
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
     * Registers a new session.
     * @param login user login
     * @param token unique session token
     * @return expiryMillis for response formatting
     */
    public long saveSession(String login, String token) {
        long expiryMillis = System.currentTimeMillis() + (expirationSeconds * 1000);
        sessionRepository.save(token, new TokenRecord(login, expiryMillis));
        return expiryMillis;
    }
}
