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
 * Provides endpoints to manage mock data, perform authentication,
 * and simulate bank card API responses.
 */
@Tag(name = "Bank Card Emulator", description = "Operations for managing mocked bank card responses")
@RestController
@RequiredArgsConstructor
public class ApiEmulatorController {
    private final AuthenticationService authService;
    private final MockService mockService;

    /**
     * Authenticates a user and generates a new session token.
     *
     * @param request login credentials including username and password
     * @return        ResponseEntity containing the AuthResponse with session token
     */
    @Operation(
            summary = "Generate Auth Token",
            description = "Authenticates user credentials and returns a session token"
    )
    @ApiResponse(responseCode = "200", description = "Token generated successfully")
    @PostMapping(value = URL_TOKEN, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> generateAuthToken(@Valid @RequestBody AuthRequest request) {

        return ResponseEntity.ok(authService.generateAuthToken(request));
    }

    /**
     * Sets or updates the mock card data for the current session.
     *
     * @param token   Authorization header containing the session token
     * @param request list of cards to be set in the mock response
     * @return        ResponseEntity containing the newly set BankCardListResponse
     */
    @Operation(summary = "Set Mock Response")
    @PostMapping(URL_BANK_CARD_MOCK)
    public ResponseEntity<BankCardListResponse> setMockResponse(@Parameter(hidden = true) @RequestHeader(name = AUTHORIZATION) String token,
                                                                @Valid @RequestBody BankCardListRequest request) {
        String cleanToken = extractToken(token);
        BankCardListResponse response = mockService.setMockResponse(cleanToken, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Removes the entire custom mock response for the current session.
     *
     * @param token Authorization header containing the session token
     * @return      ResponseEntity with a success message
     */
    @Operation(
            summary = "Clear Mock Response",
            description = "Removes the custom mock response associated with the current session token")
    @ApiResponse(responseCode = "200", description = "Mock data cleared successfully")
    @DeleteMapping(URL_BANK_CARD_MOCK)
        public ResponseEntity<Map<String, Object>> clearMockResponse(@Parameter(hidden = true) @RequestHeader(name = AUTHORIZATION) String token) {

        boolean isCleared = mockService.clearMockResponse(extractToken(token));

        return ResponseEntity.ok(Map.of(
                "result", isCleared,
                "message", MOCK_CLEARED
        ));
    }

    /**
     * Deletes a specific bank card from the mock by its unique identifier.
     *
     * @param token  Authorization header containing the session token
     * @param cardId unique identifier of the card to be deleted
     * @return       ResponseEntity containing the ID of the deleted card or 404 if not found
     */
    @Operation(
            summary = "Delete Specific Mock Card",
            description = "Removes a single card by its ID and returns the ID of the deleted card")
    @ApiResponse(
            responseCode = "200", description = "Card deleted successfully")
    @ApiResponse(
            responseCode = "404", description = "Card not found")
    @DeleteMapping(
            value = URL_BANK_CARD_DATA_ID,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteBankCardById(@Parameter(hidden = true) @RequestHeader(name = AUTHORIZATION) String token,
                                                @PathVariable(name = "cardId") Long cardId) {

        return mockService.deleteApiBankCardById(extractToken(token), cardId)
                .map(id -> ResponseEntity.ok(Map.of("cardId", id)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves either all mocked bank cards or a specific card by its identifier.
     * Supports optional path variable for granular data retrieval.
     *
     * @param token  Authorization token
     * @param cardId optional unique identifier of the card
     * @return       BankCardListResponse or a single BankCardResponse
     */
    @Operation(
            summary = "Get Bank Cards",
            description = "Returns all cards or a specific one if cardId is provided")
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

        BankCardListResponse allCards = mockService.getApiBankCards(cleanToken);
        return ResponseEntity.ok(allCards);
    }

    /**
     * Extracts the raw token string from the Authorization header.
     * Uses case-insensitive prefix check for maximum reliability.
     *
     * @param header full Authorization header value
     * @return       cleaned token string
     */
    public String extractToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new TokenValidationException("Invalid Authorization header format");
        }
        return header.substring(7);
    }
}
