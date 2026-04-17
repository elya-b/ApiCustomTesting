package elya.emulator.tokens;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Implementation of {@link TokenProvider} that uses JSON Web Tokens (JWT)
 * for session management.
 * <p>Handles the generation of secure, signed tokens containing the user's
 * identity and an expiration timestamp. Uses the HS256 signature algorithm.</p>
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    /** Default token validity period set to 1 hour (in milliseconds). */
    private static final long VALIDITY_IN_MILLISECONDS = 3600000;

    /** Securely generated secret key for HS256 signing. */
    private final SecretKey key = Jwts.SIG.HS256.key().build();

    /**
     * Generates a signed JWT for the specified user login.
     * <p>The token includes the subject (login), issue date, and an
     * expiration time based on the predefined validity period.</p>
     *
     * @param login the identifier of the user (e.g., username or login).
     * @return a compact, URL-safe JWT string.
     */
    @Override
    public String generateToken(String login) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + VALIDITY_IN_MILLISECONDS);

        return Jwts.builder()
                .subject(login)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }
}