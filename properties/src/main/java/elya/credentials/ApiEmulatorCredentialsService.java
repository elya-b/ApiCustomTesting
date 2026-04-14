package elya.credentials;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ApiEmulatorCredentialsService {
    private ApiEmulatorCredentialsStructure apiEmulatorCredentials;

    public ApiEmulatorCredentialsStructure getApiEmulatorCredentials() {
        return apiEmulatorCredentials;
    }
}