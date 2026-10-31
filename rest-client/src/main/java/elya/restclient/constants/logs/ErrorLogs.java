package elya.restclient.constants.logs;

import lombok.experimental.*;

/**
 * Collection of error message templates used for logging within the HTTP client engine.
 * <p>Contains standardized strings and placeholders for dynamic data such as URLs,
 * status codes, or technical details, ensuring consistent diagnostic output.</p>
 */
@UtilityClass
public class ErrorLogs {

    /** Logged when the mock configuration cannot be transmitted to the emulator. */
    public static final String FAILED_TO_SET_MOCK_RESPONSE =
            "Technical Error: Could not send mock configuration to the emulator.";

    /** Logged when an object cannot be converted into a valid JSON string. */
    public static final String JSON_SERIALIZATION_FAILED =
            "Technical Error: Failed to serialize object to JSON string.";

    /** General log for unexpected parsing or data processing errors. */
    public static final String PARSING_ERROR = "Parsing error: {}";
}