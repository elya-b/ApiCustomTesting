package elya.card.constants;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

/**
 * Enumeration of supported bank card categories.
 * <p>Implements {@link Identifiable} for database or internal indexing and
 * {@link Nameable} for human-readable representation and JSON serialization.</p>
 */
@AllArgsConstructor
public enum CardType implements Identifiable, Nameable {

    /**
     * Represents a standard debit card linked to a checking or savings account.
     */
    DEBIT(0, "debit"),

    /**
     * Represents a credit card with an associated credit limit.
     */
    CREDIT(1, "credit");

    /**
     * The internal numeric identifier for the card type.
     */
    private final int id;

    /**
     * The string label used for API communication and display.
     */
    private final String name;

    /**
     * Returns the numeric ID associated with the card type.
     *
     * @return the integer identifier.
     */
    @Override
    public int getId() {
        return id;
    }

    /**
     * Returns the string name of the card type.
     * <p>Annotated with {@link JsonValue} to ensure this string is used
     * as the value during JSON serialization.</p>
     *
     * @return the name string.
     */
    @Override
    @JsonValue
    public String getName() {
        return name;
    }
}