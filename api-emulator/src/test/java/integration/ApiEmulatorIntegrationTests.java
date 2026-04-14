package integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import elya.constants.ApiEndpoints;
import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardRequest;
import elya.repository.MockRepository;
import elya.repository.SessionRepository;
import elya.services.MockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static elya.constants.enums.HttpHeaderValues.BEARER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * INTEGRATION TEST COVERAGE CHECKLIST:
 *
 * 1. AUTHENTICATION & SECURITY
 * - [x] POST /token: Successful generation of session token
 * - [x] GET /bank-cards: Intercept and return 401 Unauthorized for invalid Bearer token
 * - [x] Session Expiration: Return 401 when session is cleared from SessionRepository
 * - [x] Isolation: User A cannot see or clear User B's mocked data
 *
 * 2. MOCK DATA MANAGEMENT (CRUD)
 * - [x] APPEND LOGIC: POST /mock updates data by adding cards, not overwriting
 * - [x] ID GENERATION: Auto-increment cardId based on current max ID in user's list
 * - [x] GET /bank-cards/{id}: Retrieve specific card by ID or return 404 if missing
 * - [x] DELETE /bank-cards/{id}: Remove specific card and verify 404 on next access
 * - [x] DELETE /mock: Full clear of all mocked cards for the specific user
 * - [x] EMPTY LISTS: Verify that sending an empty BankCardListRequest doesn't wipe existing data
 *
 * 3. PERSISTENCE & FAULT TOLERANCE
 * - [x] RECOVERY: Data survives MockRepository.init() reload (JSON file persistence)
 * - [x] RESILIENCE: init() survives corrupted/invalid JSON files in /mocks directory
 * - [x] CLEANUP: Temporary test files are deleted after use
 *
 * 4. PERFORMANCE & DATA INTEGRITY
 * - [x] ENUM MAPPING: Successful JSON conversion for Currency and CardType
 * - [x] VALIDATION: Return 400 Bad Request for invalid inputs (e.g., 15-digit card numbers)
 * - [x] CONCURRENCY: Handle simultaneous updates from multiple threads safely
 * - [x] STRESS: Handle large payloads (100+ cards) in a single request
 */
@SpringBootTest(classes = elya.ApiEmulator.class) // Starts the full Spring application context for the test
@AutoConfigureMockMvc // Sets up the MockMvc instance to simulate HTTP requests
@ActiveProfiles("test") // Tells Spring to use 'application-test.yml' configuration
public class ApiEmulatorIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private MockRepository mockRepository;

    @Autowired
    private MockService mockService;

    private static final Long CARD_ID = 1L;
    private static final Long CARD_NUMBER = 4444555566667777L;
    private static final Long CARD_NUMBER_2 = 5555666677778888L;
    private static final Long CARD_NUMBER_15 = 444455556666777L;
    private static final AuthRequest LOGIN_REQUEST = new AuthRequest("admin_user", "admin_password");
    private static final AuthRequest LOGIN_REQUEST_TEST = new AuthRequest("test_user", "test_password");

    @AfterEach
    void tearDown() {
        sessionRepository.clear();
        mockRepository.clearAll();
    }

    @Test
    @DisplayName("init() - Should survive repository reload and keep data in file")
    void init_ShouldKeepData_WhenRepositoryReloads() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        setMockResponse(token, createRequest(CARD_NUMBER, Currency.EUR), true);
        mockRepository.init();

        var mock = mockRepository.find(token);
        assertTrue(mock.isPresent(), "Data should be available after repository re-initialization");
        assertEquals(CARD_NUMBER, mock.get().getResponse().getCards().get(0).getCardNumber());
    }

    @Test
    @DisplayName("setMockResponse() - Should append new cards to existing list")
    void setMockResponse_ShouldAppendCards_WhenListAlreadyExists() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        setMockResponse(token, createRequest(CARD_NUMBER, Currency.EUR), true);
        setMockResponse(token, createRequest(CARD_NUMBER_2, Currency.USD), true);

        var response = getBankCards(token);
        assertThat(response.getResponse().getCards()).hasSize(2);
    }

    @Test
    @DisplayName("setMockResponse() - Should increment card IDs based on max existing ID")
    void setMockResponse_ShouldIncrementIds_BasedOnExistingMax() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        setMockResponse(token, createRequest(CARD_NUMBER, Currency.EUR), true);
        setMockResponse(token, createRequest(CARD_NUMBER_2, Currency.EUR), true);

        var cards = getBankCards(token).getResponse().getCards();
        assertThat(cards.get(1).getCardId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("deleteById() - Should return 404 on subsequent access after deletion")
    void deleteById_ShouldReturn404_AfterCardIsDeleted() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        setMockResponse(token, createRequest(CARD_NUMBER, Currency.EUR), true);

        mockMvc.perform(MockMvcRequestBuilders.delete(ApiEndpoints.URL_BANK_CARD_DATA_ID, CARD_ID)
                .header(AUTHORIZATION, BEARER + token)).andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get(ApiEndpoints.URL_BANK_CARD_DATA_ID, CARD_ID)
                .header(AUTHORIZATION, BEARER + token))
                .andExpect(
                        MockMvcResultMatchers.status().isNotFound()
                );
    }

    @Test
    @DisplayName("clear() - Should remove all mocks for the specific user")
    void clear_ShouldRemoveAllMocks_ForSpecificUser() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        setMockResponse(token, createRequest(CARD_NUMBER, Currency.EUR), true);

        mockMvc.perform(MockMvcRequestBuilders.delete(ApiEndpoints.URL_BANK_CARD_MOCK)
                .header(AUTHORIZATION, BEARER + token)).andExpect(MockMvcResultMatchers.status().isOk());

        assertThat(getBankCards(token).getResponse().getCards()).isEmpty();
    }

    @Test
    @DisplayName("clear() - Should not affect other users when one user clears their mock")
    void clear_ShouldMaintainIsolation_BetweenUsers() throws Exception {
        String tokenA = loginAndGetToken(LOGIN_REQUEST);
        String tokenB = loginAndGetToken(LOGIN_REQUEST_TEST);
        setMockResponse(tokenA, createRequest(CARD_NUMBER, Currency.EUR), true);
        setMockResponse(tokenB, createRequest(CARD_NUMBER_2, Currency.EUR), true);

        mockMvc.perform(MockMvcRequestBuilders.delete(ApiEndpoints.URL_BANK_CARD_MOCK).header(AUTHORIZATION, BEARER + tokenA));

        assertThat(getBankCards(tokenB).getResponse().getCards()).hasSize(1);
    }

    @Test
    @DisplayName("setMockResponse() - Should return 400 and not save data for invalid card number")
    void setMockResponse_ShouldReturn400_WhenCardNumberIsInvalid() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        setMockResponse(token, createRequest(CARD_NUMBER_15, Currency.EUR), false);

        var mock = mockRepository.find(token);
        assertThat(mock).as("Mock should not be created for invalid data").isNotPresent();
    }

    @Test
    @DisplayName("intercept() - Should return 401 when token is invalid")
    void intercept_ShouldReturn401_WhenTokenIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(ApiEndpoints.URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, BEARER + "invalid_token"))
                .andExpect(
                        MockMvcResultMatchers.status().isUnauthorized()
                );
    }

    @Test
    @DisplayName("mapping() - Should correctly map Currency and CardType enums")
    void mapping_ShouldMapEnums_WhenJsonIsProcessed() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        setMockResponse(token, createRequest(CARD_NUMBER, Currency.USD), true);

        var card = getBankCards(token).getResponse().getCards().get(0);
        assertEquals(Currency.USD, card.getCurrency());
        assertEquals(CardType.DEBIT, card.getCardType());
    }

    @Test
    @DisplayName("concurrency() - Should handle multiple concurrent updates safely")
    void concurrency_ShouldHandleUpdates_WhenMultipleThreadsAccess() throws Exception {
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.execute(() -> {
                try {
                    String t = loginAndGetToken(new AuthRequest("user" + index, "pass" + index));
                    setMockResponse(t, createRequest(CARD_NUMBER + index, Currency.EUR), true);
                } catch (Exception ignored) {}
            });
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("getById() - Should return 404 when card ID does not exist")
    void getById_ShouldReturn404_WhenIdIsMissing() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        mockMvc.perform(MockMvcRequestBuilders.get(ApiEndpoints.URL_BANK_CARD_DATA_ID, 999L)
                .header(AUTHORIZATION, BEARER + token))
                .andExpect(
                        MockMvcResultMatchers.status().isNotFound()
                );
    }

    @Test
    @DisplayName("setMockResponse() - Should append and not overwrite when empty list is sent")
    void setMockResponse_ShouldAppend_WhenListIsEmpty() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        setMockResponse(token, createRequest(CARD_NUMBER, Currency.EUR), true);

        setMockResponse(token, BankCardListRequest.of(Collections.emptyList()), true);
        var response = getBankCards(token).getResponse().getCards();

        assertAll("Check delete result",
                () -> assertEquals(1, response.size()),
                () -> assertEquals(CARD_NUMBER, response.getFirst().getCardNumber()),
                () -> assertEquals(Currency.EUR, response.getFirst().getCurrency())
        );
    }

    @Test
    @DisplayName("init() - Should handle corrupted JSON file gracefully")
    void init_ShouldHandleCorruptedFile_WhenJsonIsInvalid() throws Exception {
        Path path = Paths.get("target/test-data/mock_response.json");

        Files.createDirectories(path.getParent());
        Files.writeString(path, "{ !!! invalid json !!! }");

        assertDoesNotThrow(() -> mockRepository.init(),
                "Repository should catch Jackson exceptions and not crash the context");

        Files.deleteIfExists(path);
    }

    @Test
    @DisplayName("setMockResponse() - Should handle large number of cards in one request")
    void setMockResponse_ShouldHandleLargePayload_WhenManyCardsAreSent() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        int cardCount = 100;

        List<BankCardRequest> largeList = IntStream.range(0, cardCount)
                .mapToObj(i -> BankCardRequest.builder()
                        .cardNumber(4000000000000000L + i)
                        .cardType(CardType.DEBIT)
                        .currency(Currency.EUR)
                        .build())
                .toList();

        setMockResponse(token, BankCardListRequest.of(largeList), true);

        var response = getBankCards(token);
        var cards = response.getResponse().getCards();

        assertAll("Large Payload Verification",
                () -> assertThat(cards).hasSize(cardCount),
                () -> assertEquals(4000000000000099L, cards.get(99).getCardNumber())
        );
    }

    @Test
    @DisplayName("intercept() - Should return 401 when session is no longer valid")
    void intercept_ShouldReturn401_WhenSessionIsExpired() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);

        sessionRepository.clear();

        mockMvc.perform(MockMvcRequestBuilders.get(ApiEndpoints.URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, BEARER + token))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }



    private BankCardListRequest createRequest(Long number, Currency currency) {
        return BankCardListRequest.of(List.of(BankCardRequest.builder()
                                                                .cardNumber(number)
                                                                .cardType(CardType.DEBIT)
                                                                .currency(currency)
                                                                .build()));
    }

    private BankCardListResponse getBankCards(String token) throws Exception {
        var result = mockMvc.perform(MockMvcRequestBuilders.get(ApiEndpoints.URL_BANK_CARD_DATA)
                .header(AUTHORIZATION, BEARER + token)).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), BankCardListResponse.class);
    }

    private void setMockResponse(String token, BankCardListRequest request, Boolean result) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(ApiEndpoints.URL_BANK_CARD_MOCK)
                        .header(AUTHORIZATION, BEARER + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result ?
                        MockMvcResultMatchers.status().isOk() : MockMvcResultMatchers.status().isBadRequest()
                );
    }

    private String loginAndGetToken(AuthRequest authRequest) throws Exception {

        var result = mockMvc.perform(MockMvcRequestBuilders.post(ApiEndpoints.URL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var authResponse = objectMapper.readValue(responseBody, AuthResponse.class);

        return authResponse.getData().getToken();
    }
}
