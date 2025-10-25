package elya;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static elya.ApiConstants.*;

@RestController
@RequiredArgsConstructor
public class ApiController {
    private final ApiHttpService service;

    @PostMapping(URL_TOKEN)
    public ResponseEntity<Map<String, String>> generateAuthToken(@RequestBody Map<String, String> request) {
        String login = request.get("login");
        String password = request.get("password");

        return ResponseEntity.ok(service.generateAuthToken(login, password));
    }

    @PostMapping(URL_BANK_CARD_MOCK_RESPONSE)
    public ResponseEntity<String> setMockResponse(@RequestBody Map<String, Object> mockResponse) {
        service.setMockedResponse(mockResponse);

        return ResponseEntity.ok("Mock response is set!");
    }

    @DeleteMapping(URL_BANK_CARD_MOCK_RESPONSE)
    public ResponseEntity<String> clearMockResponse() {
        service.clearMockedResponse();

        return ResponseEntity.ok("Mock response is cleared!");
    }

    @GetMapping(URL_BANK_CARD_DATA)
    public ResponseEntity<Map<String, Object>> getApiBankCards(@RequestHeader String authorization) {
        Map<String, Object> response = service.getApiBankCards(authorization);

        if (HttpStatus.UNAUTHORIZED.value() == ((Number) response.getOrDefault("status", 0)).intValue()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        return ResponseEntity.ok(service.getApiBankCards(authorization));
    }
}
