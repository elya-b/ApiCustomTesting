package elya.card.constants;

import java.util.Arrays;
import java.util.Optional;

/**
 * Functional interface providing a generic utility to retrieve enum constants by their numeric identifier.
 * <p>This utility works with any {@link Enum} that implements the {@link Identifiable} interface,
 * ensuring a consistent lookup mechanism across different constant types.</p>
 */
public interface GetById {

    /**
     * Searches for an enum constant of the specified class that matches the given ID.
     *
     * @param <E>       the type of the enum, which must implement {@link Identifiable}.
     * @param enumClass the {@link Class} object of the enum type to search in.
     * @param id        the numeric identifier to look for.
     * @return an {@link Optional} containing the matching enum constant,
     * or {@link Optional#empty()} if no match is found.
     */
    static <E extends Enum<E> & Identifiable> Optional<E> getById(Class<E> enumClass, int id) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.getId() == id)
                .findFirst();
    }
}