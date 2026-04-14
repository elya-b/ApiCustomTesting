package elya.services;

import elya.apicontracts.IAuthApi;
import elya.credentials.ApiEmulatorCredentialsService;
import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;
import elya.emulator.constants.excpetions.TokenValidationException;
import elya.emulator.constants.formats.DataTransformer;
import elya.emulator.tokens.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static elya.emulator.constants.logs.ApiErrorLogs.INVALID_OR_MISSING_CREDENTIALS;
import static org.apache.logging.log4j.util.Strings.isBlank;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService implements IAuthApi, DataTransformer {
    private final TokenManagerService tokenManagerService;
    private final ApiEmulatorCredentialsService credentialsService;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse generateAuthToken(AuthRequest request) {
        if (request == null || isBlank(request.getLogin()) || isBlank(request.getPassword())) {
            log.error(INVALID_OR_MISSING_CREDENTIALS);
            throw new TokenValidationException(INVALID_OR_MISSING_CREDENTIALS);
        }

        boolean credentialsMatch = credentialsService.getApiEmulatorCredentials()
                .getUsers().stream()
                .anyMatch(cred -> request.getLogin().equals(cred.getLogin()) &&
                        passwordEncoder.matches(request.getPassword(), cred.getPassword()));

        if (!credentialsMatch) {
            log.error(INVALID_OR_MISSING_CREDENTIALS);
            throw new TokenValidationException(INVALID_OR_MISSING_CREDENTIALS);
        }

        String authToken = tokenProvider.generateToken(request.getLogin());
        String ttl = asString(tokenManagerService.getExpirationSeconds());
        String expires = formatToStandard(tokenManagerService.saveSession(request.getLogin(), authToken));
        String issuer = tokenManagerService.getIssuer();

        return AuthResponse.success(authToken, ttl, expires, issuer);
    }
}
