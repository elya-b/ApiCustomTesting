package elya.emulator.objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import elya.card.BankCard;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static elya.emulator.constants.excpetions.ApiExceptions.*;
import static elya.emulator.constants.logs.ApiErrorLogs.*;

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
        CardType parsedCardType = CardType.getByName(cardType)
                .orElseThrow(() -> new IllegalArgumentException(UNSUPPORTED_CARD_TYPE + cardType));

        var bankCard = BankCard.builder()
                .cardId(cardId)
                .cardNumber(cardNumber)
                .cardType(parsedCardType)
                .cardStatus(cardStatus)
                .currency(parseCurrency(currency))
                .balance(balance)
                .build();

        return bankCard;
    }

    private Currency parseCurrency(String currency) {
        Optional<Currency> foundCurrency = Currency.getByCurrencySymbol(currency.toUpperCase(Locale.ROOT));
        if (foundCurrency.isPresent()) {
            return foundCurrency.get();
        } else {
            log.error(CANT_PARSE_UNKNOWN_CURRENCY, currency);
            throw new IllegalArgumentException(UNSUPPORTED_CURRENCY + currency);
        }
    }
}
