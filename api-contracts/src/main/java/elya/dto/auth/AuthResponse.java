package elya.dto.auth;

import com.fasterxml.jackson.annotation.*;
import elya.authentication.*;
import io.swagger.v3.oas.annotations.media.*;
import lombok.*;

import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

import static elya.enums.responsemodel.AuthToken.*;

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
     *
     * @return a domain {@link Token} object if data is present, otherwise {@code null}.
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
     * Creates a successful authentication response.
     * <p>Owns the wire format of the token fields: the lifetime is written in whole seconds and the
     * expiry as an ISO-8601 UTC instant with second precision, e.g. {@code 2026-03-07T23:00:00Z}.</p>
     *
     * @param token      the generated session token.
     * @param ttlSeconds the token lifetime in seconds.
     * @param expiresAt  the moment the token expires.
     * @param issuer     the identifier of the token issuing authority.
     * @return a pre-configured {@link AuthResponse} instance marked as successful.
     * @throws NullPointerException if {@code expiresAt} is null.
     */
    public static AuthResponse success(String token, long ttlSeconds, Instant expiresAt, String issuer) {
        Objects.requireNonNull(expiresAt, "expiresAt");
        AuthResponseData data = AuthResponseData.builder()
                .token(token)
                .ttl(String.valueOf(ttlSeconds))
                .expires(DateTimeFormatter.ISO_INSTANT.format(expiresAt.truncatedTo(ChronoUnit.SECONDS)))
                .issuer(issuer)
                .build();

        return AuthResponse.builder()
                .success(true)
                .message(SUCCESS.toString())
                .data(data)
                .build();
    }
}