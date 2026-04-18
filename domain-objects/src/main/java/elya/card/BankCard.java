package elya.card;

import elya.card.constants.CardType;
import elya.card.constants.Currency;
import lombok.*;

import java.math.BigDecimal;

/**
 * Core domain model representing a bank card within the emulator system.
 * <p>This entity encapsulates all essential financial and identification data,
 * including security status, balance, and account categorization. It is designed
 * to be processed by the internal business logic before being mapped to response DTOs.</p>
 */
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

    /**
     * Static factory method to initiate a builder with mandatory card attributes.
     *
     * @param cardNumber the 16-digit card number.
     * @param cardType   the category of the card.
     * @param currency   the account currency.
     * @return a {@link BankCardBuilder} pre-populated with mandatory fields.
     */
    public static BankCardBuilder builder(Long cardNumber, CardType cardType, Currency currency) {
        return new BankCardBuilder()
                .cardNumber(cardNumber)
                .cardType(cardType)
                .currency(currency);
    }

    /**
     * Custom builder class for controlled instantiation of {@link BankCard} objects.
     * <p>Enforces a design where specific core fields must be provided via the
     * static {@code builder()} method, while optional fields can be set fluently.</p>
     */
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

        /**
         * Finalizes the build process and returns a new {@link BankCard} instance.
         *
         * @return a fully initialized bank card object.
         */
        public BankCard build() {
            return new BankCard(cardId, cardNumber, cardType, cardStatus, currency, balance);
        }
    }
}