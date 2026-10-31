package dto.bankcard;

import elya.dto.bankcard.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link elya.dto.bankcard.BankCardListResponse}.
 * <ul>
 *   <li>{@code of()} correctly wraps a card list into a response container and sets the size</li>
 *   <li>{@code of()} returns cards that cannot be modified</li>
 *   <li>{@code of()} keeps a copy, so later changes to the source list do not reach the response</li>
 *   <li>{@code of()} returns an empty response for a {@code null} list</li>
 *   <li>{@code of()} rejects a {@code null} card</li>
 * </ul>
 */
public class BankCardListResponseTests {

    private static final BankCardResponse CARD_1 = BankCardResponse.builder().cardId(1L).build();
    private static final BankCardResponse CARD_2 = BankCardResponse.builder().cardId(2L).build();

    @Test
    @DisplayName("of() - Should correctly wrap card list and set size")
    void of_ShouldCreateValidResponseContainer() {
        var cards = List.of(CARD_1, CARD_2);

        var response = BankCardListResponse.of(cards);

        assertAll("Verify response wrapper",
                () -> assertNotNull(response.getResponse(), "Container should not be null"),
                () -> assertEquals(2, response.getResponse().getSize(), "Size mismatch"),
                () -> assertEquals(cards, response.getResponse().getCards(), "Cards list mismatch")
        );
    }

    @Test
    @DisplayName("of() - Should return cards that cannot be modified")
    void of_ShouldReturnUnmodifiableCards() {
        var cards = BankCardListResponse.of(List.of(CARD_1)).getResponse().getCards();

        assertThrows(UnsupportedOperationException.class, () -> cards.add(CARD_2),
                "A response must not be changeable through its getters");
    }

    @Test
    @DisplayName("of() - Should keep a copy, so later changes to the source list do not reach the response")
    void of_ShouldNotReflectLaterChangesOfSourceList() {
        var source = new ArrayList<>(List.of(CARD_1));
        var response = BankCardListResponse.of(source);

        source.add(CARD_2);

        assertAll("The response must keep the cards it was created with",
                () -> assertEquals(List.of(CARD_1), response.getResponse().getCards(), "Cards"),
                () -> assertEquals(1, response.getResponse().getSize(), "Size"));
    }

    @Test
    @DisplayName("of() - Should return an empty response for a null list")
    void of_ShouldReturnEmptyResponse_WhenCardsIsNull() {
        var response = BankCardListResponse.of(null);

        assertAll("Empty response",
                () -> assertEquals(0, response.getResponse().getSize(), "Size"),
                () -> assertTrue(response.getResponse().getCards().isEmpty(), "Cards"));
    }

    @Test
    @DisplayName("of() - Should reject a null card")
    void of_ShouldRejectNullCard() {
        var cards = Arrays.asList(CARD_1, null);

        assertThrows(NullPointerException.class, () -> BankCardListResponse.of(cards),
                "A null card is malformed data and must fail fast");
    }
}