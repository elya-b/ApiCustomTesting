package dto.bankcard;

import elya.card.constants.CardType;
import elya.card.constants.Currency;
import elya.dto.bankcard.BankCardRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BankCardRequestTests {

    @Test
    @DisplayName("toDomain() - Should map all DTO fields to BankCard domain model")
    void toDomain_ShouldMapAllFields() {
        var request = BankCardRequest.builder()
                .cardNumber(4444333322221111L)
                .cardType(CardType.DEBIT)
                .cardStatus(true)
                .currency(Currency.USD)
                .balance(new java.math.BigDecimal("1500.51"))
                .build();

        var domain = request.toDomain();

        assertAll("Verify domain mapping",
                () -> assertEquals(request.getCardNumber(), domain.getCardNumber()),
                () -> assertEquals(request.getCardType(), domain.getCardType()),
                () -> assertEquals(request.getCardStatus(), domain.getCardStatus()),
                () -> assertEquals(request.getCurrency(), domain.getCurrency()),
                () -> assertEquals(request.getBalance(), domain.getBalance())
        );
    }
}
