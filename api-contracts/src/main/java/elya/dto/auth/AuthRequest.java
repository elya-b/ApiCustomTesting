package elya.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for authentication request containing user credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    @NotBlank(message = "Invalid credentials or missing fields")
    private String login;

    @NotBlank(message = "Invalid credentials or missing fields")
    private String password;
}
