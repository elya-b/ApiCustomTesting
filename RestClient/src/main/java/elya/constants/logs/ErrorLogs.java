package elya.constants.logs;

public class ErrorLogs {

    /** FAILED */
    public static final String FAILED_TO_GENERATE_TOKEN = "Failed to generate token.";
    public static final String FAILED_TO_PARSE_BANK_CARDS_RESPONSE_NULL_OR_NOT_ARRAY = "Failed to parse bank cards response: result is null or not an array.";
    public static final String FAILED_TO_GENERATE_TOKEN_UNEXPECTED_JSON = "Failed to parse bank cards response: unexpected JSON structure.";
    public static final String FAILED_TO_GET_BANK_CARDS_DATA = "Failed to get bank cards data.";
    public static final String FAILED_TO_SET_MOCK_RESPONSE = "Failed to set mock response.";
    public static final String FAILED_TO_CLEAR_MOCK_RESPONSE = "Failed to clear mock response.";

    /** HTTP */
    public static final String HTTP_REQUEST_FAILED = "HTTP request failed for URL: {}";
    public static final String UNKNOWN_HTTP_STATUS_CODE = "Unknown HTTP status code received for URL: {}. Code: {}";
}
