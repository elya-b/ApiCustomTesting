package elya.card;

import elya.card.constants.CardType;
import elya.card.constants.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BankCard {
    private Integer cardNumber;
    private CardType cardType;
    private Boolean cardStatus;
    private Currency currency;
    private BigDecimal balance;

//    public Integer getCardNumber() {
//        return cardNumber;
//    }
//
//    public void setCardNumber(Integer cardNumber) {
//        this.cardNumber = cardNumber;
//    }
//
//    public String getCardType() {
//        return cardType;
//    }
//
//    public void setCardType(String cardType) {
//        this.cardType = cardType;
//    }
//
//    public Boolean getCardStatus() {
//        return cardStatus;
//    }
//
//    public void setCardStatus(Boolean cardStatus) {
//        this.cardStatus = cardStatus;
//    }
//
//    public String getCurrency() {
//        return currency;
//    }
//
//    public void setCurrency(String currency) {
//        this.currency = currency;
//    }
//
//    public BigDecimal getBalance() {
//        return balance;
//    }
//
//    public void setBalance(BigDecimal balance) {
//        this.balance = balance;
//    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BankCard bankCard = (BankCard) o;
        return Objects.equals(cardNumber, bankCard.cardNumber) && Objects.equals(cardType, bankCard.cardType) && Objects.equals(cardStatus, bankCard.cardStatus) && Objects.equals(currency, bankCard.currency) && Objects.equals(balance, bankCard.balance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardNumber, cardType, cardStatus, currency, balance);
    }

    @Override
    public String toString() {
        return "BankCard{" +
                "cardNumber=" + cardNumber +
                ", cardType='" + cardType + '\'' +
                ", cardStatus=" + cardStatus +
                ", currency='" + currency + '\'' +
                ", balance=" + balance +
                '}';
    }
}