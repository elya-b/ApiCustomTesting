package elya.card.constants;

/**
 * Interface for objects that can be uniquely identified by a numeric ID.
 * <p>Commonly implemented by enumerations to facilitate mapping between
 * internal integer codes and domain constants.</p>
 */
public interface Identifiable {

    /**
     * Retrieves the unique numeric identifier associated with the object.
     *
     * @return the integer ID.
     */
    int getId();
}