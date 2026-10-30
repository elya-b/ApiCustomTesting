package dto.bankcard;

import elya.card.constants.*;
import elya.dto.bankcard.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link elya.dto.bankcard.BankCardRequest}.
 * <ul>
 *   <li>{@code toDomain()} maps all DTO fields to the {@link elya.card.BankCard} domain model correctly</li>
 * </ul>
 */
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