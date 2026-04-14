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

public class GetBankCardsDataById extends AbstractApiTest {

    @Test
    @DisplayName("getApiBankCardById() - Should return specific card with all matching parameters")
    void getApiBankCardById_ShouldReturnSpecificCard_WithAllParameters() {
        String token = emulator.getAuthToken();

        var expectedCard = BankCard.builder(4444555566667777L, CardType.DEBIT, Currency.USD).build();
        var otherCard = BankCard.builder(1111222233334444L, CardType.CREDIT, Currency.EUR).build();
        List<BankCard> seededCards = emulator.addBankCards(token, List.of(expectedCard, otherCard));

        BankCard targetFromEmulator = seededCards.stream()
                .filter(c -> c.getCardNumber().equals(4444555566667777L))
                .findFirst()
                .orElseThrow();

        BankCard actualCard = clientApi.getApiBankCardById(token, targetFromEmulator.getCardId());

        verify("Verify all fields match exactly", () -> {
            assertThat(actualCard)
                    .usingRecursiveComparison()
                    .isEqualTo(targetFromEmulator);
        });
    }

    @Test
    @DisplayName("getApiBankCardById() - Should throw exception when card ID does not exist")
    void getApiBankCardById_ShouldThrowException_WhenCardIdDoesNotExist() {
        String token = emulator.getAuthToken();
        Long nonExistentId = 999999L;

        verify("Verify that requesting a non-existent ID results in a RuntimeException", () -> {
            assertThatThrownBy(() -> clientApi.getApiBankCardById(token, nonExistentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Card not found with ID: " + nonExistentId);
        });
    }
}
