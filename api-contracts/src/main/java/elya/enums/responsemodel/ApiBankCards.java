package elya.enums.responsemodel;

/**
 * Enumeration of JSON keys used in bank card API response structures.
 * Provides a centralized way to reference field names for parsing and mapping.
 */
public enum ApiBankCards {
    RESPONSE("response"),
    CARDS("cards");

    private final String key;

    /**
     * Initializes the enum constant with its corresponding JSON key string.
     *
     * @param key the literal string key used in the JSON response
     */
    ApiBankCards(String key) {
        this.key = key;
    }

    /**
     * Returns the string representation of the JSON key.
     *
     * @return the key value
     */
    @Override
    public String toString() {
        return key;
    }
}
