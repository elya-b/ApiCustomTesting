package elya.card.constants;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum CardType implements Identifiable, Nameable {
    DEBIT(0, "debit"),
    CREDIT(1, "credit");

    private final int id;
    private final String name;

    @Override
    public int getId() {
        return id;
    };

    @Override
    @JsonValue
    public String getName() {
        return name;
    }
}
