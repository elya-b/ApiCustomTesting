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

import static elya.constants.ApiEndpoints.URL_BANK_CARD_DATA;
import static elya.constants.enums.HttpHeaderValues.BEARER;
import static elya.enums.responsemodel.ApiBankCards.CARDS;
import static elya.enums.responsemodel.ApiBankCards.RESPONSE;
import static elya.restclient.constants.logs.ExceptionMessage.GET_CARDS_EXCEPTION;
import static elya.restclient.constants.logs.ExceptionMessage.UNEXPECTED_JSON_EXCEPTION;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Implementation of the bank card API client.
 * Responsible for retrieving and parsing financial card data associated with a user session.
 */
@Slf4j
@Component
public class BankCardClient implements IBankCardApi {

    private final IRestClientApi clientApi;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes the BankCardClient with a REST API execution engine.
     *
     * @param clientApi the low-level API client used for executing HTTP requests
     */
    public BankCardClient(IRestClientApi clientApi) {
        this.clientApi = clientApi;
    }

    /**
     * Retrieves a list of bank cards associated with the provided authorization token.
     *
     * @param token authentication bearer token
     * @return      list of BankCardResponse objects
     * @throws RestClientException if the request fails or JSON parsing of the card array is unsuccessful
     */
    @Override
    public BankCardListResponse getApiBankCards(String token) {
        System.out.println("!!! TRYING TO FETCH FROM EMULATOR !!!");
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
            log.error("Parsing error: {}", e.getMessage());
            throw new RestClientException(UNEXPECTED_JSON_EXCEPTION, e);
        }
    }

    /**
     * Retrieves a specific bank card by its ID.
     *
     * @param token  authentication bearer token
     * @param cardId unique identifier of the bank card
     * @return       Optional containing the BankCardResponse if found, otherwise empty
     * @throws RestClientException if the request fails during execution or parsing
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
            return Optional.empty();
        }
    }

    /**
     * Helper method to create authorization headers.
     *
     * @param token raw or bearer token
     * @return      map of HTTP headers
     */
    private Map<String, String> createHeaders(String token) {
        return Map.of(AUTHORIZATION, token.startsWith(BEARER.toString()) ? token : BEARER + token);
    }
}
