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

/**
 * Data Transfer Object for creating or updating bank card mock data.
 * <p>Includes strict validation rules for financial data, such as 16-digit card numbers
 * and mandatory enumeration fields (Type, Currency).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for defining a bank card mock instance")
public class BankCardRequest {

    /**
     * The unique 16-digit primary account number (PAN).
     * Validated to be within the range of standard 16-digit integers.
     */
    @Schema(description = "Card number", example = "4444333322221111")
    @NotNull
    @Range(
            min = 1_000_000_000_000_000L,
            max = 9_999_999_999_999_999L,
            message = "Card number must be 16 digits")
    private Long cardNumber;

    /**
     * The functional category of the card (e.g., DEBIT, CREDIT).
     */
    @Schema(description = "Card category", example = "DEBIT")
    @NotNull(message = "Card type is mandatory")
    private CardType cardType;

    /**
     * The operational status of the card.
     * If true, the card is considered active.
     */
    @Schema(description = "Status", example = "true")
    private Boolean cardStatus;

    /**
     * The three-letter ISO currency code associated with the card account.
     */
    @Schema(description = "Currency code", example = "USD")
    @NotNull(message = "Currency is mandatory")
    private Currency currency;

    /**
     * The current available funds on the card.
     */
    @Schema(description = "Balance", example = "9999.99")
    private BigDecimal balance;

    /**
     * Converts this request DTO into a {@link BankCard} domain model.
     * This method is ignored by Jackson during serialization.
     *
     * @return a domain {@link BankCard} instance.
     */
    @JsonIgnore
    public BankCard toDomain() {
        return BankCard.builder(this.cardNumber, this.cardType, this.currency)
                .cardStatus(this.cardStatus)
                .balance(this.balance)
                .build();
    }

    /**
     * Static factory method to create a request DTO from an existing domain model.
     * Useful for pre-filling update forms or mirroring current state.
     *
     * @param domain the {@link BankCard} domain entity.
     * @return a mapped {@link BankCardRequest} instance, or {@code null} if the input is null.
     */
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