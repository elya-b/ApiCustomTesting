package elya.apicontracts;

import elya.dto.auth.*;

/**
 * Contract for the authentication API.
 * Defines the operation of generating session tokens based on user credentials.
 */
public interface IAuthApi {
    AuthResponse generateAuthToken(AuthRequest request);
}