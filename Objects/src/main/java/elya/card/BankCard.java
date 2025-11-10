package elya.card;

import elya.card.constants.CardType;
import elya.card.constants.Currency;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode
@ToString
public class BankCard {
    private Integer cardId;
    private Integer cardNumber;
    private CardType cardType;
    private Boolean cardStatus;
    private Currency currency;
    private BigDecimal balance;
}