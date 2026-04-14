package elya.enums.responsemodel;

/**
 * Enumeration representing key constants for authentication token responses.
 * Used to identify and validate status fields within the authorization flow.
 */
public enum AuthToken {
    /** Indicates a successful token operation outcome. */
    SUCCESS("Success"),
    ;

    private final String responseKey;

    /**
     * Initializes the enum constant with its corresponding response key.
     *
     * @param responseKey the string literal representing the response status
     */
    AuthToken(String responseKey) {
        this.responseKey = responseKey;
    }

    /**
     * Returns the string representation of the response key.
     *
     * @return the literal response key value
     */
    @Override
    public String toString() {
        return responseKey;
    }
}
