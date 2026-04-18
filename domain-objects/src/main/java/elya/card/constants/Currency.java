package elya.card.constants;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

/**
 * Enumeration of supported international currencies for bank card accounts.
 * <p>Implements {@link Identifiable} for internal logic/indexing and
 * {@link Nameable} for standard ISO currency code representation.</p>
 */
@AllArgsConstructor
public enum Currency implements Identifiable, Nameable {

    /** United States Dollar. */
    USD("USD", 0),

    /** Euro. */
    EUR("EUR", 1),

    /** Japanese Yen. */
    JPY("JPY", 2);

    /**
     * The ISO 4217 currency code or common symbol representation.
     */
    private final String currencySymbol;

    /**
     * The internal numeric identifier for the currency.
     */
    private final int currencyId;

    /**
     * Returns the internal numeric identifier for this currency.
     *
     * @return the integer identifier.
     */
    @Override
    public int getId() {
        return currencyId;
    }

    /**
     * Returns the string representation (symbol/code) of the currency.
     * <p>Annotated with {@link JsonValue} to ensure the currency code is used
     * as the value during JSON serialization and API communication.</p>
     *
     * @return the currency symbol string.
     */
    @Override
    @JsonValue
    public String getName() {
        return currencySymbol;
    }
}