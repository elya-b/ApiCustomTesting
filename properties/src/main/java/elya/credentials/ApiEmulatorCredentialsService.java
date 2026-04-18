package elya.credentials;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service responsible for providing access to the emulator's security credentials.
 * <p>Acts as a high-level wrapper around the {@link ApiEmulatorCredentialsStructure},
 * ensuring that authentication data is easily accessible for security filters
 * and validation logic across the application.</p>
 */
@AllArgsConstructor
@Service
public class ApiEmulatorCredentialsService {

    /** The underlying structure containing parsed credential data. */
    private final ApiEmulatorCredentialsStructure apiEmulatorCredentials;

    /**
     * Retrieves the complete credentials structure.
     *
     * @return the {@link ApiEmulatorCredentialsStructure} containing users and their roles.
     */
    public ApiEmulatorCredentialsStructure getApiEmulatorCredentials() {
        return apiEmulatorCredentials;
    }
}