package elya.dto.bankcard;

import com.fasterxml.jackson.annotation.*;
import elya.card.*;
import elya.card.constants.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.*;

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

    /** Smallest 16-digit card number. */
    private static final long CARD_NUMBER_MIN = 1_000_000_000_000_000L;

    /** Largest 16-digit card number. */
    private static final long CARD_NUMBER_MAX = 9_999_999_999_999_999L;

    /** Validation message for a card number outside the 16-digit range. */
    private static final String CARD_NUMBER_LENGTH_MESSAGE = "Card number must be 16 digits";

    /**
     * The unique 16-digit primary account number (PAN).
     * Validated to be within the range of standard 16-digit integers.
     */
    @Schema(description = "Card number", example = "4444333322221111")
    @NotNull(message = "Card number is mandatory")
    @Min(value = CARD_NUMBER_MIN, message = CARD_NUMBER_LENGTH_MESSAGE)
    @Max(value = CARD_NUMBER_MAX, message = CARD_NUMBER_LENGTH_MESSAGE)
    private Long cardNumber;

    /**
     * The functional category of the card (e.g., DEBIT, CREDIT).
     */
    @Schema(description = "Card category", example = "debit")
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