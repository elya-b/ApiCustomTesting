package elya.engine.services.emulator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import elya.ApiEmulatorHttpStatusInfoGenerator;
import elya.api.*;
import elya.credentials.ApiEmulatorCredentialsService;
import elya.credentials.ApiEmulatorCredentialsStructure;
import elya.interfaces.IRestClientApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestTemplate;


@ComponentScan(basePackages = "elya")
@EnableConfigurationProperties(ApiEmulatorCredentialsStructure.class)
@Import(ApiEmulatorCredentialsService.class)
@Configuration
public class EmulatorTestConfig {

    @Bean
    public RestTemplate restTemplate() { return new RestTemplate(); }

    @Bean
    public ObjectMapper objectMapper() { return new ObjectMapper(); }

    @Bean
    public String baseUrl() { return ""; }

    @Bean
    @Primary
    public IRestClientApi testRestClientApiEngine(ApiEmulatorHttpStatusInfoGenerator statusGenerator) {
        return new RestClientApiEngine("", statusGenerator);
    }

    @Bean
    public RestClientApi restClientApi(IRestClientApi testRestClientApiEngine) {
        return new RestClientApi(
                new AuthClient(testRestClientApiEngine),
                new BankCardClient(testRestClientApiEngine),
                new MockClient(testRestClientApiEngine)
        );
    }
}
