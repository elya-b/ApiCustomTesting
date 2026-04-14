package dto.bankcard;

import elya.card.BankCard;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import elya.dto.bankcard.BankCardResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
