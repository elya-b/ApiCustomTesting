package elya.authentication;

import lombok.*;

/**
 * Domain model representing an authentication session token.
 * <p>This class serves as the internal representation of security credentials within the application core,
 * independent of the API layer's Data Transfer Objects.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class Token {

    /**
     * The unique session identifier string.
     */
    private String token;

    /**
     * The duration of the token's validity, typically expressed as a human-readable duration or seconds.
     */
    private String ttl;

    /**
     * The absolute timestamp when this token becomes invalid.
     */
    private String expires;

    /**
     * The name of the authority that generated and issued this token.
     */
    private String issuer;
}