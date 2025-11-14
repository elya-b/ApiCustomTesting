package elya.services;

import elya.structure.ApiEmulatorCredentialsStructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiEmulatorCredentialsService {
    private ApiEmulatorCredentialsStructure apiEmulatorCredentials;

    @Autowired
    public ApiEmulatorCredentialsService(ApiEmulatorCredentialsStructure apiEmulatorCredentials) {
        this.apiEmulatorCredentials = apiEmulatorCredentials;
    }

    public ApiEmulatorCredentialsStructure getApiEmulatorCredentials() {
        return apiEmulatorCredentials;
    }
}