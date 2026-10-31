package elya.dto.bankcard;

import com.fasterxml.jackson.annotation.*;
import elya.card.*;
import lombok.*;

import java.util.*;

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
     * Creates a response from a list of card DTOs.
     * <p>Keeps an unmodifiable copy of the cards: later changes to the source list do not reach
     * the response, and the response cannot be changed through its getters. This makes an instance
     * safe to share, e.g. as a constant empty response.</p>
     *
     * @param cards the list of {@link BankCardResponse} objects; {@code null} gives an empty response.
     * @return a fully initialized {@link BankCardListResponse}.
     * @throws NullPointerException if the list contains a {@code null} card.
     */
    public static BankCardListResponse of(List<BankCardResponse> cards) {
        List<BankCardResponse> copy = (cards != null) ? List.copyOf(cards) : List.of();
        return BankCardListResponse.builder()
                .response(ResponseContainer.builder()
                        .size(copy.size())
                        .cards(copy)
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