package elya.api;

import elya.apicontracts.IAuthApi;
import elya.apicontracts.IBankCardApi;
import elya.apicontracts.IMockControlApi;
import elya.authentication.Token;
import elya.card.BankCard;
import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardRequest;
import elya.dto.bankcard.BankCardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Facade class providing a single entry point for all bank-related API operations.
 * Coordinates between specialized clients for authentication, card data, and mocks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientApi {
    private final IAuthApi authClient;
    private final IBankCardApi cardClient;
    private final IMockControlApi mockClient;

    public Token generateAuthToken(AuthRequest authRequest) {
        AuthResponse response = authClient.generateAuthToken(authRequest);
        return response.toDomain();
    }

    /**
     * Retrieves a specific bank card by its ID through the specialized card client.
     *
     * @param token  authentication bearer token
     * @param cardId unique identifier of the bank card
     * @return       Optional containing the BankCardResponse if found
     */
    public BankCard getApiBankCardById(String token, Long cardId) {
        Optional<BankCardResponse> response = cardClient.getApiBankCardById(token, cardId);

        return response
                .map(BankCardResponse::getBankCard)
                .orElseThrow(() -> new RuntimeException("Card not found with ID: " + cardId));
    }

    /**
     * Retrieves all bank cards associated with the provided authorization token.
     *
     * @param token authentication bearer token
     * @return      list of BankCardResponse objects
     */
    public List<BankCard> getApiBankCards(String token) {
        BankCardListResponse response = cardClient.getApiBankCards(token);

        if (response == null || response.getResponse() == null) {
            return List.of();
        }

        return response.getResponse().getCards().stream()
                .map(BankCardResponse::getBankCard)
                .toList();
    }

    /**
     * Updates the mock response through the specialized mock client.
     * Uses the simplified request DTO.
     *
     * @param token   authorization session token
     * @param request simplified mock data request
     * @return        true if the operation was successful
     */
    public List<BankCard> setMockResponse(String token, List<BankCard> request) {
        List<BankCardRequest> dtoList = request.stream()
                .map(BankCardRequest::fromDomain)
                .toList();
        BankCardListRequest finalRequest = BankCardListRequest.of(dtoList);

        BankCardListResponse response = mockClient.setMockResponse(token, finalRequest);

        return response.toDomainList();
    }

    /**
     * Clears the mock response for the specified session.
     *
     * @param token authorization session token
     * @return      true if the mock was cleared
     */
    public boolean clearMockResponse(String token) {
        return mockClient.clearMockResponse(token);
    }

    /**
     * Deletes a specific bank card from the mock response by its ID.
     *
     * @param token  authorization session token
     * @param cardId unique identifier of the card to be removed
     * @return       Optional containing the deleted card's ID if successful
     */
    public Long deleteApiBankCardById(String token, Long cardId) {

        return mockClient.deleteApiBankCardById(token, cardId)
                .orElseThrow(() -> new RuntimeException("Can not delete card with ID: " + cardId));
    }
}
