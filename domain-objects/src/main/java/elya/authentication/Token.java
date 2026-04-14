package elya.authentication;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class Token {
    private String token;
    private String ttl;
    private String expires;
    private String issuer;
}
