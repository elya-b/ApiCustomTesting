package elya;

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
@Slf4j
public class ApiBankCard {

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

    public Integer getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(Integer cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public Boolean getCardStatus() {
        return cardStatus;
    }

    public void setCardStatus(Boolean cardStatus) {
        this.cardStatus = cardStatus;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BankCard getBankCard() {
        var bankCard = new BankCard();

        bankCard.setCardNumber(cardNumber);
        bankCard.setCardType(cardType.equalsIgnoreCase("debit") ? CardType.DEBIT : CardType.CREDIT);
        bankCard.setCardStatus(cardStatus);
        bankCard.setCurrency(parseCurrency(currency));
        bankCard.setBalance(balance);

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
