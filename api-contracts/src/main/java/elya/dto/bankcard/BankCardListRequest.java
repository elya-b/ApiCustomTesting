package elya.dto.bankcard;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object acting as a request wrapper for bulk bank card mocking.
 * <p>Used by the control API to seed the emulator with multiple card definitions
 * in a single operation. Includes validation for the collection and its elements.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request wrapper for a list of bank cards to be mocked")
public class BankCardListRequest {

    /**
     * The collection of bank card definitions provided by the client.
     * Each element is subject to nested {@link Valid} constraints.
     */
    @Valid
    @NotNull(message = "Cards list cannot be null")
    @Schema(description = "List of cards provided by the user")
    private List<BankCardRequest> cards;

    /**
     * Static factory method to create a request instance from a list of card DTOs.
     *
     * @param cards the list of {@link BankCardRequest} objects.
     * @return a new {@link BankCardListRequest} instance.
     */
    public static BankCardListRequest of(List<BankCardRequest> cards) {
        return BankCardListRequest.builder()
                .cards(cards)
                .build();
    }
}