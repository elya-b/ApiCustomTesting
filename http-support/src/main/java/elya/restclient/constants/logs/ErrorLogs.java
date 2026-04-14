package elya.restclient.constants.logs;

/**
 * Collection of error message templates used for logging within the HTTP client engine.
 * Contains placeholders for dynamic data such as URLs or status codes.
 */
public class ErrorLogs {

    /** HTTP Engine Errors */
    public static final String HTTP_REQUEST_FAILED =
            "HTTP request failed for URL: {}";
    public static final String UNKNOWN_HTTP_STATUS_CODE =
            "Unknown HTTP status code received for URL: {}. Message: {}";

    /** Mocking Errors */
    public static final String FAILED_TO_SET_MOCK_RESPONSE =
            "Technical Error: Could not send mock configuration to the emulator.";
    public static final String FAILED_TO_CLEAR_MOCK_RESPONSE =
            "Technical Error: Failed to request mock data clearing.";

    /** Serialization Errors */
    public static final String JSON_SERIALIZATION_FAILED =
            "Technical Error: Failed to serialize object to JSON string.";
}
