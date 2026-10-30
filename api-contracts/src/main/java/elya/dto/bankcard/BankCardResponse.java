package elya.dto.bankcard;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import elya.card.BankCard;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing the bank card details as returned by the API.
 * <p>Extends the basic card information with a unique system identifier (cardId).
 * This DTO is used to provide clients with the current state of mocked or real cards.</p>
 * <p>Fields carry no Bean Validation: the server builds responses itself, so nothing validates them.
 * {@code requiredMode} documents which fields are always present in the OpenAPI contract.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Information about a specific bank card stored in the system")
public class BankCardResponse {

    /**
     * The internal unique identifier assigned to the card instance.
     */
    @Schema(
            description = "Unique identifier of the card record",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("cardId")
    private Long cardId;

    /**
     * The 16-digit primary account number.
     */
    @Schema(
            description = "16-digit card number",
            example = "1234567812345678",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("cardNumber")
    private Long cardNumber;

    /**
     * The classification of the card (e.g., DEBIT or CREDIT).
     */
    @Schema(
            description = "The category of the bank card",
            example = "debit",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("cardType")
    private CardType cardType;

    /**
     * Current availability status. If true, the card is active and can process transactions.
     */
    @Schema(
            description = "Current operational status of the card (active/inactive)",
            example = "true"
    )
    @JsonProperty("cardStatus")
    private Boolean cardStatus;

    /**
     * The ISO currency code defining the card's primary account balance.
     */
    @Schema(
            description = "Account currency code",
            example = "USD",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonProperty("currency")
    private Currency currency;

    /**
     * The amount of money currently available on the account.
     */
    @Schema(
            description = "Current account balance",
            example = "9999.99"
    )
    @JsonProperty("balance")
    private BigDecimal balance;

    /**
     * Maps the response DTO back to the internal {@link BankCard} domain model.
     * Useful for internal processing where a business entity is required instead of a DTO.
     *
     * @return a fully populated {@link BankCard} domain object.
     */
    @JsonIgnore
    public BankCard getBankCard() {
        return BankCard.builder(cardNumber, cardType, currency)
                .cardId(cardId)
                .cardStatus(cardStatus)
                .balance(balance)
                .build();
    }

    /**
     * Static factory method to create a response DTO from a domain model and an explicit ID.
     *
     * @param domain the {@link BankCard} business entity.
     * @param id     the unique identifier to be assigned in the response.
     * @return a mapped {@link BankCardResponse} instance.
     */
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