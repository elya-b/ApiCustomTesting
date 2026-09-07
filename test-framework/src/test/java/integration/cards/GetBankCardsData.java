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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for retrieving the card list (GET /bank-cards/data).
 */
@Epic("Bank Card API")
@Feature("GET /bank-cards/data — Retrieve Cards")
public class GetBankCardsData extends AbstractApiTest {

    @Test
    @Story("Cards are present")
    @Severity(SeverityLevel.CRITICAL)
    @Priority(PriorityLevel.CRITICAL)
    @DisplayName("getBankCards() - Should return list of cards when cards are present in emulator")
    void getBankCards_ShouldReturnListOfCards_WhenCardsArePresent() {
        String token = emulator.getAuthToken();

        var expectedCard1 = BankCard.builder(4444555566667777L, CardType.DEBIT, Currency.USD)
                .cardStatus(true).balance(BigDecimal.valueOf(100.50)).build();
        var expectedCard2 = BankCard.builder(1111222233334444L, CardType.CREDIT, Currency.EUR)
                .cardStatus(false).balance(BigDecimal.ZERO).build();

        List<BankCard> expectedCards = emulator.addBankCards(token, List.of(expectedCard1, expectedCard2));
        attachJson("Seeded cards", expectedCards);

        List<BankCard> actualCards = clientApi.getApiBankCards(token);
        attachJson("Response", actualCards);

        verify("Verify that received cards match the seeded data", () ->
                assertThat(actualCards).usingRecursiveComparison().isEqualTo(expectedCards)
        );
    }

    @Test
    @Story("No cards present")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.HIGH)
    @DisplayName("getBankCards() - Should return empty list when no cards are set")
    void getBankCards_ShouldReturnEmptyList_WhenNoCardsAreSet() {
        List<BankCard> result = clientApi.getApiBankCards(emulator.getAuthToken());
        attachJson("Response", result);

        verify("Verify empty list", () ->
                assertTrue(result.isEmpty(), "Result should be empty if no cards were seeded")
        );
    }

    @Test
    @Story("Response completeness")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("getBankCards() - Response size must match the number of seeded cards")
    void getBankCards_ResponseSize_ShouldMatchSeededCount() {
        String token = emulator.getAuthToken();

        emulator.addBankCards(token, List.of(
                BankCard.builder(1111222233334444L, CardType.DEBIT,  Currency.USD).build(),
                BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.EUR).build(),
                BankCard.builder(9999888877776666L, CardType.DEBIT,  Currency.JPY).build()
        ));

        List<BankCard> result = clientApi.getApiBankCards(token);
        attachJson("Response", result);

        verify("Card count must match exactly", () ->
                assertEquals(3, result.size(), "Expected exactly 3 cards in the response")
        );
    }

    @Test
    @Story("Currency support")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("getBankCards() - All three Currency values should be stored and returned correctly")
    void getBankCards_ShouldReturnAllCurrencyValues() {
        String token = emulator.getAuthToken();

        emulator.addBankCards(token, List.of(
                BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.USD).build(),
                BankCard.builder(5555666677778888L, CardType.DEBIT, Currency.EUR).build(),
                BankCard.builder(9999888877776666L, CardType.DEBIT, Currency.JPY).build()
        ));

        List<BankCard> result = clientApi.getApiBankCards(token);
        attachJson("Response", result);

        verify("All currency variants must be present", () ->
                assertThat(result)
                        .extracting(BankCard::getCurrency)
                        .containsExactlyInAnyOrder(Currency.USD, Currency.EUR, Currency.JPY)
        );
    }

    @Test
    @Story("Card type support")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("getBankCards() - Both CardType values should be stored and returned correctly")
    void getBankCards_ShouldReturnBothCardTypes() {
        String token = emulator.getAuthToken();

        emulator.addBankCards(token, List.of(
                BankCard.builder(1111222233334444L, CardType.DEBIT,  Currency.USD).build(),
                BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.EUR).build()
        ));

        List<BankCard> result = clientApi.getApiBankCards(token);
        attachJson("Response", result);

        verify("Both DEBIT and CREDIT types must be present", () ->
                assertThat(result)
                        .extracting(BankCard::getCardType)
                        .containsExactlyInAnyOrder(CardType.DEBIT, CardType.CREDIT)
        );
    }

    @Test
    @Story("Optional field storage")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("getBankCards() - Cards with balance and cardStatus should be returned with correct values")
    void getBankCards_ShouldReturnCorrectBalanceAndStatus() {
        String token = emulator.getAuthToken();

        var card = BankCard.builder(4444555566667777L, CardType.DEBIT, Currency.USD)
                .cardStatus(true).balance(BigDecimal.valueOf(999.99)).build();
        emulator.addBankCards(token, List.of(card));

        List<BankCard> result = clientApi.getApiBankCards(token);
        attachJson("Response", result);

        verify("Balance and status must match seeded values", () -> {
            BankCard returned = result.getFirst();
            Allure.step("Balance equals 999.99", () -> assertEquals(BigDecimal.valueOf(999.99), returned.getBalance(), "Balance mismatch"));
            Allure.step("CardStatus is true",    () -> assertTrue(returned.getCardStatus(), "CardStatus must be true"));
        });
    }
}