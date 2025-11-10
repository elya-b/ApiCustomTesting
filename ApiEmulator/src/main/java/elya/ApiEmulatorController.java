package elya;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static elya.constants.ApiEmulatorConstants.*;

@RestController
@RequiredArgsConstructor
public class ApiEmulatorController {
    private final ApiEmulatorService service;

    @PostMapping(URL_TOKEN)
    public ResponseEntity<Map<String, String>> generateAuthToken(@RequestBody Map<String, String> request) {
        String login = request.get("login");
        String password = request.get("password");

        Map<String, String> response = service.generateAuthToken(login, password);

        if (HttpStatus.UNAUTHORIZED.value() == Integer.parseInt(response.get("status"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        return ResponseEntity.ok(response);
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
    public ResponseEntity<Map<String, Object>> getApiBankCards(@RequestHeader String token) {
        Map<String, Object> response = service.getApiBankCards(token);

        if (HttpStatus.UNAUTHORIZED.value() == ((Number) response.getOrDefault("status", 0)).intValue()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
