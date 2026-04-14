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
 * Data Transfer Object for authentication response data.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Token response container")
public class AuthResponse {

    @Schema(description = "Payload containing token details")
    @JsonProperty("Data")
    private AuthResponseData data;

    @Schema(description = "Indicates if the operation was successful", example = "true")
    @JsonProperty("Success")
    private Boolean success;

    @Schema(description = "Status message", example = "SUCCESS")
    @JsonProperty("Message")
    private String message;

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

    public static AuthResponse success(String token, String ttl, String expires, String issuer) {
        AuthResponseData data = AuthResponseData.builder()
                .token(token)
                .ttl(ttl)
                .expires(expires)
                .issuer(issuer)
                .build();

        return AuthResponse.builder()
                .success(true)
                .message(SUCCESS.name())
                .data(data)
                .build();
    }
}
