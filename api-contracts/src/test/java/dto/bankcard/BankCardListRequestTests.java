package dto.bankcard;

import elya.dto.bankcard.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link elya.dto.bankcard.BankCardListRequest}.
 * <ul>
 *   <li>{@code of()} wraps a list of {@link elya.dto.bankcard.BankCardRequest} into a valid list request</li>
 * </ul>
 */
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
