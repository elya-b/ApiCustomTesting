package elya.dto.bankcard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import elya.card.BankCard;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for setting bank card mock data")
public class BankCardRequest {

    @Schema(description = "Card number", example = "4444333322221111")
    @NotNull
    @Range(
            min = 1_000_000_000_000_000L,
            max = 9_999_999_999_999_999L,
            message = "Card number must be 16 digits")
    private Long cardNumber;

    @Schema(description = "Card category", example = "debit")
    @NotNull(message = "Card type is mandatory")
    private CardType cardType;

    @Schema(description = "Status", example = "true")
    private Boolean cardStatus;

    @Schema(description = "Currency code", example = "USD")
    @NotNull(message = "Currency is mandatory")
    private Currency currency;

    @Schema(description = "Balance", example = "9999.99")
    private BigDecimal balance;

    @JsonIgnore
    public BankCard toDomain() {
        return BankCard.builder(this.cardNumber, this.cardType, this.currency)
                .cardStatus(this.cardStatus)
                .balance(this.balance)
                .build();
    }

    public static BankCardRequest fromDomain(BankCard domain) {
        if (domain == null) return null;
        return BankCardRequest.builder()
                .cardNumber(domain.getCardNumber())
                .cardType(domain.getCardType())
                .currency(domain.getCurrency())
                .balance(domain.getBalance())
                .cardStatus(domain.getCardStatus())
                .build();
    }
}
