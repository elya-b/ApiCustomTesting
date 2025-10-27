package elya.credentials;

import main.java.elya.propertiesbeans.ApiEmulatorCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiEmulatorCredentialsService {
    private ApiEmulatorCredentials apiEmulatorCredentials;

    @Autowired
    public ApiEmulatorCredentialsService (ApiEmulatorCredentials apiEmulatorCredentials) {
        this.apiEmulatorCredentials = apiEmulatorCredentials;
    }

    public ApiEmulatorCredentials getApiEmulatorCredentials() {
        return apiEmulatorCredentials;
    }
}
