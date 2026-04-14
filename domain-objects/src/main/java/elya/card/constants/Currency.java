package elya.card.constants;

import java.util.Arrays;
import java.util.Optional;

public enum Currency {
    USD("USD", 0),
    EUR("EUR", 1),
    GPB("GPB", 2),
    ;

    private final String currencySymbol;
    private final int currencyId;

    Currency(String currencySymbol, int currencyId) {
        this.currencySymbol = currencySymbol;
        this.currencyId = currencyId;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public int getCurrencyId() {
        return currencyId;
    }

    public static Optional<Currency> getByCurrencySymbol(String currencySymbol) {
        return Arrays.stream(Currency.values())
                .filter(currency -> currency.currencySymbol.equals(currencySymbol))
                .findFirst();
    }

    public static Optional<Currency> getById(int currencyId) {
        return Arrays.stream(Currency.values())
                .filter(currency -> currency.currencyId == currencyId)
                .findFirst();
    }
}
