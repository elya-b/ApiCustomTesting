package elya.constants.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Enumeration of standardized JSON property names used in API request and response payloads.
 * Ensures consistent key mapping for authentication and user-related data transfer objects.
 */
@Getter
@AllArgsConstructor
public enum JsonProperty {

    /** * The JSON key representing the user's unique identifier or username. */
    LOGIN("login"),

    /** * The JSON key representing the user's secret authentication credential. */
    PASSWORD("password");

    /**
     * The actual string value of the JSON property key.
     */
    private final String jsonProperty;

    @Override
    public String toString() {
        return jsonProperty;
    }
}