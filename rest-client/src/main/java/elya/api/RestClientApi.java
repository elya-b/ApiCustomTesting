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
import elya.restclient.constants.logs.RestClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static elya.restclient.constants.logs.ExceptionMessage.*;

/**
 * Unified entry point for all bank-related API operations.
 * <p>Acts as a Facade, coordinating between {@link AuthClient}, {@link BankCardClient},
 * and {@link MockClient}. It manages the translation between high-level Domain models
 * and low-level Data Transfer Objects (DTOs).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientApi {

    private final IAuthApi authClient;
    private final IBankCardApi cardClient;
    private final IMockControlApi mockClient;

    /**
     * Authenticates a user and returns a domain-level Token.
     */
    public Token generateAuthToken(AuthRequest authRequest) {
        AuthResponse response = authClient.generateAuthToken(authRequest);
        return response.toDomain();
    }

    /**
     * Retrieves a single bank card by ID.
     * @throws RestClientException if the card is not found.
     */
    public BankCard getApiBankCardById(String token, Long cardId) {
        return cardClient.getApiBankCardById(token, cardId)
                .map(BankCardResponse::getBankCard)
                .orElseThrow(() -> new RestClientException(CARD_NOT_FOUND_WITH_ID + cardId));
    }

    /**
     * Fetches all available bank cards for the session.
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
     * Configures the emulator's state with a custom list of cards.
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
     * Fully resets the mock data for the current session.
     */
    public boolean clearMockResponse(String token) {
        return mockClient.clearMockResponse(token);
    }

    /**
     * Removes a specific card from the mock storage.
     * @throws RestClientException if the deletion cannot be performed.
     */
    public Long deleteApiBankCardById(String token, Long cardId) {
        return mockClient.deleteApiBankCardById(token, cardId)
                .orElseThrow(() -> new RestClientException(CAN_NOT_DELETE_CARD_WITH_ID + cardId));
    }
}