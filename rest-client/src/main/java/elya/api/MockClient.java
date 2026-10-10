package elya.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.apicontracts.IMockControlApi;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.interfaces.IRestClientApi;
import elya.restclient.exceptions.ApiHttpStatusException;
import elya.restclient.exceptions.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static elya.constants.ApiEndpoints.*;
import static elya.constants.enums.HttpHeaderValues.*;
import static elya.restclient.constants.logs.ErrorLogs.*;
import static org.springframework.http.HttpHeaders.*;

/**
 * Implementation of the mock control API client.
 * <p>Provides administrative methods to dynamically configure, clear,
 * or partially update server-side mock responses during runtime.</p>
 */
@Slf4j
@Component
public class MockClient implements IMockControlApi {

    private final IRestClientApi clientApi;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructs the client with a specific REST engine.
     *
     * @param clientApi the {@link IRestClientApi} implementation for HTTP calls.
     */
    public MockClient(IRestClientApi clientApi) {
        this.clientApi = clientApi;
    }

    /**
     * Configures a custom mock response on the server.
     * <p>Sends a list of cards to be stored in the emulator's volatile storage for the current session.</p>
     *
     * @param token   the security token.
     * @param request the collection of cards to mock.
     * @return the confirmed {@link BankCardListResponse} from the server.
     * @throws RestClientException if serialization or transmission fails.
     */
    @Override
    public BankCardListResponse setMockResponse(String token, BankCardListRequest request) {
        JsonNode jsonBody = objectMapper.valueToTree(request);
        Map<String, String> headers = createHeaders(token);

        JsonNode response = clientApi.post(URL_BANK_CARD_DATA, jsonBody, headers);

        try {
            return objectMapper.treeToValue(response, BankCardListResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.error(FAILED_TO_SET_MOCK_RESPONSE, e);
            throw new RestClientException("Failed to deserialize mock response configuration", e);
        }
    }

    /**
     * Resets the emulator state by clearing all mocked data for the session.
     *
     * @param token the security token.
     * @return always {@code true} — a rejected clear surfaces as an exception.
     * @throws ApiHttpStatusException if the server rejected the request.
     */
    @Override
    public boolean isResponseClear(String token) {
        clientApi.delete(URL_BANK_CARD_DATA, createHeaders(token));
        log.info("Mock response state cleared successfully.");
        return true;
    }

    /**
     * Deletes a specific card from the mocked set.
     *
     * @param token  the security token.
     * @param cardId the unique ID of the card to remove.
     * @return the deleted ID, always present — a missing card surfaces as a 404.
     * @throws ApiHttpStatusException if the card does not exist or the request was rejected.
     */
    @Override
    public Optional<Long> deleteApiBankCardById(String token, Long cardId) {
        clientApi.delete(URL_BANK_CARD_DATA + "/" + cardId, createHeaders(token));
        log.info("Successfully deleted mock card with ID: [{}]", cardId);
        return Optional.of(cardId);
    }

    /**
     * Helper to create standard Authorization headers.
     *
     * @param token the raw token string.
     * @return a map with the "Bearer <token>" header.
     */
    private Map<String, String> createHeaders(String token) {
        String authHeader = token.startsWith(BEARER.toString()) ? token : BEARER + token;
        return Map.of(AUTHORIZATION, authHeader);
    }
}