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
 * Data Transfer Object for the root bank card list response.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankCardListResponse {
    @JsonProperty("response")
    private ResponseContainer response;

    public static BankCardListResponse of(List<BankCardResponse> cards) {
        List<BankCardResponse> mutableCards = (cards != null) ? new ArrayList<>(cards) : new ArrayList<>();
        return BankCardListResponse.builder()
                .response(ResponseContainer.builder()
                        .size(mutableCards.size())
                        .cards(mutableCards)
                        .build())
                .build();
    }

    public List<BankCard> toDomainList() {
        if (this.response == null || this.response.getCards() == null) {
            return Collections.emptyList();
        }
        return this.response.getCards().stream()
                .map(BankCardResponse::getBankCard)
                .toList();
    }

    /**
     * Inner container holding the actual list of bank cards and metadata.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseContainer {
        @JsonProperty("size")
        private Integer size;

        @JsonProperty("cards")
        private List<BankCardResponse> cards;
    }
}
