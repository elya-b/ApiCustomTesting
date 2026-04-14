package integration.cards;

import elya.card.BankCard;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import integration.AbstractApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class PostBankCardsData extends AbstractApiTest {

    @Test
    @DisplayName("setMockResponse() - Should return empty list when card number has 15 digits")
    void setMockResponse_ShouldReturnEmptyList_WhenCardNumberHas15Digits() {
        String token = emulator.getAuthToken();

        var cardRequest = BankCard.builder(444455556666777L, CardType.DEBIT, Currency.USD).build();

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));

        assertNotNull(result, "Result list should not be null");
        assertTrue(result.isEmpty(), "Card with 15 digits should not be created or returned");
    }

    @Test
    @DisplayName("setMockResponse() - Should return empty list when card number has 17 digits")
    void setMockResponse_ShouldReturnEmptyList_WhenCardNumberHas17Digits() {
        String token = emulator.getAuthToken();

        var cardRequest = BankCard.builder(44445555666677770L, CardType.DEBIT, Currency.USD).build();

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));

        assertNotNull(result, "Result list should not be null");
        assertTrue(result.isEmpty(), "Card with 17 digits should not be created or returned");
    }

    @Test
    @DisplayName("setMockResponse() - Should return empty list when card number is missing")
    void setMockResponse_ShouldReturnEmptyList_WhenCardNumberIsMissing() {
        String token = emulator.getAuthToken();

        var cardRequest = BankCard.builder(null, CardType.DEBIT, Currency.USD).build();

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));

        assertNotNull(result, "Result list should not be null");
        assertTrue(result.isEmpty(), "Card should not be created without card number");
    }

    @Test
    @DisplayName("setMockResponse() - Should return empty list when currency is missing")
    void setMockResponse_ShouldReturnEmptyList_WhenCurrencyIsMissing() {
        String token = emulator.getAuthToken();

        var cardRequest = BankCard.builder(4444555566667777L, CardType.DEBIT, null).build();

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));

        assertNotNull(result, "Result list should not be null");
        assertTrue(result.isEmpty(), "Card should not be created without currency");
    }

    @Test
    @DisplayName("setMockResponse() - Should return empty list when card type is missing")
    void setMockResponse_ShouldReturnEmptyList_WhenCardTypeIsMissing() {
        String token = emulator.getAuthToken();

        var cardRequest = BankCard.builder(4444555566667777L, null, Currency.EUR).build();

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));

        assertNotNull(result, "Result list should not be null");
        assertTrue(result.isEmpty(), "Card should not be created without card type");
    }

    @Test
    @DisplayName("setMockResponse() - Should successfully create a single bank card with all parameters")
    void setMockResponse_ShouldReturnFullCardDetails_WhenDataIsValid() {
        String token = emulator.getAuthToken();

        var cardRequest = BankCard.builder(4444555566667777L, CardType.CREDIT, Currency.USD)
                .cardStatus(true)
                .balance(BigDecimal.valueOf(1500.50))
                .build();

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));

        verify("Verify single card details", () -> {
            assertThat(result).hasSize(1);
            assertThat(result.getFirst())
                    .usingRecursiveComparison()
                    .ignoringFields("cardId")
                    .isEqualTo(cardRequest);
        });
    }

    @Test
    @DisplayName("setMockResponse() - Should successfully create multiple bank cards and match all data")
    void setMockResponse_ShouldReturnMatchedCards_WhenMultipleCardsAreAdded() {
        String token = emulator.getAuthToken();

        var card1 = BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.EUR)
                .cardStatus(true)
                .balance(BigDecimal.ZERO)
                .build();

        var card2 = BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.JPY)
                .cardStatus(false)
                .balance(BigDecimal.valueOf(9999))
                .build();

        List<BankCard> expectedCards = List.of(card1, card2);

        List<BankCard> actualCards = clientApi.setMockResponse(token, expectedCards);

        assertNotNull(actualCards, "Result list should not be null");
        assertEquals(expectedCards.size(), actualCards.size(), "Cards count mismatch");

        verify("Verify cards match using AssertJ", () -> {
            assertThat(actualCards)
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("cardId")
                    .containsExactlyInAnyOrderElementsOf(expectedCards);
        });
    }

    @Test
    @DisplayName("setMockResponse() - Should assign incremented cardId and match all parameters when cards already exist")
    void setMockResponse_ShouldAssignIncrementedId_WhenCardsAlreadyExist() {
        String token = emulator.getAuthToken();

        var existingCard1 = BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.USD).build();
        var existingCard2 = BankCard.builder(2222333344445555L, CardType.CREDIT, Currency.EUR).build();

        emulator.addBankCards(token, List.of(existingCard1, existingCard2));

        var newCardRequest = BankCard.builder(9999888877776666L, CardType.DEBIT, Currency.USD)
                .cardStatus(true)
                .balance(BigDecimal.valueOf(777.77))
                .build();

        List<BankCard> actualCards = clientApi.setMockResponse(token, List.of(newCardRequest));

        verify("Verify that the 3rd card has ID 3 and correct data", () -> {
            assertThat(actualCards)
                    .as("Result should contain all cards (2 existing + 1 new)")
                    .hasSize(3);

            BankCard lastCard = actualCards.getLast();
            assertThat(lastCard.getCardId())
                    .as("The newly added card should have cardId = 3")
                    .isEqualTo(3L);

            assertThat(lastCard)
                    .usingRecursiveComparison()
                    .ignoringFields("cardId")
                    .isEqualTo(newCardRequest);
        });
    }
}
