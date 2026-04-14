package elya.card;

import elya.card.constants.CardType;
import elya.card.constants.Currency;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class BankCard {
    private Long cardId;
    private Long cardNumber;
    private CardType cardType;
    private Boolean cardStatus;
    private Currency currency;
    private BigDecimal balance;

    public static BankCardBuilder builder(Long cardNumber, CardType cardType, Currency currency) {
        return new BankCardBuilder()
                .cardNumber(cardNumber)
                .cardType(cardType)
                .currency(currency);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BankCardBuilder {
        private Long cardId;
        private Long cardNumber;
        private CardType cardType;
        private Boolean cardStatus;
        private Currency currency;
        private BigDecimal balance;

        public BankCardBuilder cardId(Long cardId) { this.cardId = cardId; return this; }
        public BankCardBuilder cardStatus(Boolean cardStatus) { this.cardStatus = cardStatus; return this; }
        public BankCardBuilder balance(BigDecimal balance) { this.balance = balance; return this; }

        private BankCardBuilder cardNumber(Long cardNumber) { this.cardNumber = cardNumber; return this; }
        private BankCardBuilder cardType(CardType cardType) { this.cardType = cardType; return this; }
        private BankCardBuilder currency(Currency currency) { this.currency = currency; return this; }

        public BankCard build() {
            return new BankCard(cardId, cardNumber, cardType, cardStatus, currency, balance);
        }
    }
}