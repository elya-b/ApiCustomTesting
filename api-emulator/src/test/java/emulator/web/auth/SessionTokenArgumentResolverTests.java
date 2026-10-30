package emulator.web.auth;

import elya.emulator.web.auth.*;
import org.junit.jupiter.api.*;
import org.springframework.core.*;
import org.springframework.mock.web.*;
import org.springframework.web.context.request.*;

import static elya.emulator.web.auth.SessionTokenInterceptor.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link elya.emulator.web.auth.SessionTokenArgumentResolver}.
 * <ul>
 *   <li>{@code supportsParameter()} — accepts a {@code String} parameter marked with {@code @SessionToken}</li>
 *   <li>{@code supportsParameter()} — ignores a parameter without the annotation</li>
 *   <li>{@code supportsParameter()} — ignores an annotated parameter that is not a {@code String}</li>
 *   <li>{@code resolveArgument()} — returns the token exposed by the interceptor</li>
 *   <li>{@code resolveArgument()} — fails when the interceptor did not authenticate the request</li>
 * </ul>
 */
public class SessionTokenArgumentResolverTests {

    private static final String TOKEN = "3f2b8c1e-9d4a-4f6b-8e2a-7c5d1a0b1a2b";

    private final SessionTokenArgumentResolver resolver = new SessionTokenArgumentResolver();

    @Test
    @DisplayName("supportsParameter() - Should accept a String parameter marked with @SessionToken")
    void supportsParameter_ShouldAcceptAnnotatedString() throws NoSuchMethodException {
        MethodParameter parameter = parameter("annotated", String.class);

        assertTrue(resolver.supportsParameter(parameter), "An annotated String must be resolved");
    }

    @Test
    @DisplayName("supportsParameter() - Should ignore a parameter without @SessionToken")
    void supportsParameter_ShouldIgnoreParameterWithoutAnnotation() throws NoSuchMethodException {
        MethodParameter parameter = parameter("plain", String.class);

        assertFalse(resolver.supportsParameter(parameter), "A plain String must be left to other resolvers");
    }

    @Test
    @DisplayName("supportsParameter() - Should ignore an annotated parameter that is not a String")
    void supportsParameter_ShouldIgnoreAnnotatedNonString() throws NoSuchMethodException {
        MethodParameter parameter = parameter("wrongType", Long.class);

        assertFalse(resolver.supportsParameter(parameter), "Only String parameters can hold the token");
    }

    @Test
    @DisplayName("resolveArgument() - Should return the token exposed by the interceptor")
    void resolveArgument_ShouldReturnAuthenticatedToken() throws NoSuchMethodException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(SESSION_TOKEN_ATTRIBUTE, TOKEN);

        Object resolved = resolver.resolveArgument(
                parameter("annotated", String.class), null, new ServletWebRequest(request), null);

        assertEquals(TOKEN, resolved, "The validated token must be injected unchanged");
    }

    @Test
    @DisplayName("resolveArgument() - Should fail when the interceptor did not authenticate the request")
    void resolveArgument_ShouldFail_WhenRequestIsNotAuthenticated() throws NoSuchMethodException {
        ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());
        MethodParameter parameter = parameter("annotated", String.class);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> resolver.resolveArgument(parameter, null, webRequest, null));

        assertTrue(exception.getMessage().contains("annotated()"),
                "The message must name the handler that is not covered by the interceptor");
    }

    private static MethodParameter parameter(String methodName, Class<?> type) throws NoSuchMethodException {
        return new MethodParameter(SampleHandler.class.getDeclaredMethod(methodName, type), 0);
    }

    /** Handler methods that only serve as parameter metadata for the resolver. */
    @SuppressWarnings("unused")
    private static class SampleHandler {
        void annotated(@SessionToken String token) {
        }

        void plain(String token) {
        }

        void wrongType(@SessionToken Long token) {
        }
    }
}