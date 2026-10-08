package elya.restclient.constants.logs;

import lombok.experimental.UtilityClass;

/**
 * Centralized dictionary for high-level exception messages within the REST client.
 * <p>Most messages carry a category prefix (Client, Data, Transport) telling the reader
 * where the failure originated. The two ID-based entries are prefixes: the offending
 * identifier is appended at the throw site.</p>
 */
@UtilityClass
public class ExceptionMessage {

    /** Error prefix used when a card deletion operation fails on the server side. */
    public static final String CAN_NOT_DELETE_CARD_WITH_ID =
            "Can not delete card with ID: ";

    /** Error prefix used when a requested card identifier does not exist. */
    public static final String CARD_NOT_FOUND_WITH_ID =
            "Card not found with ID: ";

    /** Logged when the authentication handshake or token retrieval fails. */
    public static final String GENERATE_TOKEN_EXCEPTION =
            "Client Error: Unable to generate authentication token.";

    /** Logged when the client fails to fetch the list of bank cards from the remote server. */
    public static final String GET_CARDS_EXCEPTION =
            "Client Error: Failed to retrieve bank cards from the server.";

    /** Logged when the API response body cannot be mapped to the local DTO models. */
    public static final String UNEXPECTED_JSON_EXCEPTION =
            "Data Error: The server returned a JSON structure that doesn't match the expected model.";

    /** Thrown when the response cannot be classified because it carries no usable HTTP status. */
    public static final String NO_HTTP_STATUS_EXCEPTION =
            "Transport Error: The response carries no usable HTTP status code.";
}