package elya.dto.bankcard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import elya.card.BankCard;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing bank card details received from the API.
 * Includes functionality to map the DTO to the internal BankCard domain model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Slf4j
@Schema(description = "Information about a specific bank card")
public class BankCardResponse {

    @Schema(
            description = "Unique identifier of the card",
            example = "1"
    )
    @NotNull(message = "Card ID is mandatory")
    @JsonProperty("cardId")
    private Long cardId;

    @Schema(
            description = "Masked or full card number",
            example = "12345678"
    )
    @NotNull
    @Range(
            min = 1_000_000_000_000_000L,
            max = 9_999_999_999_999_999L,
            message = "Card number must be 16 digits")
    @JsonProperty("cardNumber")
    private Long cardNumber;

    @Schema(
            description = "The category of the bank card",
            example = "debit",
            allowableValues = {"debit", "credit"}
    )
    @JsonProperty("cardType")
    @NotNull(message = "Card type is mandatory")
    private CardType cardType;

    @Schema(
            description = "Current operational status of the card (active/inactive)",
            example = "true"
    )
    @JsonProperty("cardStatus")
    private Boolean cardStatus;

    @Schema(
            description = "Account currency code",
            example = "USD",
            allowableValues = {"USD", "EUR", "JPY"}
    )
    @NotNull(message = "Currency is mandatory")
    @JsonProperty("currency")
    private Currency currency;

    @Schema(
            description = "Current account balance",
            example = "9999.99"
    )
    @JsonProperty("balance")
    private BigDecimal balance;

    /**
     * Converts the current DTO into a BankCard domain object.
     *
     * @return a populated BankCard object
     * @throws IllegalArgumentException if card type or currency cannot be parsed
     */
    @JsonIgnore
    public BankCard getBankCard() {
        var bankCard = BankCard.builder(cardNumber, cardType, currency)
                .cardId(cardId)
                .cardStatus(cardStatus)
                .balance(balance)
                .build();

        return bankCard;
    }

    public static BankCardResponse fromDomain(BankCard domain, Long id) {
        return BankCardResponse.builder()
                .cardId(id)
                .cardNumber(domain.getCardNumber())
                .cardType(domain.getCardType())
                .cardStatus(domain.getCardStatus())
                .currency(domain.getCurrency())
                .balance(domain.getBalance())
                .build();
    }
}
