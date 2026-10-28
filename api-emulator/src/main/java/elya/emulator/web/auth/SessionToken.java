package elya.emulator.web.auth;

import java.lang.annotation.*;

/**
 * Marks a controller parameter that receives the authenticated session token.
 * <p>The token is resolved by {@link SessionTokenArgumentResolver} from the request attribute
 * set by {@link SessionTokenInterceptor}, so the parameter always holds a validated token
 * without the {@code Bearer} prefix.</p>
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface SessionToken {
}