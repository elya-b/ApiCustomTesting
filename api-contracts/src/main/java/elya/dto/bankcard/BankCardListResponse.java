package elya.dto.bankcard;

import com.fasterxml.jackson.annotation.JsonProperty;
import elya.card.BankCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Root Data Transfer Object for bank card list responses.
 * <p>Wraps the card collection into a "response" container to match the legacy API schema.
 * Provides utility methods for bulk conversion and safe instance creation.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankCardListResponse {

    /**
     * Inner container holding the card collection and associated metadata.
     */
    @JsonProperty("response")
    private ResponseContainer response;

    /**
     * Static factory method to create a response from a list of card DTOs.
     * Ensures the internal collection is mutable and correctly calculates the total size.
     *
     * @param cards the list of {@link BankCardResponse} objects.
     * @return a fully initialized {@link BankCardListResponse}.
     */
    public static BankCardListResponse of(List<BankCardResponse> cards) {
        List<BankCardResponse> mutableCards = (cards != null) ? new ArrayList<>(cards) : new ArrayList<>();
        return BankCardListResponse.builder()
                .response(ResponseContainer.builder()
                        .size(mutableCards.size())
                        .cards(mutableCards)
                        .build())
                .build();
    }

    /**
     * Converts the nested DTO collection into a list of internal domain models.
     *
     * @return a list of {@link BankCard} domain objects; returns an empty list if response is null.
     */
    public List<BankCard> toDomainList() {
        if (this.response == null || this.response.getCards() == null) {
            return Collections.emptyList();
        }
        return this.response.getCards().stream()
                .map(BankCardResponse::getBankCard)
                .toList();
    }

    /**
     * Inner container representing the JSON "response" object.
     * Encapsulates the actual data list and its count for client-side pagination or verification.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseContainer {

        /**
         * The total number of cards included in the current response.
         */
        @JsonProperty("size")
        private Integer size;

        /**
         * The collection of detailed bank card information.
         */
        @JsonProperty("cards")
        private List<BankCardResponse> cards;
    }
}