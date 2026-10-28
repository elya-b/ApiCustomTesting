package elya.emulator.web.auth;

import elya.emulator.services.TokenManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static elya.constants.ApiEndpoints.URL_BANK_CARD_DATA;

/**
 * Wires session token authentication into Spring MVC.
 * <p>Registers {@link SessionTokenInterceptor} on every bank card path, so a new endpoint
 * under that path is protected by default, and registers {@link SessionTokenArgumentResolver}
 * to inject the validated token into controllers. The token endpoint stays public.</p>
 */
@Configuration
@RequiredArgsConstructor
public class SessionTokenWebConfig implements WebMvcConfigurer {

    /** Service passed to the interceptor to validate session tokens. */
    private final TokenManagerService tokenManagerService;

    /**
     * Protects the bank card endpoints with the session token interceptor.
     *
     * @param registry the interceptor registry.
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(new SessionTokenInterceptor(tokenManagerService))
                .addPathPatterns(URL_BANK_CARD_DATA, URL_BANK_CARD_DATA + "/**");
    }

    /**
     * Registers the resolver for {@link SessionToken} parameters.
     *
     * @param resolvers the list of custom argument resolvers.
     */
    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new SessionTokenArgumentResolver());
    }
}