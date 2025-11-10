package elya.objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import elya.card.BankCard;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Locale;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Slf4j
public class ApiEmulatorBankCard {

    @JsonProperty("CARD_ID")
    private Integer cardId;

    @JsonProperty("CARD_NUMBER")
    private Integer cardNumber;

    @JsonProperty("CARD_TYPE")
    private String cardType;

    @JsonProperty("CARD_STATUS")
    private Boolean cardStatus;

    @JsonProperty("CURRENCY")
    private String currency;

    @JsonProperty("BALANCE")
    private BigDecimal balance;

    public BankCard getBankCard() {
        var bankCard = BankCard.builder()
                .cardId(cardId)
                .cardNumber(cardNumber)
                .cardType(cardType.equalsIgnoreCase("debit") ? CardType.DEBIT : CardType.CREDIT)
                .cardStatus(cardStatus)
                .currency(parseCurrency(currency))
                .balance(balance)
                .build();

        return bankCard;
    }

    private Currency parseCurrency(String currency) {
        switch (currency.toUpperCase(Locale.ROOT)) {
            case "USD":
                return Currency.USD;
            case "EUR":
                return Currency.EUR;
                case "GPB":
                return Currency.GPB;
            default:
                log.error("Unknown currency: '{}'. Can't parse value", currency);
                throw new IllegalArgumentException("Unsupported currency " + currency);
        }
    }
}
