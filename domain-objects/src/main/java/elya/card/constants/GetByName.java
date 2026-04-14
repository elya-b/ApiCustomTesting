package elya.card.constants;

import java.util.Arrays;
import java.util.Optional;

public interface GetByName {
    static <E extends Enum<E> & Nameable> Optional<E> getByName(Class<E> enumClass, String name, boolean ignoreCase) {
        if (enumClass == null) return Optional.empty();

        return Optional.ofNullable(name)
                .flatMap(e -> Arrays.stream(enumClass.getEnumConstants())
                        .filter(constantName -> ignoreCase
                                ? e.equalsIgnoreCase(constantName.getName())
                                : e.equals(constantName.getName()))
                        .findFirst());
    }
}
