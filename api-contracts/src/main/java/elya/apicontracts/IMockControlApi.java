package elya.apicontracts;

import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;

import java.util.Optional;

/**
 * Contract for controlling mock bank card data in the emulator.
 * Provides methods to seed, clear, and partially delete mocked bank card responses.
 */
public interface IMockControlApi {

    BankCardListResponse setMockResponse(String token, BankCardListRequest request);
    boolean clearMockResponse(String token);
    Optional<Long> deleteApiBankCardById(String token, Long cardId);
}