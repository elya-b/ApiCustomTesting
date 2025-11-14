package elya;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static elya.emulator.constants.messages.ResponseMessages.*;
import static elya.general.constants.ApiEndpoints.*;
import static elya.general.enums.JsonProperty.*;
import static elya.general.enums.StatusInfo.*;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequiredArgsConstructor
public class ApiEmulatorController {
    private final ApiEmulatorService service;

    @PostMapping(URL_TOKEN)
    public ResponseEntity<Map<String, String>> generateAuthToken(@RequestBody Map<String, String> request) {
        String login = request.get(LOGIN.toString());
        String password = request.get(PASSWORD.toString());

        Map<String, String> response = service.generateAuthToken(login, password);

        if (UNAUTHORIZED.value() == Integer.parseInt(response.get(STATUS.toString()))) {
            return ResponseEntity.status(UNAUTHORIZED).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping(URL_BANK_CARD_MOCK_RESPONSE)
    public ResponseEntity<String> setMockResponse(@RequestBody Map<String, Object> mockResponse) {
        service.setMockedResponse(mockResponse);

        return ResponseEntity.ok(MOCK_SET);
    }

    @DeleteMapping(URL_BANK_CARD_MOCK_RESPONSE)
    public ResponseEntity<String> clearMockResponse() {
        service.clearMockedResponse();

        return ResponseEntity.ok(MOCK_CLEARED);
    }

    @GetMapping(URL_BANK_CARD_DATA)
    public ResponseEntity<Map<String, Object>> getApiBankCards(@RequestHeader String token) {
        Map<String, Object> response = service.getApiBankCards(token);

        if (UNAUTHORIZED.value() == ((Number) response.getOrDefault(STATUS.toString(), 0)).intValue()) {
            return ResponseEntity.status(UNAUTHORIZED).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
