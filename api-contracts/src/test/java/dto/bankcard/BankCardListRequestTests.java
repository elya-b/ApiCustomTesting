package dto.bankcard;

import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BankCardListRequestTests {

    @Test
    @DisplayName("of() - Should wrap card requests into list")
    void of_ShouldCreateValidRequest() {
        var requests = List.of(BankCardRequest.builder().cardNumber(111L).build());

        var listRequest = BankCardListRequest.of(requests);

        assertAll("Verify request wrapper",
                () -> assertNotNull(listRequest.getCards()),
                () -> assertEquals(1, listRequest.getCards().size()),
                () -> assertEquals(111L, listRequest.getCards().getFirst().getCardNumber())
        );
    }
}
