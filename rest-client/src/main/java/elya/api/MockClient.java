package elya.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.apicontracts.IMockControlApi;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.interfaces.IRestClientApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static elya.constants.ApiEndpoints.URL_BANK_CARD_DATA;
import static elya.constants.ApiEndpoints.URL_BANK_CARD_MOCK;
import static elya.constants.enums.HttpHeaderValues.BEARER;
import static elya.restclient.constants.logs.ErrorLogs.FAILED_TO_CLEAR_MOCK_RESPONSE;
import static elya.restclient.constants.logs.ErrorLogs.FAILED_TO_SET_MOCK_RESPONSE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Implementation of the mock control API client.
 * Provides administrative methods to configure or clear server-side mock responses.
 */
@Slf4j
@Component
public class MockClient implements IMockControlApi {

    private final IRestClientApi clientApi;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initializes the MockControlClient with a REST API execution engine.
     *
     * @param clientApi the low-level API client used for executing HTTP requests
     */
    public MockClient(IRestClientApi clientApi) {
        this.clientApi = clientApi;
    }

    /**
     * Configures a mock response on the server using simplified request data.
     *
     * @param token   authorization session token
     * @param request simplified bank card mock data
     * @return        true if the mock was successfully set
     */
    @Override
    public BankCardListResponse setMockResponse(String token, BankCardListRequest request) {
        JsonNode jsonBody = objectMapper.valueToTree(request);
        Map<String, String> headers = Map.of(AUTHORIZATION, BEARER + token);

        JsonNode response = clientApi.post(URL_BANK_CARD_MOCK, jsonBody, headers);

        try {
            return objectMapper.treeToValue(response, BankCardListResponse.class);
        } catch (Exception e) {
            log.error(FAILED_TO_SET_MOCK_RESPONSE, e);
            throw new RuntimeException("Can not deserialize mock");
        }
    }

    /**
     * Clears all configured mock responses for the current session.
     *
     * @param token authorization session token
     * @return      true if the mock data was cleared successfully
     */
    @Override
    public boolean clearMockResponse(String token) {
        Map<String, String> headers = createHeaders(token);

        try {
            if (clientApi.delete(URL_BANK_CARD_MOCK, headers)) {
                log.info("Mock response cleared successfully via API");
                return true;
            }
        } catch (Exception e) {
            log.error("Technical error during clearing mock: {}", e.getMessage());
            return false;
        }

        log.error(FAILED_TO_CLEAR_MOCK_RESPONSE);
        return false;
    }

    /**
     * Removes a specific bank card from the mock response by its ID.
     *
     * @param token  authorization session token
     * @param cardId unique identifier of the card to be removed
     * @return       Optional containing the deleted card's ID if successful, otherwise empty
     */
    @Override
    public Optional<Long> deleteApiBankCardById(String token, Long cardId) {
        Map<String, String> headers = createHeaders(token);
        String url = URL_BANK_CARD_DATA + "/" + cardId;

        if (clientApi.delete(url, headers)) {
            log.info("Successfully deleted mock card with ID: [{}]", cardId);
            return Optional.of(cardId);
        }

        log.error("Failed to delete mock card with ID: [{}]", cardId);
        return Optional.empty();
    }

    /**
     * Helper method to create authorization headers.
     *
     * @param token raw token string
     * @return      map containing the Authorization header
     */
    private Map<String, String> createHeaders(String token) {
        return Map.of(AUTHORIZATION, BEARER + token);
    }
}
