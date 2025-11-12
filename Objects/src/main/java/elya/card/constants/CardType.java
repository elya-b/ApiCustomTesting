package elya.card.constants;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum CardType {
    DEBIT(0, "debit"),
    CREDIT(1, "credit");


    private final int id;
    private final String name;

    CardType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static Optional<CardType> getById(int id) {
        return Arrays.stream(CardType.values())
                .filter(cardType -> cardType.id == id)
                .findFirst();
    }

    public static Optional<CardType> getByName(String name) {
        String upperName = name.toUpperCase(Locale.ROOT);
        return Arrays.stream(CardType.values())
                .filter(cardType -> cardType.name.toUpperCase(Locale.ROOT).equals(upperName))
                .findFirst();
    }
}
