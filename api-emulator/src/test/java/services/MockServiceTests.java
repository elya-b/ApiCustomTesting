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
        // Stubbing: when find() is called with our token, return the mock data
        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(CARD_LIST_RESPONSE));

        // Act
        BankCardListResponse result = mockService.getApiBankCards(TOKEN);
        List<BankCardResponse> finalResult = result.getResponse().getCards();

        // Assert
        assertFalse(finalResult.isEmpty(), "Result list should not be empty");
        assertEquals(1, finalResult.size());
        assertEquals(CARD_ID, finalResult.getFirst().getCardId());

        // Verify that security check was performed
        verify(tokenManagerService, times(1)).validateAuthToken(TOKEN);
    }

    @Test
    @DisplayName("Successful. Return empty response")
    void getApiBankCards_EmptyResponse() {
        // Stubbing
        doNothing().when(tokenManagerService).validateAuthToken(TOKEN);

        // Simulate that repository returns NOTHING (empty Optional)
        when(mockRepository.find(TOKEN)).thenReturn(Optional.empty());

        // Act
        BankCardListResponse result = mockService.getApiBankCards(TOKEN);
        List<BankCardResponse> finalResult = result.getResponse().getCards();

        // Assert
        assertNotNull(finalResult, "Result should never be null");
        assertTrue(finalResult.isEmpty(), "Result list should be empty");

        // Verify
        verify(tokenManagerService).validateAuthToken(TOKEN);
        verify(mockRepository).find(TOKEN);
    }

    @Test
    @DisplayName("Failed. Invalid token")
    void getApiBankCards_InvalidToken() {
        // Stubbing: when find() is called with our token, return the mock data
        doThrow(new TokenValidationException(AUTH_TOKEN_VALIDATION_FAILED))
                .when(tokenManagerService).validateAuthToken(TOKEN);

        // Act & Assert: Check that the exception is thrown when calling the service
        assertThrows(TokenValidationException.class, () ->
                mockService.getApiBankCards(TOKEN)
        );

        // Verify that security check was performed
        verify(tokenManagerService).validateAuthToken(TOKEN);
        verify(mockRepository, never()).find(TOKEN);
    }


    // --- GET API BANK CARDS BY ID TESTS ---

    @Test
    @DisplayName("Successful. Card found by ID")
    void getApiBankCardById_Success() {
        // Stubbing
        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(CARD_LIST_RESPONSE));

        // Act
        Optional<BankCardResponse> result = mockService.getApiBankCardById(TOKEN, CARD_ID);

        // Assert
        assertTrue(result.isPresent(), "Result should not be null");
        assertEquals(CARD_ID, result.get().getCardId());

        // Verify that security check was performed
        verify(tokenManagerService).validateAuthToken(TOKEN);
    }

    @Test
    @DisplayName("Failed. Card was NOT found by ID")
    void getApiBankCardById_Failed() {
        // Stubbing
        when(mockRepository.find(TOKEN)).thenReturn(Optional.empty());

        // Act
        Optional<BankCardResponse> result = mockService.getApiBankCardById(TOKEN, CARD_ID);

        // Assert
        assertTrue(result.isEmpty(), "Result should be empty Optional");

        // Verify that security check was performed
        verify(tokenManagerService).validateAuthToken(TOKEN);
    }

    // --- CLEAR MOCK RESPONSE TESTS ---

    @Test
    @DisplayName("Successful. Response is cleared")
    void clearMockResponse_Success() {
        // Stubbing
        when(mockRepository.clear(TOKEN)).thenReturn(true);

        // Act
        boolean result = mockService.clearMockResponse(TOKEN);

        // Assert
        assertTrue(result, "Mock response was cleared");

        // Verify
        verify(mockRepository).clear(TOKEN);
    }

    @Test
    @DisplayName("Failed. Repository failed to clear data")
    void clearMockResponse_Failed() {
        // Stubbing: when find() is called with our token, return the mock data
        when(mockRepository.clear(TOKEN)).thenReturn(false);

        // Act
        boolean result = mockService.clearMockResponse(TOKEN);

        // Assert
        assertFalse(result, "Service should return false if repository failed to clear");

        // Verify that security check was performed once
        verify(mockRepository).clear(TOKEN);
    }

    // --- DELETE BANK CARD BY ID TESTS ---

    @Test
    @DisplayName("Successful. Card deleted and returns ID")
    void deleteApiBankCardById_Success() {
        BankCardResponse card1 = BankCardResponse.builder().cardId(CARD_ID).build();
        BankCardResponse card2 = BankCardResponse.builder().cardId(CARD_ID_2).build();
        BankCardListResponse existingMock = BankCardListResponse.of(List.of(card1, card2));

        // Stubbing
        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(existingMock));
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        // Act
        Optional<Long> result = mockService.deleteApiBankCardById(TOKEN, CARD_ID);

        // Assert
        assertAll("Check delete result",
                () -> assertTrue(result.isPresent(), "Result should not be empty"),
                () -> assertEquals(CARD_ID, result.get(), "Should return the ID of deleted card")
        );

        // Verify
        verify(mockRepository).save(eq(TOKEN), argThat(savedResponse ->
                savedResponse.getResponse().getCards().size() == 1 &&
                        savedResponse.getResponse().getCards().getFirst().getCardId().equals(CARD_ID_2)
        ));
    }

    @Test
    @DisplayName("Failed. Card was not deleted. ID is not found")
    void deleteApiBankCardById_Failed() {
        // Stubbing
        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(CARD_LIST_RESPONSE));

        // Act
        Optional<Long> result = mockService.deleteApiBankCardById(TOKEN, CARD_ID_2);

        // Assert
        assertTrue(result.isEmpty(), "Result should be empty because ID was not found");

        // Verify
        verify(mockRepository, never()).save(anyString(), any());
    }

    // --- SET MOCK RESPONSE TESTS ---

    @Test
    @DisplayName("Successful. Set mock response with ID increment")
    void setMockResponse_IncrementId() {
        // Prepare request with two new cards
        BankCardListRequest request = BankCardListRequest.of(List.of(CARD_REQUEST, CARD_REQUEST));

        when(mockRepository.find(TOKEN)).thenReturn(Optional.of(CARD_LIST_RESPONSE));
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        // ACT
        BankCardListResponse result = mockService.setMockResponse(TOKEN, request);

        // ASSERT & VERIFY
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
        // Stubbing
        when(mockRepository.find(TOKEN)).thenReturn(Optional.empty());
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        // Act
        mockService.setMockResponse(TOKEN, DEFAULT_LIST_REQUEST);

        // Verify
        verify(mockRepository).save(eq(TOKEN), argThat(savedResponse ->
                savedResponse.getResponse().getCards().getFirst().getCardId()
                        .equals(CARD_ID)));
    }

    @Test
    @DisplayName("Successful. Should save empty response when request is null")
    void setMockResponse_ShouldHandleNullRequest() {
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        // ACT
        BankCardListResponse result = mockService.setMockResponse(TOKEN, null);

        // ASSERT
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

        // Verify that the new card gets ID 6 (5 + 1)
        verify(mockRepository).save(eq(TOKEN), argThat(res ->
                res.getResponse().getCards().get(2).getCardId() == 6L
        ));
    }

    @Test
    @DisplayName("Successful. Should save empty response when cards list in request is null")
    void setMockResponse_ShouldHandleNullCardsList() {
        // Request is not null, but cards list is null
        BankCardListRequest request = BankCardListRequest.of(Collections.emptyList());

        // Stubbing
        when(mockRepository.save(eq(TOKEN), any())).thenReturn(true);

        // Act
        BankCardListResponse result = mockService.setMockResponse(TOKEN, request);

        // Assert && Verify

        assertNotNull(result);
        assertThat(result.getResponse().getCards()).isEmpty();

        verify(mockRepository).save(
                eq(TOKEN), argThat(res -> res.getResponse().getCards().isEmpty())
        );
    }
}
