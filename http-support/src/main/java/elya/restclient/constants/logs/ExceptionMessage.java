package elya.restclient.constants.logs;

import lombok.experimental.UtilityClass;

/**
 * Centralized dictionary for high-level exception messages within the REST client.
 * <p>Messages are categorized by error type (Client, Data, Security) to provide
 * clear context when throwing custom {@code RestClientException} instances.</p>
 */
@UtilityClass
public class ExceptionMessage {

    /** Error prefix used when a card deletion operation fails on the server side. */
    public static final String CAN_NOT_DELETE_CARD_WITH_ID = "Can not delete card with ID: ";

    /** Error prefix used when a requested card identifier does not exist. */
    public static final String CARD_NOT_FOUND_WITH_ID = "Card not found with ID: ";

    /** Logged when the authentication handshake or token retrieval fails. */
    public static final String GENERATE_TOKEN_EXCEPTION = "Client Error: Unable to generate authentication token.";

    /** Logged when the client fails to fetch the list of bank cards from the remote server. */
    public static final String GET_CARDS_EXCEPTION = "Client Error: Failed to retrieve bank cards from the server.";

    /** Logged when the API response body cannot be mapped to the local DTO models. */
    public static final String UNEXPECTED_JSON_EXCEPTION = "Data Error: The server returned a JSON structure that doesn't match the expected model.";
}