package elya.emulator.constants.logs;

public class ApiInfoLogs {
    /**
     * TOKEN VALIDATION SERVICE
     */
    public static final String TOKEN_VALIDATION_SERVICE_START =
            "TokenValidationService: Lifecycle START. Token storage is initialized and empty."; // START
    public static final String TOKEN_VALIDATION_SERVICE_STOP =
            "TokenValidationService: Lifecycle STOP. All active tokens have been invalidated."; // STOP

    /**
     * MOCK RESPONSE SERVICE
     */
    public static final String MOCK_RESPONSE_SERVICE_START =
            "MockResponseService: Lifecycle START. Ready to receive mock configurations."; // START
    public static final String MOCK_RESPONSE_SERVICE_STOP =
            "MockResponseService: Lifecycle STOP. Mocked data has been cleared."; // STOP
}
