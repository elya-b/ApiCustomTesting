package emulator.integration;

import com.fasterxml.jackson.databind.*;
import elya.card.constants.Currency;
import elya.card.constants.*;
import elya.constants.*;
import elya.dto.auth.*;
import elya.dto.bankcard.*;
import elya.emulator.*;
import elya.emulator.repository.*;
import elya.emulator.services.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.context.*;
import org.springframework.http.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;
import org.springframework.test.web.servlet.result.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import static elya.constants.enums.HttpHeaderValues.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.*;

/**
 * Integration tests for {@link ApiEmulator} (full Spring context).
 * <ul>
 *   <li>{@code setMockResponse()} — new cards are appended to the existing list (APPEND logic)</li>
 *   <li>{@code setMockResponse()} — cardId is incremented based on the current maximum</li>
 *   <li>{@code deleteById()} — subsequent GET returns 404 after card deletion</li>
 *   <li>{@code clear()} — removes all mocks for the specific user</li>
 *   <li>{@code clear()} — does not affect other users' data (isolation)</li>
 *   <li>{@code setMockResponse()} — returns 400 and does not save data for an invalid card number</li>
 *   <li>{@code intercept()} — returns 401 for an invalid token on every protected endpoint (parameterized)</li>
 *   <li>{@code mapping()} — correctly maps Currency and CardType enum fields from JSON</li>
 *   <li>{@code concurrency()} — concurrent appends to one session lose no card and never repeat a cardId (repeated)</li>
 *   <li>{@code getById()} — returns 404 when a card ID does not exist</li>
 *   <li>{@code setMockResponse()} — empty list request does not overwrite existing data</li>
 *   <li>{@code setMockResponse()} — handles a large payload (100+ cards in one request)</li>
 *   <li>{@code intercept()} — returns 401 when session is invalidated (SessionRepository)</li>
 *   <li>{@code clearMockResponse()} — returns 401 and keeps the mocks of an expired session (regression)</li>
 * </ul>
 */
@SpringBootTest(classes = ApiEmulator.class) // Starts the full Spring application context for the test
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
    private static final String INVALID_TOKEN = "invalid_token";
    private static final int CONCURRENT_WRITERS = 8;
    private static final AuthRequest LOGIN_REQUEST = new AuthRequest("admin_user", "admin_password");
    private static final AuthRequest LOGIN_REQUEST_TEST = new AuthRequest("test_user", "test_password");

    @AfterEach
    void tearDown() {
        sessionRepository.clear();
        mockRepository.clearAll();
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

        mockMvc.perform(MockMvcRequestBuilders.delete(ApiEndpoints.URL_BANK_CARD_DATA)
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

        mockMvc.perform(MockMvcRequestBuilders.delete(ApiEndpoints.URL_BANK_CARD_DATA).header(AUTHORIZATION, BEARER + tokenA));

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

    /**
     * Every endpoint behind {@code SessionTokenInterceptor}: one row per HTTP method and path.
     * <p>A new protected endpoint must get a row here, so the test keeps proving the interceptor covers it.</p>
     */
    static Stream<Arguments> protectedEndpoints() {
        String byId = ApiEndpoints.URL_BANK_CARD_DATA + "/" + CARD_ID;
        return Stream.of(
                Arguments.of(HttpMethod.GET, ApiEndpoints.URL_BANK_CARD_DATA),
                Arguments.of(HttpMethod.GET, byId),
                Arguments.of(HttpMethod.POST, ApiEndpoints.URL_BANK_CARD_DATA),
                Arguments.of(HttpMethod.DELETE, ApiEndpoints.URL_BANK_CARD_DATA),
                Arguments.of(HttpMethod.DELETE, byId)
        );
    }

    @ParameterizedTest(name = "[{index}] {0} {1}")
    @MethodSource("protectedEndpoints")
    @DisplayName("intercept() - Should return 401 for an invalid token on every protected endpoint")
    void intercept_ShouldReturn401_WhenTokenIsInvalid(HttpMethod method, String path) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.request(method, path)
                        .header(AUTHORIZATION, BEARER + INVALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(CARD_NUMBER, Currency.EUR))))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
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

    @RepeatedTest(value = 10, name = "{displayName} [{currentRepetition}/{totalRepetitions}]")
    @DisplayName("concurrency() - Concurrent appends to one session lose no card and never repeat a cardId")
    void concurrency_ShouldKeepEveryCardWithUniqueId_WhenOneSessionIsUpdatedInParallel() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_WRITERS);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<BankCardListResponse>> writers = IntStream.range(0, CONCURRENT_WRITERS)
                .mapToObj(i -> (Callable<BankCardListResponse>) () -> {
                    ready.countDown();
                    start.await();
                    return mockService.setMockResponse(token, createRequest(CARD_NUMBER + i, Currency.EUR));
                })
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_WRITERS);
        try {
            List<Future<BankCardListResponse>> results = writers.stream().map(executor::submit).toList();
            assertTrue(ready.await(5, TimeUnit.SECONDS), "All writers must be ready before the start");
            start.countDown();
            for (Future<BankCardListResponse> result : results) {
                result.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        List<Long> cardIds = mockService.getApiBankCards(token).getResponse().getCards().stream()
                .map(BankCardResponse::getCardId)
                .toList();

        assertAll("Every concurrent append must survive with its own cardId",
                () -> assertThat(cardIds)
                        .as("No append may be lost")
                        .hasSize(CONCURRENT_WRITERS),
                () -> assertThat(cardIds)
                        .as("cardIds must be unique and run from 1 to %d", CONCURRENT_WRITERS)
                        .containsExactlyInAnyOrderElementsOf(
                                LongStream.rangeClosed(1, CONCURRENT_WRITERS).boxed().toList())
        );
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

    @Test
    @DisplayName("clearMockResponse() - Should return 401 and keep the mocks when the session is no longer valid")
    void clear_ShouldReturn401AndKeepMocks_WhenSessionIsExpired() throws Exception {
        String token = loginAndGetToken(LOGIN_REQUEST);
        setMockResponse(token, createRequest(CARD_NUMBER, Currency.EUR), true);

        sessionRepository.clear();

        mockMvc.perform(MockMvcRequestBuilders.delete(ApiEndpoints.URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, BEARER + token))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        assertThat(mockRepository.find(token))
                .as("Mocks of an expired session must not be deleted")
                .isPresent();
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
        mockMvc.perform(MockMvcRequestBuilders.post(ApiEndpoints.URL_BANK_CARD_DATA)
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