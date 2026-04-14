package elya.card.constants;

import java.util.Arrays;
import java.util.Optional;

public interface GetById {
    static <E extends Enum<E> & Identifiable> Optional<E> getById(Class<E> enumClass, int id) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getId() == id)
                .findFirst();
    }
}
