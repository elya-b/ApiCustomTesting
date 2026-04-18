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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for retrieving a card by ID (GET /bank-cards/data/{id}).
 * <ul>
 *   <li>Card found — returns the correct card with all fields matching</li>
 *   <li>Card not found — throws RuntimeException with a descriptive message</li>
 *   <li>Multiple cards present — returns exactly the card with the requested ID</li>
 *   <li>Card with optional fields — all fields are returned correctly</li>
 * </ul>
 */
@Epic("Bank Card API")
@Feature("GET /bank-cards/data/{id} — Retrieve Card By ID")
public class GetBankCardsDataById extends AbstractApiTest {

    @Test
    @Story("Card found")
    @Severity(SeverityLevel.CRITICAL)
    @Priority(PriorityLevel.CRITICAL)
    @DisplayName("getApiBankCardById() - Should return specific card with all matching parameters")
    void getApiBankCardById_ShouldReturnSpecificCard_WithAllParameters() {
        String token = emulator.getAuthToken();

        List<BankCard> seededCards = emulator.addBankCards(token, List.of(
                BankCard.builder(4444555566667777L, CardType.DEBIT,  Currency.USD).build(),
                BankCard.builder(1111222233334444L, CardType.CREDIT, Currency.EUR).build()
        ));
        attachJson("Seeded cards", seededCards);

        BankCard target = seededCards.stream()
                .filter(c -> c.getCardNumber().equals(4444555566667777L))
                .findFirst()
                .orElseThrow();

        BankCard actualCard = clientApi.getApiBankCardById(token, target.getCardId());
        attachJson("Response", actualCard);

        verify("Verify all fields match exactly", () ->
                assertThat(actualCard).usingRecursiveComparison().isEqualTo(target)
        );
    }

    @Test
    @Story("Card not found")
    @Severity(SeverityLevel.CRITICAL)
    @Priority(PriorityLevel.HIGH)
    @DisplayName("getApiBankCardById() - Should throw exception when card ID does not exist")
    void getApiBankCardById_ShouldThrowException_WhenCardIdDoesNotExist() {
        String token = emulator.getAuthToken();
        Long nonExistentId = 999999L;

        verify("Verify that requesting a non-existent ID results in a RuntimeException", () ->
                assertThatThrownBy(() -> clientApi.getApiBankCardById(token, nonExistentId))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Card not found with ID: " + nonExistentId)
        );
    }

    @Test
    @Story("Card found")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.HIGH)
    @DisplayName("getApiBankCardById() - Should return correct card when multiple cards are present")
    void getApiBankCardById_ShouldReturnCorrectCard_WhenMultipleCardsPresent() {
        String token = emulator.getAuthToken();

        List<BankCard> seeded = emulator.addBankCards(token, List.of(
                BankCard.builder(1111222233334444L, CardType.DEBIT,  Currency.USD).build(),
                BankCard.builder(5555666677778888L, CardType.CREDIT, Currency.EUR).build(),
                BankCard.builder(9999888877776666L, CardType.DEBIT,  Currency.JPY).build()
        ));
        attachJson("Seeded cards", seeded);

        BankCard target = seeded.get(1); // middle card
        BankCard result = clientApi.getApiBankCardById(token, target.getCardId());
        attachJson("Response", result);

        verify("Must return exactly the card with the requested ID", () ->
                assertThat(result).usingRecursiveComparison().isEqualTo(target)
        );
    }

    @Test
    @Story("Card found")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("getApiBankCardById() - Returned card must have all fields populated")
    void getApiBankCardById_ShouldReturnFullCardData() {
        String token = emulator.getAuthToken();

        var card = BankCard.builder(4444555566667777L, CardType.CREDIT, Currency.JPY)
                .cardStatus(false).balance(BigDecimal.valueOf(12345.67)).build();
        List<BankCard> seeded = emulator.addBankCards(token, List.of(card));
        attachJson("Seeded card", seeded);

        BankCard result = clientApi.getApiBankCardById(token, seeded.getFirst().getCardId());
        attachJson("Response", result);

        verify("All fields must match", () ->
                assertAll("Full card data validation",
                        () -> assertNotNull(result.getCardId(),                                   "cardId must not be null"),
                        () -> assertEquals(4444555566667777L,       result.getCardNumber()),
                        () -> assertEquals(CardType.CREDIT,         result.getCardType()),
                        () -> assertEquals(Currency.JPY,            result.getCurrency()),
                        () -> assertEquals(false,                   result.getCardStatus()),
                        () -> assertEquals(0, BigDecimal.valueOf(12345.67).compareTo(result.getBalance()))
                )
        );
    }
}