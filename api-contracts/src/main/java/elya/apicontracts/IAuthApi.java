package elya.apicontracts;

import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;

/**
 * Contract for the authentication API.
 * Defines the operation of generating session tokens based on user credentials.
 */
public interface IAuthApi {
    AuthResponse generateAuthToken(AuthRequest request);
}