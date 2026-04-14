package integration.cards;

import elya.card.BankCard;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import integration.AbstractApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DeleteBankCardsDataById extends AbstractApiTest {

    @Test
    @DisplayName("deleteApiBankCardById() - Should successfully delete a specific card and return its ID")
    void deleteApiBankCardById_ShouldDeleteCard_WhenIdExists() {
        String token = emulator.getAuthToken();

        var cardToDelete = BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.USD).build();
        var cardToKeep = BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.EUR).build();
        List<BankCard> seededCards = emulator.addBankCards(token, List.of(cardToDelete, cardToKeep));

        Long targetId = seededCards.stream()
                .filter(c -> c.getCardNumber().equals(1111222233334444L))
                .map(BankCard::getCardId)
                .findFirst()
                .orElseThrow();

        Long deletedId = clientApi.deleteApiBankCardById(token, targetId);

        verify("Verify card is deleted but others remain", () -> {
            assertThat(deletedId).isEqualTo(targetId);

            List<BankCard> remainingCards = clientApi.getApiBankCards(token);
            assertThat(remainingCards)
                    .hasSize(1)
                    .extracting(BankCard::getCardId)
                    .doesNotContain(targetId)
                    .contains(seededCards.get(1).getCardId());
        });
    }

    @Test
    @DisplayName("deleteApiBankCardById() - Should throw exception when card ID does not exist")
    void deleteApiBankCardById_ShouldThrowException_WhenCardIdDoesNotExist() {
        String token = emulator.getAuthToken();
        Long nonExistentId = 999999L;

        verify("Verify that deleting a non-existent ID results in a RuntimeException", () -> {
            assertThatThrownBy(() -> clientApi.deleteApiBankCardById(token, nonExistentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Can not delete card with ID: " + nonExistentId);
        });
    }
}