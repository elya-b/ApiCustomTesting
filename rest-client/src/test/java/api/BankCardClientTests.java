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
        var emptyResponse = Map.of(RESPONSE.name(), Map.of());
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

    /**
     * Helper method to convert objects to JsonNode for mocking clientApi responses.
     */
    private JsonNode toJson(Object obj) {
        return objectMapper.valueToTree(obj);
    }
}
