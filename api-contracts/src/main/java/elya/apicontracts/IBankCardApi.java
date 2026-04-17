package elya.apicontracts;

import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardResponse;

import java.util.Optional;

/**
 * Contract for read-only bank card API operations.
 * Provides methods to retrieve card lists and individual cards by identifier.
 */
public interface IBankCardApi {

    BankCardListResponse getApiBankCards(String token);

    Optional<BankCardResponse> getApiBankCardById(String token, Long cardId);
}