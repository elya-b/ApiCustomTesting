package elya.emulator.web.auth;

import org.springframework.core.*;
import org.springframework.lang.*;
import org.springframework.web.bind.support.*;
import org.springframework.web.context.request.*;
import org.springframework.web.method.support.*;

import static elya.emulator.constants.messages.ExceptionMessages.*;
import static elya.emulator.web.auth.SessionTokenInterceptor.*;

/**
 * Injects the authenticated session token into controller parameters marked with {@link SessionToken}.
 * <p>Only reads the token that {@link SessionTokenInterceptor} has already validated; it never
 * authenticates on its own. A missing token means the path is not covered by the interceptor,
 * which is a server misconfiguration rather than a client error.</p>
 */
public class SessionTokenArgumentResolver implements HandlerMethodArgumentResolver {

    /**
     * Supports {@code String} parameters annotated with {@link SessionToken}.
     *
     * @param parameter the controller method parameter.
     * @return {@code true} if the parameter is a {@code String} marked with {@link SessionToken}.
     */
    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.hasParameterAnnotation(SessionToken.class)
                && String.class.equals(parameter.getParameterType());
    }

    /**
     * Returns the session token validated by the interceptor for the current request.
     *
     * @param parameter     the controller method parameter.
     * @param mavContainer  the model and view container, unused.
     * @param webRequest    the current request.
     * @param binderFactory the data binder factory, unused.
     * @return the validated session token.
     * @throws IllegalStateException if the interceptor did not authenticate the request.
     */
    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
        Object token = webRequest.getAttribute(SESSION_TOKEN_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (token == null) {
            throw new IllegalStateException(
                    SESSION_TOKEN_NOT_AUTHENTICATED.formatted(parameter.getExecutable().getName()));
        }
        return token;
    }
}