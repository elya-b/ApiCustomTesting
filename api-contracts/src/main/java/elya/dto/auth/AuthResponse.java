package elya.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import elya.authentication.Token;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static elya.enums.responsemodel.AuthToken.SUCCESS;

/**
 * Data Transfer Object representing the standard authentication response.
 * <p>It encapsulates session metadata, including the generated token and operation status.
 * This DTO is compatible with both the internal domain model and the external API schema.</p>
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Token response container including status and payload")
public class AuthResponse {

    /**
     * Nested payload containing the specific authentication token details.
     */
    @Schema(description = "Payload containing token details")
    @JsonProperty("Data")
    private AuthResponseData data;

    /**
     * Flag indicating the overall success of the authentication request.
     */
    @Schema(description = "Indicates if the operation was successful", example = "true")
    @JsonProperty("Success")
    private Boolean success;

    /**
     * Descriptive status message (e.g., "SUCCESS" or error details).
     */
    @Schema(description = "Status message", example = "SUCCESS")
    @JsonProperty("Message")
    private String message;

    /**
     * Maps the response DTO to the internal {@link Token} domain model.
     * * @return a domain {@link Token} object if data is present, otherwise {@code null}.
     */
    public Token toDomain() {
        if (this.data == null) {
            return null;
        }
        return Token.builder()
                .token(data.getToken())
                .ttl(data.getTtl())
                .expires(data.getExpires())
                .issuer(data.getIssuer())
                .build();
    }

    /**
     * Static factory method to create a successful authentication response.
     *
     * @param token   the generated session token.
     * @param ttl     time-to-live duration for the session.
     * @param expires the expiration timestamp formatted as a string.
     * @param issuer  the identifier of the token issuing authority.
     * @return a pre-configured {@link AuthResponse} instance marked as successful.
     */
    public static AuthResponse success(String token, String ttl, String expires, String issuer) {
        AuthResponseData data = AuthResponseData.builder()
                .token(token)
                .ttl(ttl)
                .expires(expires)
                .issuer(issuer)
                .build();

        return AuthResponse.builder()
                .success(true)
                .message(SUCCESS.toString())
                .data(data)
                .build();
    }
}