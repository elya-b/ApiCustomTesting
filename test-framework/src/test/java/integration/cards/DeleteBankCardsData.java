package integration.cards;

import elya.card.BankCard;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import integration.AbstractApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DeleteBankCardsData extends AbstractApiTest {

    @Test
    @DisplayName("clearMockResponse() - Should successfully remove all cards when cards are present")
    void clearMockResponse_ShouldClearAllCards_WhenCardsExist() {
        String token = emulator.getAuthToken();

        var card1 = BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.USD).build();
        var card2 = BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.EUR).build();
        emulator.addBankCards(token, List.of(card1, card2));

        boolean isCleared = clientApi.clearMockResponse(token);

        verify("Verify that the mock is cleared and the list is empty", () -> {
            assertThat(isCleared)
                    .as("The method should return true on successful clear")
                    .isTrue();

            List<BankCard> remainingCards = clientApi.getApiBankCards(token);
            assertThat(remainingCards)
                    .as("The list of cards should be empty after clearing")
                    .isEmpty();
        });
    }

    @Test
    @DisplayName("clearMockResponse() - Should return true even when no cards are present")
    void clearMockResponse_ShouldReturnTrue_WhenNoCardsArePresent() {
        String token = emulator.getAuthToken();

        clientApi.clearMockResponse(token);

        boolean isCleared = clientApi.clearMockResponse(token);

        verify("Verify that clearing an empty state still returns true", () -> {
            assertThat(isCleared)
                    .as("Clearing an already empty state should still be successful")
                    .isTrue();

            List<BankCard> result = clientApi.getApiBankCards(token);
            assertThat(result).isEmpty();
        });
    }
}