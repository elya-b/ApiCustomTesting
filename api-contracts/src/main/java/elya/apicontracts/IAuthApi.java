package elya.apicontracts;

import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;

public interface IAuthApi {
    AuthResponse generateAuthToken(AuthRequest request);
}
