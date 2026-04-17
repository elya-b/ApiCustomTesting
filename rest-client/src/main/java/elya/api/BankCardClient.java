package elya.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.RestClientApiHelper;
import elya.apicontracts.IBankCardApi;
import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardResponse;
import elya.interfaces.IRestClientApi;
import elya.restclient.constants.logs.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static elya.constants.ApiEndpoints.*;
import static elya.constants.enums.HttpHeaderValues.*;
import static elya.enums.responsemodel.ApiBankCards.*;
import static elya.restclient.constants.logs.ErrorLogs.*;
import static elya.restclient.constants.logs.ExceptionMessage.*;
import static org.springframework.http.HttpHeaders.*;

/**
 * Implementation of the bank card API client.
 * <p>Handles retrieval, filtering, and parsing of bank card data.
 * Provides robust JSON path navigation and session-based authentication.</p>
 */
@Slf4j
@Component
public class BankCardClient implements IBankCardApi {

    private final IRestClientApi clientApi;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructs the client with a specific REST engine.
     *
     * @param clientApi the {@link IRestClientApi} implementation for HTTP calls.
     */
    public BankCardClient(IRestClientApi clientApi) {
        this.clientApi = clientApi;
    }

    /**
     * Fetches all bank cards associated with the current session.
     * <p>Parses the response using a specific JSON path to isolate the card array.
     * Returns an empty list wrapper if no cards are found.</p>
     *
     * @param token the security token (with or without Bearer prefix).
     * @return a {@link BankCardListResponse} containing the list of cards.
     * @throws RestClientException if the server returns no content or the JSON structure is invalid.
     */
    @Override
    public BankCardListResponse getApiBankCards(String token) {
        Map<String, String> headers = createHeaders(token);
        JsonNode responseJson = clientApi.get(URL_BANK_CARD_DATA, headers);

        if (!RestClientApiHelper.hasContent(responseJson)) {
            throw new RestClientException(GET_CARDS_EXCEPTION);
        }

        try {
            JsonNode cardsArray = responseJson
                    .path(RESPONSE.toString())
                    .path(CARDS.toString());

            if (cardsArray.isMissingNode() || !cardsArray.isArray()) {
                return BankCardListResponse.of(Collections.emptyList());
            }

            List<BankCardResponse> cards = objectMapper.convertValue(
                    cardsArray,
                    new TypeReference<List<BankCardResponse>>() {}
            );

            return BankCardListResponse.of(cards);

        } catch (Exception e) {
            log.error(PARSING_ERROR, e.getMessage());
            throw new RestClientException(UNEXPECTED_JSON_EXCEPTION, e);
        }
    }

    /**
     * Retrieves a single bank card by its unique identifier.
     *
     * @param token  the security token.
     * @param cardId the unique ID of the card.
     * @return an {@link Optional} containing the card details, or empty if not found/error occurs.
     */
    @Override
    public Optional<BankCardResponse> getApiBankCardById(String token, Long cardId) {
        Map<String, String> headers = createHeaders(token);
        String url = URL_BANK_CARD_DATA + "/" + cardId;

        try {
            JsonNode responseJson = clientApi.get(url, headers);

            if (!RestClientApiHelper.hasContent(responseJson)) {
                return Optional.empty();
            }

            return Optional.ofNullable(objectMapper.convertValue(responseJson, BankCardResponse.class));
        } catch (Exception e) {
            log.warn("Failed to retrieve card by ID [{}]: {}", cardId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Formats and prepares the Authorization header.
     * <p>Ensures the token is correctly prefixed with "Bearer " if not already present.</p>
     *
     * @param token the raw token string.
     * @return a map containing the formatted Authorization header.
     */
    private Map<String, String> createHeaders(String token) {
        String authHeader = token.startsWith(BEARER.toString()) ? token : BEARER + token;
        return Map.of(AUTHORIZATION, authHeader);
    }
}