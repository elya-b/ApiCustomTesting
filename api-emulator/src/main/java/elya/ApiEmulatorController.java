package elya;

import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.emulator.constants.excpetions.TokenValidationException;
import elya.services.AuthenticationService;
import elya.services.MockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static elya.constants.ApiEndpoints.*;
import static elya.emulator.constants.messages.ResponseMessages.MOCK_CLEARED;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * REST controller for the bank card emulator.
 * Provides a comprehensive set of endpoints to manage custom mock data,
 * handle session-based authentication, and simulate real bank card API behaviors.
 */
@Tag(name = "Bank Card Emulator", description = "Operations for managing mocked bank card responses and sessions")
@RestController
@RequiredArgsConstructor
public class ApiEmulatorController {
    private final AuthenticationService authService;
    private final MockService mockService;

    /**
     * Authenticates the user and generates a unique session token.
     * The token is required for all subsequent operations in the emulator.
     *
     * @param request the {@link AuthRequest} containing valid user credentials
     * @return a {@link ResponseEntity} containing the session token and expiration details
     */
    @Operation(
            summary = "Generate Auth Token",
            description = "Verifies credentials and issues a session-based Bearer token"
    )
    @ApiResponse(responseCode = "200", description = "Authentication successful, token issued")
    @PostMapping(value = URL_TOKEN, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> generateAuthToken(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.generateAuthToken(request));
    }

    /**
     * Seeds or updates the mock database with custom bank card data for the current session.
     * Appends new cards to the existing list and generates unique internal IDs.
     *
     * @param token   the Authorization header (Bearer token)
     * @param request the {@link BankCardListRequest} containing the cards to be mocked
     * @return a {@link ResponseEntity} containing the full updated list of mocked cards
     */
    @Operation(
            summary = "Set Mock Response",
            description = "Appends new bank card mocks to the current session storage"
    )
    @ApiResponse(responseCode = "200", description = "Mock data updated successfully")
    @PostMapping(value = URL_BANK_CARD_DATA, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BankCardListResponse> setMockResponse(@Parameter(hidden = true) @RequestHeader(name = AUTHORIZATION) String token,
                                                                @Valid @RequestBody BankCardListRequest request) {
        String cleanToken = extractToken(token);
        BankCardListResponse response = mockService.setMockResponse(cleanToken, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes all custom mock data associated with the current session token.
     * Resets the mock state to an empty list.
     *
     * @param token the Authorization header (Bearer token)
     * @return a JSON object containing the result status and a success message
     */
    @Operation(
            summary = "Clear Mock Response",
            description = "Removes all custom mock data associated with the current session"
    )
    @ApiResponse(responseCode = "200", description = "Mock storage cleared successfully")
    @DeleteMapping(value = URL_BANK_CARD_DATA, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> clearMockResponse(@Parameter(hidden = true) @RequestHeader(name = AUTHORIZATION) String token) {
        boolean isCleared = mockService.clearMockResponse(extractToken(token));
        return ResponseEntity.ok(Map.of(
                "result", isCleared,
                "message", MOCK_CLEARED
        ));
    }

    /**
     * Removes a specific bank card from the session mock by its cardId.
     *
     * @param token  the Authorization header (Bearer token)
     * @param cardId the unique long identifier of the bank card
     * @return a JSON object with the deleted cardId or a 404 Not Found status
     */
    @Operation(
            summary = "Delete Specific Mock Card",
            description = "Granularly removes a single card from the mock list by its ID"
    )
    @ApiResponse(responseCode = "200", description = "Card successfully removed")
    @ApiResponse(responseCode = "404", description = "Card with the specified ID not found")
    @DeleteMapping(value = URL_BANK_CARD_DATA_ID, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteBankCardById(@Parameter(hidden = true) @RequestHeader(name = AUTHORIZATION) String token,
                                                @PathVariable(name = "cardId") Long cardId) {
        return mockService.deleteApiBankCardById(extractToken(token), cardId)
                .map(id -> ResponseEntity.ok(Map.of("cardId", id)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves bank card data. If a cardId is provided, returns a single card;
     * otherwise, returns the full list of mocked cards for the session.
     *
     * @param token  the Authorization header (Bearer token)
     * @param cardId (Optional) the unique identifier of a specific card
     * @return the requested card(s) or a 404 Not Found if the specific ID is missing
     */
    @Operation(
            summary = "Get Bank Cards",
            description = "Retrieves all cards or a specific card based on path variables"
    )
    @ApiResponse(responseCode = "200", description = "Data retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Specific card ID not found")
    @GetMapping(
            value = {URL_BANK_CARD_DATA, URL_BANK_CARD_DATA_ID},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getBankCards(@Parameter(hidden = true) @RequestHeader(name = AUTHORIZATION) String token,
                                          @PathVariable(name = "cardId", required = false) Long cardId) {
        String cleanToken = extractToken(token);
        if (cardId != null) {
            return mockService.getApiBankCardById(cleanToken, cardId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return ResponseEntity.ok(mockService.getApiBankCards(cleanToken));
    }

    /**
     * Internal utility to strip the 'Bearer ' prefix from the Authorization header.
     * Ensures only the raw token string is used for downstream service logic.
     *
     * @param header the full raw Authorization header value
     * @return the cleaned token string
     * @throws TokenValidationException if the header is null or does not follow Bearer format
     */
    public String extractToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new TokenValidationException("Invalid Authorization header format. Expected: Bearer <token>");
        }
        return header.substring(7).trim();
    }
}