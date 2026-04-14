package elya.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object containing detailed authentication information.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Details of the generated authentication token")
public class AuthResponseData {

    @Schema(description = "The session token (UUID format)", example = "a9cf409c-5db8-4d54-ab3d-5afa81d53652")
    @JsonProperty("token")
    private String token;

    @Schema(description = "Time-to-live in seconds", example = "3600")
    @JsonProperty("ttl")
    private String ttl;

    @Schema(description = "Expiration timestamp in standard format", example = "2026-03-07T23:00:00Z")
    @JsonProperty("expires")
    private String expires;

    @Schema(description = "Token issuer identifier", example = "api-emulator")
    @JsonProperty("issuer")
    private String issuer;
}
