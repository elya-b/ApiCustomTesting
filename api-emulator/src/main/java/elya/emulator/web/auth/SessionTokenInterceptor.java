package elya.emulator.web.auth;

import elya.emulator.exceptions.*;
import elya.emulator.services.*;
import jakarta.servlet.http.*;
import lombok.*;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.*;

import static elya.constants.enums.HttpHeaderValues.*;
import static elya.emulator.constants.messages.ExceptionMessages.*;
import static org.springframework.http.HttpHeaders.*;

/**
 * Authenticates every request to a protected path before it reaches a controller.
 * <p>Extracts the Bearer token from the {@code Authorization} header, validates it through
 * {@link TokenManagerService} and exposes the clean token as a request attribute for
 * {@link SessionTokenArgumentResolver}. A rejected token surfaces as
 * {@link TokenValidationException}, which the global exception handler maps to 401.</p>
 */
@RequiredArgsConstructor
public class SessionTokenInterceptor implements HandlerInterceptor {

    /** Request attribute under which the validated session token is exposed to the web layer. */
    public static final String SESSION_TOKEN_ATTRIBUTE = SessionTokenInterceptor.class.getName() + ".token";

    /** Service that checks the token against the active sessions. */
    private final TokenManagerService tokenManagerService;

    /**
     * Validates the session token of the incoming request.
     *
     * @param request  the current HTTP request.
     * @param response the current HTTP response.
     * @param handler  the handler chosen for the request.
     * @return always {@code true}; a rejected request ends with an exception instead.
     * @throws TokenValidationException if the header is missing, not Bearer, or the token is invalid.
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String token = extractBearerToken(request.getHeader(AUTHORIZATION));
        tokenManagerService.validateAuthToken(token);
        request.setAttribute(SESSION_TOKEN_ATTRIBUTE, token);
        return true;
    }

    /**
     * Strips the Bearer scheme from the header value.
     * <p>The scheme name is matched case-insensitively, as HTTP authentication schemes are.</p>
     *
     * @param header the raw {@code Authorization} header value, may be {@code null}.
     * @return the token without the scheme prefix.
     * @throws TokenValidationException if the header is missing or uses another scheme.
     */
    private static String extractBearerToken(String header) {
        String prefix = BEARER.toString();
        if (header == null || !header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            throw new TokenValidationException(INVALID_AUTHORIZATION_HEADER);
        }
        return header.substring(prefix.length()).trim();
    }
}