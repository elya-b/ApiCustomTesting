package elya.emulator.tokens;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider implements TokenProvider {
    private final SecretKey key = Jwts.SIG.HS256.key().build();
    private final long validityInMilliseconds = 3600000;

    @Override
    public String generateToken(String login) {
        Date date = new Date();
        Date validity = new Date(date.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(login)
                .issuedAt(date)
                .expiration(validity)
                .signWith(key)
                .compact();
    }
}
