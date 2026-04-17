package elya.constants;

/**
 * Global constants representing API endpoint paths for the bank service and emulator.
 * Centralizes URI strings to ensure consistency across controllers and integration tests.
 */
public class ApiEndpoints {

    /**
     * Endpoint for issuing and validating session authentication tokens.
     */
    public static final String URL_TOKEN = "/api/v1/elya-bank/auth/token";

    /**
     * Core endpoint for bank card operations.
     * Acts as the base URI for retrieving card lists, seeding mock data,
     * and performing full mock storage cleanups.
     */
    public static final String URL_BANK_CARD_DATA = "/api/v1/elya-bank/bank-cards/data";

    /**
     * Parameterized endpoint for granular bank card operations.
     * Appends a template variable to {@link #URL_BANK_CARD_DATA} to target
     * specific resources by their unique identifier.
     */
    public static final String URL_BANK_CARD_DATA_ID = URL_BANK_CARD_DATA + "/{cardId}";
}