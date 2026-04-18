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
 * Integration tests for setting mock data (POST /bank-cards/data).
 */
@Epic("Bank Card API")
@Feature("POST /bank-cards/data — Set Mock Response")
public class PostBankCardsData extends AbstractApiTest {

    @Test
    @Story("Card number validation")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("setMockResponse() - Should return empty list when card number has 15 digits")
    void setMockResponse_ShouldReturnEmptyList_WhenCardNumberHas15Digits() {
        String token = emulator.getAuthToken();
        var cardRequest = BankCard.builder(444455556666777L, CardType.DEBIT, Currency.USD).build();
        attachJson("Request", cardRequest);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));
        attachJson("Response", result);

        assertNotNull(result, "Result list should not be null");
        assertTrue(result.isEmpty(), "Card with 15 digits should not be created or returned");
    }

    @Test
    @Story("Card number validation")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("setMockResponse() - Should return empty list when card number has 17 digits")
    void setMockResponse_ShouldReturnEmptyList_WhenCardNumberHas17Digits() {
        String token = emulator.getAuthToken();
        var cardRequest = BankCard.builder(44445555666677770L, CardType.DEBIT, Currency.USD).build();
        attachJson("Request", cardRequest);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));
        attachJson("Response", result);

        assertNotNull(result, "Result list should not be null");
        assertTrue(result.isEmpty(), "Card with 17 digits should not be created or returned");
    }

    @Test
    @Story("Required field validation")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("setMockResponse() - Should return empty list when card number is missing")
    void setMockResponse_ShouldReturnEmptyList_WhenCardNumberIsMissing() {
        String token = emulator.getAuthToken();
        var cardRequest = BankCard.builder(null, CardType.DEBIT, Currency.USD).build();
        attachJson("Request", cardRequest);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));
        attachJson("Response", result);

        assertNotNull(result, "Result list should not be null");
        assertTrue(result.isEmpty(), "Card should not be created without card number");
    }

    @Test
    @Story("Required field validation")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("setMockResponse() - Should return empty list when currency is missing")
    void setMockResponse_ShouldReturnEmptyList_WhenCurrencyIsMissing() {
        String token = emulator.getAuthToken();
        var cardRequest = BankCard.builder(4444555566667777L, CardType.DEBIT, null).build();
        attachJson("Request", cardRequest);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));
        attachJson("Response", result);

        assertNotNull(result, "Result list should not be null");
        assertTrue(result.isEmpty(), "Card should not be created without currency");
    }

    @Test
    @Story("Required field validation")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("setMockResponse() - Should return empty list when card type is missing")
    void setMockResponse_ShouldReturnEmptyList_WhenCardTypeIsMissing() {
        String token = emulator.getAuthToken();
        var cardRequest = BankCard.builder(4444555566667777L, null, Currency.EUR).build();
        attachJson("Request", cardRequest);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));
        attachJson("Response", result);

        assertNotNull(result, "Result list should not be null");
        assertTrue(result.isEmpty(), "Card should not be created without card type");
    }

    @Test
    @Story("Successful card creation")
    @Severity(SeverityLevel.CRITICAL)
    @Priority(PriorityLevel.CRITICAL)
    @DisplayName("setMockResponse() - Should successfully create a single bank card with all parameters")
    void setMockResponse_ShouldReturnFullCardDetails_WhenDataIsValid() {
        String token = emulator.getAuthToken();
        var cardRequest = BankCard.builder(4444555566667777L, CardType.CREDIT, Currency.USD)
                .cardStatus(true)
                .balance(BigDecimal.valueOf(1500.50))
                .build();
        attachJson("Request", cardRequest);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(cardRequest));
        attachJson("Response", result);

        verify("Verify single card details", () -> {
            Allure.step("Result contains exactly 1 card", () -> assertThat(result).hasSize(1));
            Allure.step("Card data matches request (ignoring cardId)", () ->
                    assertThat(result.getFirst())
                            .usingRecursiveComparison()
                            .ignoringFields("cardId")
                            .isEqualTo(cardRequest)
            );
        });
    }

    @Test
    @Story("Successful card creation")
    @Severity(SeverityLevel.CRITICAL)
    @Priority(PriorityLevel.HIGH)
    @DisplayName("setMockResponse() - Should successfully create multiple bank cards and match all data")
    void setMockResponse_ShouldReturnMatchedCards_WhenMultipleCardsAreAdded() {
        String token = emulator.getAuthToken();

        var card1 = BankCard.builder(1111222233334444L, CardType.DEBIT, Currency.EUR)
                .cardStatus(true).balance(BigDecimal.ZERO).build();
        var card2 = BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.JPY)
                .cardStatus(false).balance(BigDecimal.valueOf(9999)).build();
        List<BankCard> expectedCards = List.of(card1, card2);
        attachJson("Request", expectedCards);

        List<BankCard> actualCards = clientApi.setMockResponse(token, expectedCards);
        attachJson("Response", actualCards);

        assertNotNull(actualCards, "Result list should not be null");
        assertEquals(expectedCards.size(), actualCards.size(), "Cards count mismatch");

        verify("Verify cards match using AssertJ", () ->
                assertThat(actualCards)
                        .usingRecursiveFieldByFieldElementComparatorIgnoringFields("cardId")
                        .containsExactlyInAnyOrderElementsOf(expectedCards)
        );
    }

    @Test
    @Story("cardId auto-increment")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.HIGH)
    @DisplayName("setMockResponse() - Should assign incremented cardId when cards already exist")
    void setMockResponse_ShouldAssignIncrementedId_WhenCardsAlreadyExist() {
        String token = emulator.getAuthToken();

        emulator.addBankCards(token, List.of(
                BankCard.builder(1111222233334444L, CardType.DEBIT,   Currency.USD).build(),
                BankCard.builder(2222333344445555L, CardType.CREDIT,  Currency.EUR).build()
        ));

        var newCardRequest = BankCard.builder(9999888877776666L, CardType.DEBIT, Currency.USD)
                .cardStatus(true).balance(BigDecimal.valueOf(777.77)).build();
        attachJson("New card request", newCardRequest);

        List<BankCard> actualCards = clientApi.setMockResponse(token, List.of(newCardRequest));
        attachJson("Response (all cards)", actualCards);

        verify("Verify that the 3rd card has ID 3 and correct data", () -> {
            Allure.step("Total card count is 3",     () -> assertThat(actualCards).hasSize(3));
            Allure.step("New card has cardId = 3",   () -> assertThat(actualCards.getLast().getCardId()).isEqualTo(3L));
            Allure.step("New card data matches",     () -> assertThat(actualCards.getLast())
                    .usingRecursiveComparison().ignoringFields("cardId").isEqualTo(newCardRequest));
        });
    }

    @Test
    @Story("Card number boundary values")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("setMockResponse() - Card number at minimum boundary (1_000_000_000_000_000) should be accepted")
    void setMockResponse_ShouldAcceptCardNumber_AtMinBoundary() {
        String token = emulator.getAuthToken();
        var card = BankCard.builder(1_000_000_000_000_000L, CardType.DEBIT, Currency.USD).build();
        attachJson("Request", card);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(card));
        attachJson("Response", result);

        verify("Min boundary card number must be accepted", () -> assertThat(result).hasSize(1));
    }

    @Test
    @Story("Card number boundary values")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("setMockResponse() - Card number at maximum boundary (9_999_999_999_999_999) should be accepted")
    void setMockResponse_ShouldAcceptCardNumber_AtMaxBoundary() {
        String token = emulator.getAuthToken();
        var card = BankCard.builder(9_999_999_999_999_999L, CardType.DEBIT, Currency.USD).build();
        attachJson("Request", card);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(card));
        attachJson("Response", result);

        verify("Max boundary card number must be accepted", () -> assertThat(result).hasSize(1));
    }

    @Test
    @Story("Optional field defaults")
    @Severity(SeverityLevel.MINOR)
    @Priority(PriorityLevel.LOW)
    @DisplayName("setMockResponse() - cardStatus defaults to null when not provided")
    void setMockResponse_ShouldStoreNullCardStatus_WhenNotProvided() {
        String token = emulator.getAuthToken();
        var card = BankCard.builder(4444555566667777L, CardType.DEBIT, Currency.USD).build();
        attachJson("Request", card);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(card));
        attachJson("Response", result);

        verify("cardStatus must be null when not set", () ->
                assertNull(result.getFirst().getCardStatus(), "cardStatus should be null when not provided")
        );
    }

    @Test
    @Story("Optional field defaults")
    @Severity(SeverityLevel.MINOR)
    @Priority(PriorityLevel.LOW)
    @DisplayName("setMockResponse() - balance defaults to null when not provided")
    void setMockResponse_ShouldStoreNullBalance_WhenNotProvided() {
        String token = emulator.getAuthToken();
        var card = BankCard.builder(4444555566667777L, CardType.DEBIT, Currency.USD).build();
        attachJson("Request", card);

        List<BankCard> result = clientApi.setMockResponse(token, List.of(card));
        attachJson("Response", result);

        verify("balance must be null when not set", () ->
                assertNull(result.getFirst().getBalance(), "balance should be null when not provided")
        );
    }

    @Test
    @Story("Append behaviour")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("setMockResponse() - Calling twice should accumulate cards, not replace them")
    void setMockResponse_ShouldAccumulateCards_OnMultipleCalls() {
        String token = emulator.getAuthToken();

        clientApi.setMockResponse(token, List.of(BankCard.builder(1111222233334444L, CardType.DEBIT,  Currency.USD).build()));
        List<BankCard> result = clientApi.setMockResponse(token, List.of(BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.EUR).build()));
        attachJson("Response after 2nd call", result);

        verify("Second call must append, not replace", () -> assertThat(result).hasSize(2));
    }
}