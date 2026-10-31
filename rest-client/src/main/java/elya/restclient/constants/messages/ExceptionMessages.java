package elya.restclient.constants.messages;

import lombok.experimental.*;

/**
 * Centralized dictionary for high-level exception messages within the REST client.
 * <p>Most messages carry a category prefix (Client, Data, Transport) telling the reader
 * where the failure originated. The two ID-based entries are prefixes: the offending
 * identifier is appended at the throw site.</p>
 */
@UtilityClass
public class ExceptionMessages {

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

    /** Thrown when the body of a successful response cannot be parsed as JSON. */
    public static final String RESPONSE_NOT_JSON =
            "Data Error: The server returned a body that is not valid JSON.";

    /** Thrown when no HTTP response was received; the argument is the request URI. */
    public static final String TRANSPORT_FAILURE =
            "Transport Error: No HTTP response received from %s";
}