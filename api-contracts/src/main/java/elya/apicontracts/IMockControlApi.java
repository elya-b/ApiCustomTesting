package elya.apicontracts;

import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;

import java.util.Optional;

public interface IMockControlApi {

    BankCardListResponse setMockResponse(String token, BankCardListRequest request);
    boolean clearMockResponse(String token);
    Optional<Long> deleteApiBankCardById(String token, Long cardId);
}
