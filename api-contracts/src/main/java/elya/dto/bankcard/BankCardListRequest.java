package elya.dto.bankcard;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request wrapper for a list of bank cards to be mocked")
public class BankCardListRequest {

    @Valid
    @NotNull(message = "Cards list cannot be null")
    @Schema(description = "List of cards provided by the user")
    private List<BankCardRequest> cards;

    public static BankCardListRequest of(List<BankCardRequest> cards) {
        return BankCardListRequest.builder()
                .cards(cards)
                .build();
    }
}
