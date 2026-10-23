package elya.emulator.tokens;

import elya.ApiTokenProperties;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Implementation of {@link TokenProvider} that issues signed JSON Web Tokens (HS256).
 * <p>Lifetime and issuer come from {@link ApiTokenProperties}, so the {@code exp} and
 * {@code iss} claims always match the server-side session settings.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProvider {

    private final ApiTokenProperties tokenProperties;

    /** Securely generated secret key for HS256 signing. */
    private final SecretKey key = Jwts.SIG.HS256.key().build();

    /**
     * Generates a signed JWT for the specified user login.
     * <p>The token includes the subject (login), issuer, issue date and an expiration
     * time based on the configured token lifetime.</p>
     *
     * @param login the identifier of the user (e.g., username or login).
     * @return a compact, URL-safe JWT string.
     */
    @Override
    public String generateToken(String login) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenProperties.expiration().toMillis());

        return Jwts.builder()
                .subject(login)
                .issuer(tokenProperties.issuer())
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }
}