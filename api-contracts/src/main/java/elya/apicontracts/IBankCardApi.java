package elya.apicontracts;

import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardResponse;

import java.util.Optional;

public interface IBankCardApi {

    BankCardListResponse getApiBankCards(String token);

    Optional<BankCardResponse> getApiBankCardById(String token, Long cardId);
}
