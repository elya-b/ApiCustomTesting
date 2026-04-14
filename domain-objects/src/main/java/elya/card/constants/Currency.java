package elya.card.constants;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Currency implements Identifiable, Nameable {
    USD("USD", 0),
    EUR("EUR", 1),
    JPY("JPY", 2),
    ;

    private final String currencySymbol;
    private final int currencyId;

    Currency(String currencySymbol, int currencyId) {
        this.currencySymbol = currencySymbol;
        this.currencyId = currencyId;
    }

    @Override
    public int getId() {
        return currencyId;
    }

    @Override
    @JsonValue
    public String getName() {
        return currencySymbol;
    }
}
