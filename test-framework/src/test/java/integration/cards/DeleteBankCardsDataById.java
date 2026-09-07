package integration.cards;

import elya.allure.Priority;
import elya.allure.PriorityLevel;
import elya.card.BankCard;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import integration.AbstractApiTest;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for deleting a card by ID (DELETE /bank-cards/data/{id}).
 * <ul>
 *   <li>Card found — deleted successfully; remaining cards are untouched</li>
 *   <li>Card not found — throws RuntimeException with a descriptive message</li>
 *   <li>Only card in list — list is empty after deletion</li>
 *   <li>Deleted card — not accessible via {@code getApiBankCardById()}</li>
 *   <li>Returned ID — matches the ID of the deleted card</li>
 * </ul>
 */
@Epic("Bank Card API")
@Feature("DELETE /bank-cards/data/{id} — Delete Card By ID")
public class DeleteBankCardsDataById extends AbstractApiTest {

    @Test
    @Story("Successful deletion")
    @Severity(SeverityLevel.CRITICAL)
    @Priority(PriorityLevel.CRITICAL)
    @DisplayName("deleteApiBankCardById() - Should successfully delete a specific card and return its ID")
    void deleteApiBankCardById_ShouldDeleteCard_WhenIdExists() {
        String token = emulator.getAuthToken();

        List<BankCard> seededCards = emulator.addBankCards(token, List.of(
                BankCard.builder(1111222233334444L, CardType.DEBIT,  Currency.USD).build(),
                BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.EUR).build()
        ));
        attachJson("Seeded cards", seededCards);

        Long targetId = seededCards.stream()
                .filter(c -> c.getCardNumber().equals(1111222233334444L))
                .map(BankCard::getCardId)
                .findFirst()
                .orElseThrow();

        Long deletedId = clientApi.deleteApiBankCardById(token, targetId);

        List<BankCard> remainingCards = clientApi.getApiBankCards(token);
        attachJson("Remaining cards", remainingCards);

        verify("Verify card is deleted but others remain", () -> {
            Allure.step("Returned ID matches target",    () -> assertThat(deletedId).isEqualTo(targetId));
            Allure.step("Remaining list has 1 card",     () -> assertThat(remainingCards).hasSize(1));
            Allure.step("Deleted card ID is absent",     () -> assertThat(remainingCards)
                    .extracting(BankCard::getCardId).doesNotContain(targetId));
            Allure.step("Second card is still present",  () -> assertThat(remainingCards)
                    .extracting(BankCard::getCardId).contains(seededCards.get(1).getCardId()));
        });
    }

    @Test
    @Story("Card not found")
    @Severity(SeverityLevel.CRITICAL)
    @Priority(PriorityLevel.HIGH)
    @DisplayName("deleteApiBankCardById() - Should throw exception when card ID does not exist")
    void deleteApiBankCardById_ShouldThrowException_WhenCardIdDoesNotExist() {
        String token = emulator.getAuthToken();
        Long nonExistentId = 999999L;

        verify("Verify that deleting a non-existent ID results in a RuntimeException", () ->
                assertThatThrownBy(() -> clientApi.deleteApiBankCardById(token, nonExistentId))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Can not delete card with ID: " + nonExistentId)
        );
    }

    @Test
    @Story("Successful deletion")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("deleteApiBankCardById() - Deleting the only card should leave an empty list")
    void deleteApiBankCardById_ShouldLeaveEmptyList_WhenOnlyOneCardExists() {
        String token = emulator.getAuthToken();

        List<BankCard> seeded = emulator.addBankCards(token,
                List.of(BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.USD).build()));

        clientApi.deleteApiBankCardById(token, seeded.getFirst().getCardId());

        List<BankCard> remaining = clientApi.getApiBankCards(token);
        attachJson("Remaining cards", remaining);

        verify("After deleting the only card, list must be empty", () -> assertThat(remaining).isEmpty());
    }

    @Test
    @Story("State after deletion")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("deleteApiBankCardById() - Deleted card should not be accessible by getById")
    void deleteApiBankCardById_DeletedCard_ShouldNotBeRetrievableById() {
        String token = emulator.getAuthToken();

        List<BankCard> seeded = emulator.addBankCards(token,
                List.of(BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.USD).build()));

        Long deletedId = seeded.getFirst().getCardId();
        clientApi.deleteApiBankCardById(token, deletedId);

        verify("Deleted card must not be retrievable by ID", () ->
                assertThatThrownBy(() -> clientApi.getApiBankCardById(token, deletedId))
                        .isInstanceOf(RuntimeException.class)
        );
    }

    @Test
    @Story("Successful deletion")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.LOW)
    @DisplayName("deleteApiBankCardById() - Returned ID must match the requested deletion ID")
    void deleteApiBankCardById_ReturnedId_ShouldMatchRequestedId() {
        String token = emulator.getAuthToken();

        List<BankCard> seeded = emulator.addBankCards(token,
                List.of(BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.USD).build()));

        Long targetId  = seeded.getFirst().getCardId();
        Long returnedId = clientApi.deleteApiBankCardById(token, targetId);

        verify("Returned ID must equal the deleted card's ID", () ->
                assertEquals(targetId, returnedId)
        );
    }
}