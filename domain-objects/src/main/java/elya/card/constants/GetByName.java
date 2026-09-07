package elya.card.constants;

import java.util.Arrays;
import java.util.Optional;

/**
 * Functional interface providing a generic utility to retrieve enum constants by their string name.
 * <p>This utility works with any {@link Enum} that implements the {@link Nameable} interface,
 * allowing for flexible lookups based on custom string representations rather than just enum ordinal names.</p>
 */
public interface GetByName {

    /**
     * Searches for an enum constant of the specified class that matches the given name.
     * <p>Includes safe handling of null inputs and support for case-insensitive comparisons.</p>
     *
     * @param <E>        the type of the enum, which must implement {@link Nameable}.
     * @param enumClass  the {@link Class} object of the enum type to search in.
     * @param name       the string name to look for (can be null).
     * @param ignoreCase if {@code true}, performs a case-insensitive match;
     * if {@code false}, matching is case-sensitive.
     * @return an {@link Optional} containing the matching enum constant,
     * or {@link Optional#empty()} if no match is found or input is null.
     */
    static <E extends Enum<E> & Nameable> Optional<E> getByName(Class<E> enumClass, String name, boolean ignoreCase) {
        if (enumClass == null) return Optional.empty();

        return Optional.ofNullable(name)
                .flatMap(e -> Arrays.stream(enumClass.getEnumConstants())
                        .filter(constant -> ignoreCase
                                ? e.equalsIgnoreCase(constant.getName())
                                : e.equals(constant.getName()))
                        .findFirst());
    }
}