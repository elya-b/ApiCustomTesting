package api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.api.MockClient;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardRequest;
import elya.dto.bankcard.BankCardResponse;
import elya.interfaces.IRestClientApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static elya.constants.ApiEndpoints.URL_BANK_CARD_DATA;
import static elya.constants.ApiEndpoints.URL_BANK_CARD_MOCK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MockClientTests {

    @Mock
    private IRestClientApi clientApi;

    @InjectMocks
    private MockClient mockClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Long CARD_ID = 1L;
    private static final String TOKEN = "token";
    private static final String DELETE_BY_ID_URL = URL_BANK_CARD_DATA + "/" + CARD_ID;
    private static final BankCardListRequest BANK_CARD_LIST_REQUEST = BankCardListRequest.builder()
                                                        .cards(List.of(BankCardRequest.builder().build()))
                                                        .build();
    private static final BankCardResponse CARD_RESPONSE = BankCardResponse.builder().cardId(CARD_ID).build();
    private static final BankCardListResponse CARD_LIST_RESPONSE = BankCardListResponse.of(List.of(CARD_RESPONSE));

    @Test
    @DisplayName("setMockResponse() - Should return response object when server returns content")
    void setMockResponse_ShouldReturnTrue_WhenResponseHasContent() {

        when(clientApi.post(anyString(), any(), anyMap())).thenReturn(toJson(CARD_LIST_RESPONSE));

        var result = mockClient.setMockResponse(TOKEN, BANK_CARD_LIST_REQUEST);

        assertNotNull(result, "Should return response object if server confirms operation");

        assertThat(result.getResponse().getCards())
                .as("Check cards in response")
                .hasSize(1)
                .first()
                .extracting(BankCardResponse::getCardId)
                .isEqualTo(CARD_ID);

        verify(clientApi).post(eq(URL_BANK_CARD_MOCK), any(JsonNode.class), anyMap());
    }

    @Test
    @DisplayName("setMockResponse() - Should return false when server returns null")
    void setMockResponse_ShouldReturnFalse_WhenResponseIsNull() {
        when(clientApi.post(anyString(), any(), anyMap())).thenReturn(null);

        var result = mockClient.setMockResponse(TOKEN, BANK_CARD_LIST_REQUEST);

        assertNull(result, "Should return null if server response is null");
        verify(clientApi).post(eq(URL_BANK_CARD_MOCK), any(JsonNode.class), anyMap());
    }

    @Test
    @DisplayName("deleteApiBankCardById() - Should return Optional with ID when delete is successful")
    void deleteApiBankCardById_ShouldReturnId_WhenSuccessful() {
        when(clientApi.delete(anyString(), anyMap())).thenReturn(true);

        var result = mockClient.deleteApiBankCardById(TOKEN, CARD_ID);

        assertAll("Verify delete by ID",
                () -> assertTrue(result.isPresent(), "Optional should contain deleted ID"),
                () -> assertEquals(CARD_ID, result.get(), "Deleted ID mismatch")
        );
        verify(clientApi).delete(eq(DELETE_BY_ID_URL), anyMap());
    }

    @Test
    @DisplayName("deleteApiBankCardById() - Should return empty Optional when delete fails")
    void deleteApiBankCardById_ShouldReturnEmpty_WhenDeleteFails() {
        when(clientApi.delete(anyString(), anyMap())).thenReturn(false);

        var result = mockClient.deleteApiBankCardById(TOKEN, CARD_ID);

        assertTrue(result.isEmpty());
        verify(clientApi).delete(eq(DELETE_BY_ID_URL), anyMap());
    }

    @Test
    @DisplayName("clearMockResponse() - Should return true when delete is successful")
    void clearMockResponse_ShouldReturnTrue() {
        when(clientApi.delete(anyString(), anyMap())).thenReturn(true);

        var result = mockClient.clearMockResponse(TOKEN);

        assertTrue(result);
        verify(clientApi).delete(eq(URL_BANK_CARD_MOCK), anyMap());
    }

    @Test
    @DisplayName("clearMockResponse() - Should return false when server fails to delete")
    void clearMockResponse_ShouldReturnFalse_WhenDeletionFails() {
        when(clientApi.delete(anyString(), anyMap())).thenReturn(false);

        var result = mockClient.clearMockResponse(TOKEN);

        assertFalse(result);
        verify(clientApi).delete(eq(URL_BANK_CARD_MOCK), anyMap());
    }

    /**
     * Helper method to convert objects to JsonNode for mocking clientApi responses.
     */
    private JsonNode toJson(Object obj) {
        return objectMapper.valueToTree(obj);
    }
}
