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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetBankCardsData extends AbstractApiTest {

    @Test
    @DisplayName("getBankCards() - Should return list of cards when cards are present in emulator")
    void getBankCards_ShouldReturnListOfCards_WhenCardsArePresent() {
        String token = emulator.getAuthToken();

        var expectedCard1 = BankCard.builder(4444555566667777L, CardType.DEBIT, Currency.USD)
                .cardStatus(true)
                .balance(BigDecimal.valueOf(100.50))
                .build();

        var expectedCard2 = BankCard.builder(1111222233334444L, CardType.CREDIT, Currency.EUR)
                .cardStatus(false)
                .balance(BigDecimal.ZERO)
                .build();
        List<BankCard> managerRequestCards = List.of(expectedCard1, expectedCard2);
        List<BankCard> expectedCards = emulator.addBankCards(token, managerRequestCards);

        List<BankCard> actualCards = clientApi.getApiBankCards(token);
        verify("Verify that received cards match the seeded data", () -> {
            assertThat(actualCards)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedCards);
        });
    }

    @Test
    @DisplayName("getBankCards() - Should return empty list when no cards are set")
    void getBankCards_ShouldReturnEmptyList_WhenNoCardsAreSet() {
        List<BankCard> result = clientApi.getApiBankCards(emulator.getAuthToken());

        verify("Verify empty list", () ->
                assertTrue(result.isEmpty(), "Result should be empty if no cards were seeded")
        );
    }
}
