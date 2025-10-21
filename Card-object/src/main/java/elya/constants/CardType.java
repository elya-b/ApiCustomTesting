package elya.constants;

import java.util.Arrays;
import java.util.Optional;

public enum CardType {
    DEBIT(0),

    CREDIT(1);


    private final int id;

    CardType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Optional<CardType> getById(int id) {
        return Arrays.stream(CardType.values())
                .filter(cardType -> cardType.id == id)
                .findFirst();
    }
}
