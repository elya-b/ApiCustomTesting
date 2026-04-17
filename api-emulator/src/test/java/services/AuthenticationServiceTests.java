package services;

import elya.credentials.ApiEmulatorCredentialsService;
import elya.credentials.ApiEmulatorCredentialsStructure;
import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;
import elya.emulator.constants.excpetions.TokenValidationException;
import elya.emulator.tokens.TokenProvider;
import elya.services.AuthenticationService;
import elya.services.TokenManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Stream;

import static elya.enums.responsemodel.AuthToken.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests (Mockito) for {@link elya.services.AuthenticationService}.
 * <ul>
 *   <li>Successful authentication — full token generation flow</li>
 *   <li>Failure on login or password mismatch (parameterized)</li>
 *   <li>Failure on empty or null login/password (parameterized)</li>
 *   <li>Failure when the users list in configuration is empty</li>
 *   <li>Failure when the authentication request object is null</li>
 *   <li>Successful authentication with multiple users in config — only the matching user authorizes</li>
 *   <li>Response contains a non-null expires field</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)

public class AuthenticationServiceTests {
    @Mock
    private TokenManagerService tokenManagerService;
    @Mock
    private ApiEmulatorCredentialsService credentialsService;
    @Mock
    private TokenProvider tokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authService;

    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";
    private static final AuthRequest AUTH_REQUEST = new AuthRequest(LOGIN, PASSWORD);

    // --- SUCCESSFUL AUTHENTICATION ---

    @Test
    @DisplayName("Successful. Full authentication flow")
    void generateAuthToken_Success() {
        String encodedPassword = "encoded_password";
        String generatedToken = "mock-jwt-token";
        String issuer = "api-emulator";
        String ttl = "3600";

        ApiEmulatorCredentialsStructure.Credential user = new ApiEmulatorCredentialsStructure.Credential();
        user.setLogin(AUTH_REQUEST.getLogin());
        user.setPassword(encodedPassword);

        ApiEmulatorCredentialsStructure config = new ApiEmulatorCredentialsStructure();
        config.setUsers(List.of(user));

        when(credentialsService.getApiEmulatorCredentials()).thenReturn(config);
        when(passwordEncoder.matches(AUTH_REQUEST.getPassword(), encodedPassword)).thenReturn(true);
        when(tokenProvider.generateToken(AUTH_REQUEST.getLogin())).thenReturn(generatedToken);
        when(tokenManagerService.getExpirationSeconds()).thenReturn(3600L);
        when(tokenManagerService.getIssuer()).thenReturn(issuer);
        when(tokenManagerService.saveSession(AUTH_REQUEST.getLogin(), generatedToken)).thenReturn(1741644000000L);

        AuthResponse response = authService.generateAuthToken(AUTH_REQUEST);

        assertTrue(response.getSuccess());
        assertEquals(SUCCESS.toString(), response.getMessage());
        assertNotNull(response.getData(), "Data payload should not be null");
        assertEquals(generatedToken, response.getData().getToken());
        assertEquals(ttl, response.getData().getTtl());
        assertEquals(issuer, response.getData().getIssuer());

        verify(tokenManagerService, times(1))
                .saveSession(eq(AUTH_REQUEST.getLogin()), eq(generatedToken));
    }

    // --- INPUT VALIDATION FAILURES ---

    @ParameterizedTest(name = "[{index}] inputLogin={0}, configLogin={2}")
    @MethodSource("provideMismatchData")
    @DisplayName("Failed. Auth failure due to mismatch")
    void generateAuthToken_MismatchInputs(AuthRequest authRequest, AuthRequest configAuthRequest) {

        ApiEmulatorCredentialsStructure.Credential existingUser = new ApiEmulatorCredentialsStructure.Credential();
        existingUser.setLogin(configAuthRequest.getLogin());
        existingUser.setPassword(configAuthRequest.getPassword());

        ApiEmulatorCredentialsStructure configStructure = new ApiEmulatorCredentialsStructure();
        configStructure.setUsers(List.of(existingUser));

        when(credentialsService.getApiEmulatorCredentials()).thenReturn(configStructure);

        assertThrows(TokenValidationException.class, () -> {
            authService.generateAuthToken(authRequest);
        });

        // Conditional Verification
        if (authRequest.getLogin().equals(configAuthRequest.getLogin())) {
            // If login matches, we expect exactly 1 call to encoder
            verify(passwordEncoder, times(1)).matches(anyString(), anyString());
        } else {
            // If login doesn't match, we expect 0 calls
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        verify(tokenProvider, never()).generateToken(anyString());
    }

    static Stream<Arguments> provideMismatchData() {
        return Stream.of(
                Arguments.of(
                        new AuthRequest(LOGIN, PASSWORD),
                        new AuthRequest("wrong_login", PASSWORD)
                ),

                Arguments.of(
                        new AuthRequest(LOGIN, "wrong_password"),
                        new AuthRequest(LOGIN, PASSWORD)
                )
        );
    }

    @ParameterizedTest(name = "[{index}] login=''{0}'', password=''{1}''")
    @MethodSource("provideInvalidRequests")
    @DisplayName("Failed. Invalid input: null or empty login/password")
    void generateAuthToken_EmptyOrNullInputs(AuthRequest authRequest) {
        assertThrows(TokenValidationException.class, () -> {
            authService.generateAuthToken(authRequest);
        });

        verify(credentialsService, never()).getApiEmulatorCredentials();
        verify(tokenProvider, never()).generateToken(anyString());
    }

    static Stream<AuthRequest> provideInvalidRequests() {
        return Stream.of(
                new AuthRequest(null, PASSWORD),
                new AuthRequest("", PASSWORD),
                new AuthRequest(LOGIN, null),
                new AuthRequest(LOGIN, "")
        );
    }

    @Test
    @DisplayName("Failed. Users list is empty in configuration")
    void generateAuthToken_EmptyUsersList() {
        ApiEmulatorCredentialsStructure configStructure = new ApiEmulatorCredentialsStructure();
        configStructure.setUsers(List.of());

        when(credentialsService.getApiEmulatorCredentials()).thenReturn(configStructure);

        assertThrows(TokenValidationException.class, () -> {
            authService.generateAuthToken(AUTH_REQUEST);
        });

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenProvider, never()).generateToken(anyString());
    }

    // --- ADDITIONAL EDGE CASES ---

    @Test
    @DisplayName("Failed. Null request object throws TokenValidationException")
    void generateAuthToken_NullRequest_ShouldThrow() {
        assertThrows(TokenValidationException.class, () ->
                authService.generateAuthToken(null)
        );

        verify(credentialsService, never()).getApiEmulatorCredentials();
        verify(tokenProvider, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Successful. Multiple users in config — only the matching one authorises login")
    void generateAuthToken_MultipleUsers_OnlyMatchingOneAuthorises() {
        String encodedPassword = "encoded_password";

        ApiEmulatorCredentialsStructure.Credential wrongUser = new ApiEmulatorCredentialsStructure.Credential();
        wrongUser.setLogin("other_login");
        wrongUser.setPassword("other_password");

        ApiEmulatorCredentialsStructure.Credential correctUser = new ApiEmulatorCredentialsStructure.Credential();
        correctUser.setLogin(LOGIN);
        correctUser.setPassword(encodedPassword);

        ApiEmulatorCredentialsStructure config = new ApiEmulatorCredentialsStructure();
        config.setUsers(List.of(wrongUser, correctUser));

        when(credentialsService.getApiEmulatorCredentials()).thenReturn(config);
        // Mockito returns false by default for unstubbed calls, so we only stub what returns true
        when(passwordEncoder.matches(PASSWORD, encodedPassword)).thenReturn(true);
        when(tokenProvider.generateToken(LOGIN)).thenReturn("mock-token");
        when(tokenManagerService.getExpirationSeconds()).thenReturn(3600L);
        when(tokenManagerService.getIssuer()).thenReturn("api-emulator");
        when(tokenManagerService.saveSession(LOGIN, "mock-token")).thenReturn(1741644000000L);

        AuthResponse response = authService.generateAuthToken(AUTH_REQUEST);

        assertTrue(response.getSuccess());
        verify(tokenProvider, times(1)).generateToken(LOGIN);
    }

    @Test
    @DisplayName("Successful. Response contains non-null expires field")
    void generateAuthToken_ResponseContainsNonNullExpires() {
        String encodedPassword = "encoded_password";

        ApiEmulatorCredentialsStructure.Credential user = new ApiEmulatorCredentialsStructure.Credential();
        user.setLogin(LOGIN);
        user.setPassword(encodedPassword);

        ApiEmulatorCredentialsStructure config = new ApiEmulatorCredentialsStructure();
        config.setUsers(List.of(user));

        when(credentialsService.getApiEmulatorCredentials()).thenReturn(config);
        when(passwordEncoder.matches(PASSWORD, encodedPassword)).thenReturn(true);
        when(tokenProvider.generateToken(LOGIN)).thenReturn("mock-token");
        when(tokenManagerService.getExpirationSeconds()).thenReturn(3600L);
        when(tokenManagerService.getIssuer()).thenReturn("api-emulator");
        when(tokenManagerService.saveSession(LOGIN, "mock-token")).thenReturn(1741644000000L);

        AuthResponse response = authService.generateAuthToken(AUTH_REQUEST);

        assertNotNull(response.getData().getExpires(),
                "Expires field must be populated from saveSession return value");
    }
}