package elya.constants;

/**
 * Global constants representing API endpoint paths for the bank service and emulator.
 */
public class ApiEndpoints {
    /** Endpoint for obtaining authentication tokens. */
    public static final String URL_TOKEN = "/api/v1/elya-bank/auth/token";

    /** Endpoint for retrieving bank card information. */
    public static final String URL_BANK_CARD_DATA = "/api/v1/elya-bank/bank-cards/data";

    /** Endpoint for retrieving bank card information by ID. */
    public static final String URL_BANK_CARD_DATA_ID = "/api/v1/elya-bank/bank-cards/data/{cardId}";

    /** Endpoint for managing mock responses in the emulator. */
    public static final String URL_BANK_CARD_MOCK = "/api/v1/elya-bank/bank-cards/data";
}
