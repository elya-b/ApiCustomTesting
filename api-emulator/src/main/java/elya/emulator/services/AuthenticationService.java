package elya.emulator.services;

import elya.apicontracts.*;
import elya.credentials.*;
import elya.dto.auth.*;
import elya.emulator.constants.formats.*;
import elya.emulator.exceptions.*;
import elya.emulator.tokens.*;
import lombok.*;
import org.springframework.security.crypto.password.*;
import org.springframework.stereotype.*;
import org.springframework.util.*;

import static elya.emulator.constants.messages.ExceptionMessages.*;

/**
 * Service responsible for user authentication and session token issuance.
 * Coordinates credential verification using a {@link PasswordEncoder}
 * and delegates token lifecycle management to the {@link TokenManagerService}.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthApi, DataTransformer {
    private final TokenManagerService tokenManagerService;
    private final ApiEmulatorCredentialsService credentialsService;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticates a user based on the provided credentials and returns a secure session token.
     * <p>The process involves:
     * 1. Validating that login and password are not blank.
     * 2. Verifying the credentials against the pre-configured user list.
     * 3. Generating a unique token with specific TTL and expiration metadata.</p>
     *
     * @param request the {@link AuthRequest} containing the user's login and plain-text password.
     * @return an {@link AuthResponse} containing the generated token and its expiration details.
     * @throws TokenValidationException if credentials are null, empty, or do not match any known user.
     */
    @Override
    public AuthResponse generateAuthToken(AuthRequest request) {
        if (request == null ||
                !StringUtils.hasText(request.getLogin()) || !StringUtils.hasText(request.getPassword())) {
            throw new TokenValidationException(INVALID_OR_MISSING_CREDENTIALS);
        }

        boolean credentialsMatch = credentialsService.getApiEmulatorCredentials()
                .getUsers().stream()
                .anyMatch(cred -> request.getLogin().equals(cred.getLogin()) &&
                        passwordEncoder.matches(request.getPassword(), cred.getPassword()));

        if (!credentialsMatch) {
            throw new TokenValidationException(INVALID_OR_MISSING_CREDENTIALS);
        }

        String authToken = tokenProvider.generateToken(request.getLogin());
        String ttl = asString(tokenManagerService.getExpirationSeconds());
        String expires = formatToStandard(tokenManagerService.saveSession(request.getLogin(), authToken));
        String issuer = tokenManagerService.getIssuer();

        return AuthResponse.success(authToken, ttl, expires, issuer);
    }
}