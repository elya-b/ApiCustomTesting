package emulator.web.auth;

import elya.emulator.exceptions.TokenValidationException;
import elya.emulator.services.TokenManagerService;
import elya.emulator.web.auth.SessionTokenInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static elya.emulator.constants.messages.ExceptionMessages.AUTH_TOKEN_IS_EXPIRED;
import static elya.emulator.web.auth.SessionTokenInterceptor.SESSION_TOKEN_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Unit tests (Mockito) for {@link elya.emulator.web.auth.SessionTokenInterceptor}.
 * <ul>
 *   <li>{@code preHandle()} — a valid Bearer token is validated and exposed as a request attribute</li>
 *   <li>{@code preHandle()} — the scheme name is accepted in any case</li>
 *   <li>{@code preHandle()} — a missing header is rejected without calling the token service</li>
 *   <li>{@code preHandle()} — a non-Bearer header is rejected (parameterized)</li>
 *   <li>{@code preHandle()} — an invalid token is rejected and not exposed</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class SessionTokenInterceptorTests {

    private static final String TOKEN = "3f2b8c1e-9d4a-4f6b-8e2a-7c5d1a0b1a2b";

    @Mock
    private TokenManagerService tokenManagerService;

    private SessionTokenInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new SessionTokenInterceptor(tokenManagerService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("preHandle() - Should validate the Bearer token and expose it as a request attribute")
    void preHandle_ShouldExposeValidatedToken() {
        request.addHeader(AUTHORIZATION, "Bearer " + TOKEN);

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertTrue(proceed, "A valid token must let the request through");
        assertEquals(TOKEN, request.getAttribute(SESSION_TOKEN_ATTRIBUTE),
                "The token without the scheme prefix must be exposed to the controller");
        verify(tokenManagerService).validateAuthToken(TOKEN);
    }

    @Test
    @DisplayName("preHandle() - Should accept the scheme name in any case")
    void preHandle_ShouldAcceptSchemeInAnyCase() {
        request.addHeader(AUTHORIZATION, "bearer " + TOKEN);

        interceptor.preHandle(request, response, new Object());

        assertEquals(TOKEN, request.getAttribute(SESSION_TOKEN_ATTRIBUTE),
                "HTTP authentication schemes are case-insensitive");
    }

    @Test
    @DisplayName("preHandle() - Should reject a request without the Authorization header")
    void preHandle_ShouldReject_WhenHeaderIsMissing() {
        assertThrows(TokenValidationException.class,
                () -> interceptor.preHandle(request, response, new Object()));

        assertNull(request.getAttribute(SESSION_TOKEN_ATTRIBUTE), "No token must be exposed");
        verifyNoInteractions(tokenManagerService);
    }

    @ParameterizedTest(name = "[{index}] Testing with header: ''{0}''")
    @ValueSource(strings = {"Basic abc", "abc", "Bearerabc"})
    @DisplayName("preHandle() - Should reject a header that does not use the Bearer scheme")
    void preHandle_ShouldReject_WhenSchemeIsNotBearer(String header) {
        request.addHeader(AUTHORIZATION, header);

        assertThrows(TokenValidationException.class,
                () -> interceptor.preHandle(request, response, new Object()));

        verifyNoInteractions(tokenManagerService);
    }

    @Test
    @DisplayName("preHandle() - Should reject an invalid or expired token and not expose it")
    void preHandle_ShouldReject_WhenTokenIsInvalid() {
        request.addHeader(AUTHORIZATION, "Bearer " + TOKEN);
        doThrow(new TokenValidationException(AUTH_TOKEN_IS_EXPIRED))
                .when(tokenManagerService).validateAuthToken(TOKEN);

        assertThrows(TokenValidationException.class,
                () -> interceptor.preHandle(request, response, new Object()));

        assertNull(request.getAttribute(SESSION_TOKEN_ATTRIBUTE), "A rejected token must not be exposed");
    }
}