package elya.emulator.tokens;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Primary
@Component
public class UuidTokenProvider implements TokenProvider {
    @Override
    public String generateToken(String login){
        return UUID.randomUUID().toString();
    }
}
