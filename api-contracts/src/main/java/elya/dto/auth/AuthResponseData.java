package elya.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object containing the specific details of a generated authentication session.
 * <p>This class encapsulates the token string, its lifespan, expiration metadata,
 * and the issuing authority information.</p>
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Details of the generated authentication token and session metadata")
public class AuthResponseData {

    /**
     * The unique session identifier, typically provided in UUID format.
     * This token must be used as a Bearer string in subsequent API calls.
     */
    @Schema(description = "The session token (UUID format)", example = "a9cf409c-5db8-4d54-ab3d-5afa81d53652")
    @JsonProperty("token")
    private String token;

    /**
     * The duration for which the token remains valid, represented in seconds.
     */
    @Schema(description = "Time-to-live in seconds", example = "3600")
    @JsonProperty("ttl")
    private String ttl;

    /**
     * The exact date and time when the token becomes invalid.
     * Expressed in ISO 8601 standard format (UTC).
     */
    @Schema(description = "Expiration timestamp in standard format", example = "2026-03-07T23:00:00Z")
    @JsonProperty("expires")
    private String expires;

    /**
     * The name or identifier of the service that issued the authentication credentials.
     */
    @Schema(description = "Token issuer identifier", example = "api-emulator")
    @JsonProperty("issuer")
    private String issuer;
}