package elya.enums.responsemodel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Enumeration of top-level JSON keys used in bank card API response structures.
 * <p>These constants define the structural hierarchy for card-related responses,
 * ensuring consistent parsing between the emulator and its clients.</p>
 */
@Getter
@AllArgsConstructor
public enum ApiBankCards {

    /**
     * The root JSON key for the primary response object.
     */
    RESPONSE("response"),

    /**
     * The JSON key used for the collection of bank card entities within the response.
     */
    CARDS("cards");

    /**
     * The literal string value of the JSON key.
     */
    private final String key;

    @Override
    public String toString() {
        return key;
    }
}