package dto.bankcard;

import elya.card.*;
import elya.card.constants.*;
import elya.dto.bankcard.*;
import org.junit.jupiter.api.*;

import java.math.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link elya.dto.bankcard.BankCardResponse}.
 * <ul>
 *   <li>{@code getBankCard()} correctly maps all response fields to the {@link elya.card.BankCard} domain model</li>
 * </ul>
 */
public class BankCardResponseTests {

    @Test
    @DisplayName("Should correctly map BankCardResponse to BankCard domain model")
    void shouldMapToBankCard() {
        Long expectedCardId = 1L;
        Long expectedCardNumber = 4444555566667777L;
        CardType expectedType = CardType.DEBIT;
        Currency expectedCurrency = Currency.USD;
        BigDecimal expectedBalance = new BigDecimal("1500.51");
        Boolean expectedStatus = true;

        BankCardResponse response = BankCardResponse.builder()
                .cardId(expectedCardId)
                .cardNumber(expectedCardNumber)
                .cardType(expectedType)
                .cardStatus(expectedStatus)
                .currency(expectedCurrency)
                .balance(expectedBalance)
                .build();

        BankCard result = response.getBankCard();

        assertAll("Mapping validation",
                () -> assertEquals(expectedCardId, result.getCardId(), "Card ID mismatch"),
                () -> assertEquals(expectedCardNumber, result.getCardNumber(), "Card Number mismatch"),
                () -> assertEquals(expectedType, result.getCardType(), "Card Type mismatch"),
                () -> assertEquals(expectedStatus, result.getCardStatus(), "Card Status mismatch"),
                () -> assertEquals(expectedCurrency, result.getCurrency(), "Currency mismatch"),
                () -> assertEquals(expectedBalance, result.getBalance(), "Balance mismatch")
        );
    }
}