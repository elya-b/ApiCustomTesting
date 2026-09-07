package elya.card.constants;

/**
 * Interface for objects that possess a human-readable or system-defined string name.
 * <p>Typically implemented by constants and enumerations to provide a consistent
 * string value for logging, display, or JSON serialization.</p>
 */
public interface Nameable {

    /**
     * Retrieves the string-based name or label associated with the object.
     *
     * @return the name string.
     */
    String getName();
}