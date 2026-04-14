package elya.constants.enums;

/**
 * Enumeration of JSON property names used for request payloads.
 * Mapping common fields like login and password to their respective JSON keys.
 */
public enum JsonProperty {
    LOGIN("login"),
    PASSWORD("password");

    private final String jsonProperty;

    /**
     * Initializes the enum constant with its corresponding JSON property name.
     *
     * @param jsonProperty the literal string name of the JSON property
     */
    JsonProperty (String jsonProperty) {
        this.jsonProperty = jsonProperty;
    }

    /**
     * Gets the raw string value of the JSON property.
     *
     * @return the property name as a string
     */
    public String getJsonProperty() {
        return jsonProperty;
    }

    /**
     * Returns the string representation of the JSON property.
     *
     * @return the property name as a string
     */
    @Override
    public String toString() {
        return jsonProperty;
    }
}
