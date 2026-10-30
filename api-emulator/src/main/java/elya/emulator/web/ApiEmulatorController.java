package elya.emulator.web;

import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardResponse;
import elya.emulator.exceptions.CardNotFoundException;
import elya.emulator.services.AuthenticationService;
import elya.emulator.services.MockService;
import elya.emulator.web.auth.SessionToken;
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

/**
 * REST controller for the bank card emulator.
 * <p>Provides endpoints to issue session tokens and to manage the mocked bank card data of a session.
 * Every bank card endpoint is authenticated by {@link elya.emulator.web.auth.SessionTokenInterceptor}
 * before it is invoked, so handlers receive an already validated token through {@link SessionToken}.</p>
 * <p>Error responses are documented only by status and description here; their problem detail body,
 * the {@code 401} of protected endpoints and the {@code 500} are added by {@link OpenApiConfig}.</p>
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
    @ApiResponse(responseCode = "400", description = "Login or password is blank")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping(value = URL_TOKEN, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> generateAuthToken(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.generateAuthToken(request));
    }

    /**
     * Seeds or updates the mock database with custom bank card data for the current session.
     * Appends new cards to the existing list and generates unique internal IDs.
     *
     * @param token   the authenticated session token
     * @param request the {@link BankCardListRequest} containing the cards to be mocked
     * @return a {@link ResponseEntity} containing the full updated list of mocked cards
     */
    @Operation(
            summary = "Set Mock Response",
            description = "Appends new bank card mocks to the current session storage"
    )
    @ApiResponse(responseCode = "200", description = "Mock data updated successfully")
    @ApiResponse(responseCode = "400", description = "Request body is missing, malformed or fails validation")
    @PostMapping(value = URL_BANK_CARD_DATA, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BankCardListResponse> setMockResponse(@Parameter(hidden = true) @SessionToken String token,
                                                                @Valid @RequestBody BankCardListRequest request) {
        BankCardListResponse response = mockService.setMockResponse(token, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes all custom mock data associated with the current session token.
     * Resets the mock state to an empty list.
     *
     * @param token the authenticated session token
     * @return a JSON object containing the result status and a success message
     */
    @Operation(
            summary = "Clear Mock Response",
            description = "Removes all custom mock data associated with the current session"
    )
    @ApiResponse(responseCode = "200", description = "Mock storage cleared successfully")
    @DeleteMapping(value = URL_BANK_CARD_DATA, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> clearMockResponse(@Parameter(hidden = true) @SessionToken String token) {
        boolean isCleared = mockService.clearMockResponse(token);
        return ResponseEntity.ok(Map.of(
                "result", isCleared,
                "message", MOCK_CLEARED
        ));
    }

    /**
     * Removes a specific bank card from the session mock by its cardId.
     *
     * @param token  the authenticated session token
     * @param cardId the unique long identifier of the bank card
     * @return a JSON object with the deleted cardId
     * @throws CardNotFoundException if the session mock holds no card with {@code cardId}
     */
    @Operation(
            summary = "Delete Specific Mock Card",
            description = "Granularly removes a single card from the mock list by its ID"
    )
    @ApiResponse(responseCode = "200", description = "Card successfully removed")
    @ApiResponse(responseCode = "404", description = "Card with the specified ID not found")
    @DeleteMapping(value = URL_BANK_CARD_DATA_ID, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Long>> deleteBankCardById(@Parameter(hidden = true) @SessionToken String token,
                                                                @PathVariable(name = "cardId") Long cardId) {
        Long deletedId = mockService.deleteApiBankCardById(token, cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        return ResponseEntity.ok(Map.of("cardId", deletedId));
    }

    /**
     * Retrieves all mocked bank cards of the current session.
     *
     * @param token the authenticated session token
     * @return the full list of mocked cards, empty if none were set
     */
    @Operation(
            summary = "Get Bank Cards",
            description = "Retrieves all mocked cards of the current session"
    )
    @ApiResponse(responseCode = "200", description = "Cards retrieved successfully (empty list if none)")
    @GetMapping(value = URL_BANK_CARD_DATA, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BankCardListResponse> getBankCards(@Parameter(hidden = true) @SessionToken String token) {
        return ResponseEntity.ok(mockService.getApiBankCards(token));
    }

    /**
     * Retrieves a specific mocked bank card of the current session by its cardId.
     *
     * @param token  the authenticated session token
     * @param cardId the unique identifier of the card
     * @return the requested card
     * @throws CardNotFoundException if the session mock holds no card with {@code cardId}
     */
    @Operation(
            summary = "Get Bank Card By ID",
            description = "Retrieves a single mocked card of the current session"
    )
    @ApiResponse(responseCode = "200", description = "Card retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Card with the specified ID not found")
    @GetMapping(value = URL_BANK_CARD_DATA_ID, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BankCardResponse> getBankCardById(@Parameter(hidden = true) @SessionToken String token,
                                                            @PathVariable(name = "cardId") Long cardId) {
        return ResponseEntity.ok(mockService.getApiBankCardById(token, cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId)));
    }
}