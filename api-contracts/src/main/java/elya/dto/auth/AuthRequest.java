package elya.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for authentication requests containing user credentials.
 * <p>This object is used to capture login and password from the client side.
 * Fields are subject to validation to ensure non-empty values are provided
 * before processing authentication logic.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    /**
     * The unique identifier or username for the account.
     * Must not be null or contain only whitespace.
     */
    @NotBlank(message = "Invalid credentials or missing fields")
    private String login;

    /**
     * The plain-text password for the account.
     * Must not be null or contain only whitespace.
     */
    @NotBlank(message = "Invalid credentials or missing fields")
    private String password;
}