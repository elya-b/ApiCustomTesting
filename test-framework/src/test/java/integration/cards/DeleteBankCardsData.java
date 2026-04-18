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

/**
 * Integration tests for clearing mock data (DELETE /bank-cards/data).
 */
@Epic("Bank Card API")
@Feature("DELETE /bank-cards/data — Clear Mock Response")
public class DeleteBankCardsData extends AbstractApiTest {

    @Test
    @Story("Clear existing cards")
    @Severity(SeverityLevel.CRITICAL)
    @Priority(PriorityLevel.CRITICAL)
    @DisplayName("clearMockResponse() - Should successfully remove all cards when cards are present")
    void clearMockResponse_ShouldClearAllCards_WhenCardsExist() {
        String token = emulator.getAuthToken();

        emulator.addBankCards(token, List.of(
                BankCard.builder(1111222233334444L, CardType.DEBIT,  Currency.USD).build(),
                BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.EUR).build()
        ));

        boolean isCleared = clientApi.clearMockResponse(token);
        List<BankCard> remainingCards = clientApi.getApiBankCards(token);
        attachJson("Remaining cards after clear", remainingCards);

        verify("Verify that the mock is cleared and the list is empty", () -> {
            Allure.step("clearMockResponse returns true", () ->
                    assertThat(isCleared).as("The method should return true on successful clear").isTrue());
            Allure.step("Card list is empty after clear", () ->
                    assertThat(remainingCards).as("The list of cards should be empty after clearing").isEmpty());
        });
    }

    @Test
    @Story("Clear empty state")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("clearMockResponse() - Should return true even when no cards are present")
    void clearMockResponse_ShouldReturnTrue_WhenNoCardsArePresent() {
        String token = emulator.getAuthToken();

        clientApi.clearMockResponse(token);
        boolean isCleared = clientApi.clearMockResponse(token);

        List<BankCard> result = clientApi.getApiBankCards(token);
        attachJson("Response after 2nd clear", result);

        verify("Verify that clearing an empty state still returns true", () -> {
            Allure.step("clearMockResponse returns true on empty state", () ->
                    assertThat(isCleared).as("Clearing an already empty state should still be successful").isTrue());
            Allure.step("Card list is still empty", () -> assertThat(result).isEmpty());
        });
    }

    @Test
    @Story("State after clear")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("clearMockResponse() - After clearing, getBankCards must return empty list")
    void clearMockResponse_GetBankCards_ShouldReturnEmpty_AfterClear() {
        String token = emulator.getAuthToken();

        emulator.addBankCards(token, List.of(
                BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.USD).build()
        ));
        clientApi.clearMockResponse(token);

        List<BankCard> result = clientApi.getApiBankCards(token);
        attachJson("Response after clear", result);

        verify("After clearing, card list must be empty", () -> assertThat(result).isEmpty());
    }

    @Test
    @Story("State after clear")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("clearMockResponse() - After clearing, new cards can be added again")
    void clearMockResponse_ShouldAllowAddingCards_AfterClear() {
        String token = emulator.getAuthToken();

        emulator.addBankCards(token, List.of(
                BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.USD).build()
        ));
        clientApi.clearMockResponse(token);

        var newCard = BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.EUR).build();
        attachJson("New card request", newCard);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(newCard));
        attachJson("Response", result);

        verify("After clearing, fresh cards must be addable with ID starting from 1", () -> {
            Allure.step("Result contains 1 card",  () -> assertThat(result).hasSize(1));
            Allure.step("New card has cardId = 1", () -> assertThat(result.getFirst().getCardId()).isEqualTo(1L));
        });
    }
}