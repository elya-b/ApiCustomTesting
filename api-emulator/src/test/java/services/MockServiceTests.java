package services;

import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardRequest;
import elya.dto.bankcard.BankCardResponse;
import elya.emulator.constants.excpetions.TokenValidationException;
import elya.repository.MockRepository;
import elya.services.MockService;
import elya.services.TokenManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static elya.emulator.constants.excpetions.ExceptionMessage.AUTH_TOKEN_VALIDATION_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (Mockito) for {@link elya.services.MockService}.
 * <ul>
 *   <li>{@code getApiBankCards()} — returns a card list for a valid token</li>
 *   <li>{@code getApiBankCards()} — returns an empty response when no mock is found</li>
 *   <li>{@code getApiBankCards()} — propagates TokenValidationException for an invalid token</li>
 *   <li>{@code getApiBankCardById()} — card found by ID</li>
 *   <li>{@code getApiBankCardById()} — returns an empty Optional when the card is not found</li>
 *   <li>{@code clearMockResponse()} — successfully clears the mock</li>
 *   <li>{@code clearMockResponse()} — returns false when the repository fails to clear</li>
 *   <li>{@code deleteApiBankCardById()} — deletes the card and returns its ID</li>
 *   <li>{@code deleteApiBankCardById()} — returns empty Optional when the ID is not found</li>
 *   <li>{@code setMockResponse()} — appends cards with incremented IDs</li>
 *   <li>{@code setMockResponse()} — first cards are assigned IDs starting from 1</li>
 *   <li>{@code setMockResponse()} — null request saves an empty response</li>
 *   <li>{@code setMockResponse()} — new ID equals Max ID + 1 even when there are gaps</li>
 *   <li>{@code setMockResponse()} — empty card list saves an empty response</li>
 *   <li>{@code getApiBankCardById()} — propagates TokenValidationException</li>
 *   <li>{@code deleteApiBankCardById()} — propagates TokenValidationException</li>
 *   <li>{@code setMockResponse()} — does not call tokenManagerService directly (validation is delegated)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class MockServiceTests {
    @Mock
    private MockRepository mockRepository;
    @Mock
    private TokenManagerService tokenManagerService;

    @InjectMocks
    private MockService mockService;

    private static final Long CARD_ID = 1L;
    private static final Long CARD_ID_2 = 2L;
    private static final String TOKEN = "token";
    private static final BankCardResponse CARD_RESPONSE = BankCardResponse.builder().cardId(CARD_ID).build();
    private static final BankCardListResponse CARD_LIST_RESPONSE = BankCardListResponse.of(List.of(CARD_RESPONSE));
    private static final BankCardRequest CARD_REQUEST = BankCardRequest.builder().build();
    private static final BankCardListRequest DEFAULT_LIST_REQUEST = BankCardListRequest.of(List.of(CARD_REQUEST));

    // --- GET API BANK CARDS TESTS ---

    @Test
    @DisplayName("Successful. Valid token")
    void getApiBankCards_Success() {
        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(CARD_LIST_RESPONSE));

        BankCardListResponse result = mockService.getApiBankCards(TOKEN);
        List<BankCardResponse> finalResult = result.getResponse().getCards();

        assertFalse(finalResult.isEmpty(), "Result list should not be empty");
        assertEquals(1, finalResult.size());
        assertEquals(CARD_ID, finalResult.getFirst().getCardId());

        verify(tokenManagerService, times(1)).validateAuthToken(TOKEN);
    }

    @Test
    @DisplayName("Successful. Return empty response")
    void getApiBankCards_EmptyResponse() {
        doNothing().when(tokenManagerService).validateAuthToken(TOKEN);

        when(mockRepository.find(TOKEN)).thenReturn(Optional.empty());

        BankCardListResponse result = mockService.getApiBankCards(TOKEN);
        List<BankCardResponse> finalResult = result.getResponse().getCards();

        assertNotNull(finalResult, "Result should never be null");
        assertTrue(finalResult.isEmpty(), "Result list should be empty");

        verify(tokenManagerService).validateAuthToken(TOKEN);
        verify(mockRepository).find(TOKEN);
    }

    @Test
    @DisplayName("Failed. Invalid token")
    void getApiBankCards_InvalidToken() {
        doThrow(new TokenValidationException(AUTH_TOKEN_VALIDATION_FAILED))
                .when(tokenManagerService).validateAuthToken(TOKEN);

        assertThrows(TokenValidationException.class, () ->
                mockService.getApiBankCards(TOKEN)
        );

        verify(tokenManagerService).validateAuthToken(TOKEN);
        verify(mockRepository, never()).find(TOKEN);
    }


    // --- GET API BANK CARDS BY ID TESTS ---

    @Test
    @DisplayName("Successful. Card found by ID")
    void getApiBankCardById_Success() {
        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(CARD_LIST_RESPONSE));

        Optional<BankCardResponse> result = mockService.getApiBankCardById(TOKEN, CARD_ID);

        assertTrue(result.isPresent(), "Result should not be null");
        assertEquals(CARD_ID, result.get().getCardId());
        verify(tokenManagerService).validateAuthToken(TOKEN);
    }

    @Test
    @DisplayName("Failed. Card was NOT found by ID")
    void getApiBankCardById_Failed() {
        when(mockRepository.find(TOKEN)).thenReturn(Optional.empty());

        Optional<BankCardResponse> result = mockService.getApiBankCardById(TOKEN, CARD_ID);

        assertTrue(result.isEmpty(), "Result should be empty Optional");
        verify(tokenManagerService).validateAuthToken(TOKEN);
    }

    // --- CLEAR MOCK RESPONSE TESTS ---

    @Test
    @DisplayName("Successful. Response is cleared")
    void clearMockResponse_Success() {
        when(mockRepository.clear(TOKEN)).thenReturn(true);

        boolean result = mockService.clearMockResponse(TOKEN);

        assertTrue(result, "Mock response was cleared");
        verify(mockRepository).clear(TOKEN);
    }

    @Test
    @DisplayName("Failed. Repository failed to clear data")
    void clearMockResponse_Failed() {
        when(mockRepository.clear(TOKEN)).thenReturn(false);

        boolean result = mockService.clearMockResponse(TOKEN);

        assertFalse(result, "Service should return false if repository failed to clear");
        verify(mockRepository).clear(TOKEN);
    }

    // --- DELETE BANK CARD BY ID TESTS ---

    @Test
    @DisplayName("Successful. Card deleted and returns ID")
    void deleteApiBankCardById_Success() {
        BankCardResponse card1 = BankCardResponse.builder().cardId(CARD_ID).build();
        BankCardResponse card2 = BankCardResponse.builder().cardId(CARD_ID_2).build();
        BankCardListResponse existingMock = BankCardListResponse.of(List.of(card1, card2));

        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(existingMock));
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        Optional<Long> result = mockService.deleteApiBankCardById(TOKEN, CARD_ID);

        assertAll("Check delete result",
                () -> assertTrue(result.isPresent(), "Result should not be empty"),
                () -> assertEquals(CARD_ID, result.get(), "Should return the ID of deleted card")
        );

        verify(mockRepository).save(eq(TOKEN), argThat(savedResponse ->
                savedResponse.getResponse().getCards().size() == 1 &&
                        savedResponse.getResponse().getCards().getFirst().getCardId().equals(CARD_ID_2)
        ));
    }

    @Test
    @DisplayName("Failed. Card was not deleted. ID is not found")
    void deleteApiBankCardById_Failed() {
        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(CARD_LIST_RESPONSE));

        Optional<Long> result = mockService.deleteApiBankCardById(TOKEN, CARD_ID_2);

        assertTrue(result.isEmpty(), "Result should be empty because ID was not found");
        verify(mockRepository, never()).save(anyString(), any());
    }

    // --- SET MOCK RESPONSE TESTS ---

    @Test
    @DisplayName("Successful. Set mock response with ID increment")
    void setMockResponse_IncrementId() {
        BankCardListRequest request = BankCardListRequest.of(List.of(CARD_REQUEST, CARD_REQUEST));

        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(CARD_LIST_RESPONSE));
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        BankCardListResponse result = mockService.setMockResponse(TOKEN, request);

        assertNotNull(result);

        verify(mockRepository).save(eq(TOKEN), argThat(savedResponse -> {
            List<BankCardResponse> cards = savedResponse.getResponse().getCards();
            return cards.size() == 3 &&
                    cards.get(0).getCardId().equals(CARD_ID) &&
                    cards.get(1).getCardId().equals(CARD_ID_2) &&
                    cards.get(2).getCardId() == 3L;
        }));
    }

    @Test
    @DisplayName("Successful. First cards get ID starting from 1")
    void setMockResponse_FirstCards() {
        when(mockRepository.find(TOKEN)).thenReturn(Optional.empty());
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        mockService.setMockResponse(TOKEN, DEFAULT_LIST_REQUEST);

        verify(mockRepository).save(eq(TOKEN), argThat(savedResponse ->
                savedResponse.getResponse().getCards().getFirst().getCardId()
                        .equals(CARD_ID)));
    }

    @Test
    @DisplayName("Successful. Should save empty response when request is null")
    void setMockResponse_ShouldHandleNullRequest() {
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        BankCardListResponse result = mockService.setMockResponse(TOKEN, null);

        assertNotNull(result);
        assertTrue(result.getResponse().getCards().isEmpty());

        verify(mockRepository).save(eq(TOKEN), argThat(res ->
                res.getResponse().getCards().isEmpty()
        ));
    }

    @Test
    @DisplayName("Successful. New ID should be Max ID + 1 even if there are gaps")
    void setMockResponse_ShouldHandleGapsInIds() {
        BankCardResponse card1 = BankCardResponse.builder().cardId(CARD_ID).build();
        BankCardResponse card5 = BankCardResponse.builder().cardId(5L).build();
        BankCardListResponse existingMock = BankCardListResponse.of(List.of(card1, card5));

        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(existingMock));
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        mockService.setMockResponse(TOKEN, DEFAULT_LIST_REQUEST);

        verify(mockRepository).save(eq(TOKEN), argThat(res ->
                res.getResponse().getCards().get(2).getCardId() == 6L
        ));
    }

    @Test
    @DisplayName("Successful. Should save empty response when cards list in request is null")
    void setMockResponse_ShouldHandleNullCardsList() {
        BankCardListRequest request = BankCardListRequest.of(Collections.emptyList());
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        BankCardListResponse result = mockService.setMockResponse(TOKEN, request);

        assertNotNull(result);
        assertThat(result.getResponse().getCards()).isEmpty();
        verify(mockRepository).save(
                eq(TOKEN), argThat(res -> res.getResponse().getCards().isEmpty())
        );
    }

    // --- TOKEN VALIDATION PROPAGATION ---

    @Test
    @DisplayName("Failed. getApiBankCardById() propagates TokenValidationException from getApiBankCards()")
    void getApiBankCardById_PropagatesTokenValidationException() {
        doThrow(new TokenValidationException(AUTH_TOKEN_VALIDATION_FAILED))
                .when(tokenManagerService).validateAuthToken(TOKEN);

        assertThrows(TokenValidationException.class, () ->
                mockService.getApiBankCardById(TOKEN, CARD_ID)
        );

        verify(tokenManagerService).validateAuthToken(TOKEN);
        verify(mockRepository, never()).find(anyString());
    }

    @Test
    @DisplayName("Failed. deleteApiBankCardById() propagates TokenValidationException from getApiBankCards()")
    void deleteApiBankCardById_PropagatesTokenValidationException() {
        doThrow(new TokenValidationException(AUTH_TOKEN_VALIDATION_FAILED))
                .when(tokenManagerService).validateAuthToken(TOKEN);

        assertThrows(TokenValidationException.class, () ->
                mockService.deleteApiBankCardById(TOKEN, CARD_ID)
        );

        verify(tokenManagerService).validateAuthToken(TOKEN);
        verify(mockRepository, never()).find(anyString());
    }

    @Test
    @DisplayName("Successful. setMockResponse() does NOT call tokenManagerService directly")
    void setMockResponse_DoesNotCallTokenValidation() {
        when(mockRepository.find(TOKEN)).thenReturn(Optional.empty());
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        mockService.setMockResponse(TOKEN, DEFAULT_LIST_REQUEST);

        verify(tokenManagerService, times(1)).validateAuthToken(TOKEN);
    }
}