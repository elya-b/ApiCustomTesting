package elya.constants.enums;

/**
 * Enumeration of keys used to extract status-related information from API responses.
 * Maps to standard fields describing the HTTP status, its series, and the reason phrase.
 */
public enum StatusInfo {
    STATUS("status"),
    SERIES("series"),
    REASON("reason");

    private final String statusInfo;

    /**
     * Initializes the enum constant with its corresponding status info key.
     *
     * @param statusInfo the literal string name of the status property
     */
    StatusInfo (String statusInfo) {
        this.statusInfo = statusInfo;
    }

    /**
     * Returns the string representation of the status information key.
     *
     * @return the status property name as a string
     */
    @Override
    public String toString() {
        return statusInfo;
    }
}
