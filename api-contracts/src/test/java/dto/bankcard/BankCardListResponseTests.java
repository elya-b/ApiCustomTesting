package dto.bankcard;

import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link elya.dto.bankcard.BankCardListResponse}.
 * <ul>
 *   <li>{@code of()} correctly wraps a card list into a response container and sets the size</li>
 * </ul>
 */
public class BankCardListResponseTests {

    @Test
    @DisplayName("of() - Should correctly wrap card list and set size")
    void of_ShouldCreateValidResponseContainer() {
        var cards = List.of(
                BankCardResponse.builder().cardId(1L).build(),
                BankCardResponse.builder().cardId(2L).build()
        );

        var response = BankCardListResponse.of(cards);

        assertAll("Verify response wrapper",
                () -> assertNotNull(response.getResponse(), "Container should not be null"),
                () -> assertEquals(2, response.getResponse().getSize(), "Size mismatch"),
                () -> assertEquals(cards, response.getResponse().getCards(), "Cards list mismatch")
        );
    }
}
