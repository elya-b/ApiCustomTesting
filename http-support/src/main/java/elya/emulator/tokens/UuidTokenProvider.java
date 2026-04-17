package elya.emulator.tokens;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementation of {@link TokenProvider} that generates simple UUID-based tokens.
 * <p>Marked as {@link Primary} to serve as the default token generation strategy.
 * This provider is ideal for lightweight sessions or testing environments where
 * the overhead of cryptographic tokens (like JWT) is not required.</p>
 */
@Primary
@Component
public class UuidTokenProvider implements TokenProvider {

    /**
     * Generates a unique, random string identifier using {@link UUID}.
     * <p>Note: Unlike JWT, this token is opaque and does not inherently
     * carry user identity or expiration data.</p>
     *
     * @param login the identifier of the user (currently unused in UUID generation).
     * @return a random 36-character UUID string.
     */
    @Override
    public String generateToken(String login) {
        return UUID.randomUUID().toString();
    }
}