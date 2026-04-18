package api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.api.BankCardClient;
import elya.dto.bankcard.BankCardListResponse;
import elya.interfaces.IRestClientApi;
import elya.restclient.constants.logs.RestClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static elya.constants.enums.HttpHeaderValues.BEARER;
import static elya.enums.responsemodel.ApiBankCards.CARDS;
import static elya.enums.responsemodel.ApiBankCards.RESPONSE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Unit tests (Mockito) for {@link elya.api.BankCardClient}.
 * <ul>
 *   <li>{@code getApiBankCards()} — returns a list of cards for a valid response</li>
 *   <li>{@code getApiBankCards()} — throws RestClientException when response is null</li>
 *   <li>{@code getApiBankCards()} — returns an empty list when the CARDS field is missing</li>
 *   <li>{@code getApiBankCardById()} — returns a card when the ID exists</li>
 *   <li>{@code getApiBankCardById()} — returns an empty Optional when response is null</li>
 *   <li>{@code getApiBankCards()} — throws RestClientException for incompatible JSON</li>
 *   <li>{@code getApiBankCards()} — returns an empty list when CARDS node is not an array</li>
 *   <li>{@code getApiBankCards()} — prepends "Bearer " to a token that has no prefix</li>
 *   <li>{@code getApiBankCards()} — does not double-prepend "Bearer " when token already has the prefix</li>
 *   <li>{@code getApiBankCardById()} — returns an empty Optional when clientApi throws an exception</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class BankCardClientTests {

    @Mock
    private IRestClientApi clientApi;

    @InjectMocks
    private BankCardClient bankCardClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOKEN = "token";
    private static final String BEARER_TOKEN = BEARER + TOKEN;
    private static final Long CARD_ID = 1L;

    @Test
    @DisplayName("getApiBankCards() - Should return list of cards when response is valid")
    void getApiBankCards_ShouldReturnList_WhenResponseIsValid() {
        var responseData = Map.of(RESPONSE.toString(), Map.of(CARDS.toString(), List.of(Map.of("cardId", CARD_ID))));
        when(clientApi.get(anyString(), anyMap())).thenReturn(toJson(responseData));

        BankCardListResponse result = bankCardClient.getApiBankCards(TOKEN);
        var listResult = result.getResponse().getCards();

        assertNotNull(listResult);
        assertFalse(listResult.isEmpty());
        assertEquals(1, listResult.size());

        verify(clientApi).get(anyString(), eq(Map.of(AUTHORIZATION, BEARER_TOKEN)));
    }

    @Test
    @DisplayName("getApiBankCards() - Should throw RestClientException when response has no content")
    void getApiBankCards_ShouldThrowException_WhenNoContent() {
        when(clientApi.get(anyString(), anyMap())).thenReturn(null);

        assertThrows(RestClientException.class, () ->
                bankCardClient.getApiBankCards(TOKEN)
        );
    }

    @Test
    @DisplayName("getApiBankCards() - Should return empty list when CARDS field is missing")
    void getApiBankCards_ShouldReturnEmptyList_WhenCardsMissing() {
        var emptyResponse = Map.of(RESPONSE.toString(), Map.of());
        when(clientApi.get(anyString(), anyMap())).thenReturn(toJson(emptyResponse));

        var result = bankCardClient.getApiBankCards(TOKEN);
        var listResult = result.getResponse().getCards();

        assertTrue(listResult.isEmpty());
    }

    @Test
    @DisplayName("getApiBankCardById() - Should return card when ID exists")
    void getApiBankCardById_ShouldReturnCard_WhenIdExists() {
        var fakeCard = Map.of("cardId", CARD_ID);
        when(clientApi.get(anyString(), anyMap())).thenReturn(toJson(fakeCard));

        var result = bankCardClient.getApiBankCardById(TOKEN, CARD_ID);

        assertTrue(result.isPresent());
        assertEquals(CARD_ID, result.get().getCardId());

        verify(clientApi).get(contains(CARD_ID.toString()), anyMap());
    }

    @Test
    @DisplayName("getApiBankCardById() - Should return empty Optional when no content")
    void getApiBankCardById_ShouldReturnEmpty_WhenNoContent() {
        when(clientApi.get(anyString(), anyMap())).thenReturn(null);

        var result = bankCardClient.getApiBankCardById(TOKEN, CARD_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getApiBankCards() - Should throw RestClientException when JSON content is incompatible")
    void getApiBankCards_ShouldThrowException_WhenJsonIsInvalid() {
        var invalidResponse = Map.of(RESPONSE.toString(), Map.of(CARDS.toString(), List.of("Invalid Data Type")));
        when(clientApi.get(anyString(), anyMap())).thenReturn(toJson(invalidResponse));

        assertThrows(RestClientException.class, () ->
                bankCardClient.getApiBankCards(TOKEN)
        );
    }

    // --- ADDITIONAL CASES ---

    @Test
    @DisplayName("getApiBankCards() - Should return empty list when CARDS node is not an array")
    void getApiBankCards_ShouldReturnEmptyList_WhenCardsNodeIsNotArray() {
        // CARDS field is present but not an array (e.g. a plain string)
        var response = Map.of(RESPONSE.toString(), Map.of(CARDS.toString(), "not-an-array"));
        when(clientApi.get(anyString(), anyMap())).thenReturn(toJson(response));

        var result = bankCardClient.getApiBankCards(TOKEN);

        assertTrue(result.getResponse().getCards().isEmpty(),
                "Non-array CARDS node must yield an empty list");
    }

    @Test
    @DisplayName("getApiBankCards() - Should prepend 'Bearer ' when token has no prefix")
    void getApiBankCards_ShouldPrependBearerPrefix_WhenTokenHasNone() {
        var responseData = Map.of(RESPONSE.toString(), Map.of(CARDS.toString(), List.of()));
        when(clientApi.get(anyString(), anyMap())).thenReturn(toJson(responseData));

        bankCardClient.getApiBankCards(TOKEN);

        // Verify that the Authorization header contains the full "Bearer <token>" value
        verify(clientApi).get(anyString(), eq(Map.of(AUTHORIZATION, BEARER_TOKEN)));
    }

    @Test
    @DisplayName("getApiBankCards() - Should NOT double-prepend 'Bearer ' when token already has prefix")
    void getApiBankCards_ShouldNotDoublePrependBearer_WhenTokenAlreadyPrefixed() {
        String alreadyPrefixed = BEARER_TOKEN; // "Bearer token"
        var responseData = Map.of(RESPONSE.toString(), Map.of(CARDS.toString(), List.of()));
        when(clientApi.get(anyString(), anyMap())).thenReturn(toJson(responseData));

        bankCardClient.getApiBankCards(alreadyPrefixed);

        // Must send "Bearer token", NOT "Bearer Bearer token"
        verify(clientApi).get(anyString(), eq(Map.of(AUTHORIZATION, BEARER_TOKEN)));
    }

    @Test
    @DisplayName("getApiBankCardById() - Should return empty Optional when exception is thrown by clientApi")
    void getApiBankCardById_ShouldReturnEmpty_WhenClientApiThrows() {
        when(clientApi.get(anyString(), anyMap())).thenThrow(new RuntimeException("connection error"));

        var result = bankCardClient.getApiBankCardById(TOKEN, CARD_ID);

        assertTrue(result.isEmpty(),
                "Exception from clientApi must be caught and return empty Optional");
    }

    private JsonNode toJson(Object obj) {
        return objectMapper.valueToTree(obj);
    }
}