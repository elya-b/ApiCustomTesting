import com.fasterxml.jackson.databind.ObjectMapper;
import elya.ApiEmulator;
import elya.ApiEmulatorController;
import elya.card.constants.CardType;
import elya.card.constants.Currency;
import elya.dto.auth.AuthRequest;
import elya.dto.auth.AuthResponse;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardRequest;
import elya.dto.bankcard.BankCardResponse;
import elya.emulator.constants.excpetions.TokenValidationException;
import elya.services.AuthenticationService;
import elya.services.MockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static elya.constants.ApiEndpoints.*;
import static elya.emulator.constants.logs.ApiErrorLogs.INVALID_OR_MISSING_CREDENTIALS;
import static elya.emulator.constants.messages.ResponseMessages.MOCK_CLEARED;
import static elya.enums.responsemodel.AuthToken.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiEmulatorController.class)
@ContextConfiguration(classes = ApiEmulator.class)
public class ApiEmulatorControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authService;

    @MockitoBean
    private MockService mockService;

    private static final Long CARD_ID = 1L;
    private static final Long CARD_NUMBER = 4444333322221111L;
    private static final String FULL_TOKEN = "Bearer token";
    private static final String CLEAN_TOKEN = "token";
    private static final String FULL_URL_BANK_CARD_DATA = URL_BANK_CARD_DATA + "/" + CARD_ID;
    private static final AuthRequest AUTH_REQUEST = new AuthRequest("login", "password");
    private static final BankCardResponse DEFAULT_CARD_RESPONSE = BankCardResponse.builder()
                                                                                    .cardId(CARD_ID)
                                                                                    .cardNumber(CARD_NUMBER)
                                                                                    .cardType(CardType.DEBIT)
                                                                                    .currency(Currency.EUR)
                                                                                    .build();
    private static final BankCardListResponse DEFAULT_CARD_LIST_RESPONSE = BankCardListResponse.of(List.of(DEFAULT_CARD_RESPONSE));

    @Test
    @DisplayName("GET " + URL_BANK_CARD_DATA + " - Success")
    void getBankCards_ShouldReturnList() throws Exception {
        // Stubbing: expecting the cleaned token
        when(mockService.getApiBankCards(CLEAN_TOKEN)).thenReturn(DEFAULT_CARD_LIST_RESPONSE);

        // ACT & ASSERT
        mockMvc.perform(get(URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, FULL_TOKEN))
                .andExpect(status().isOk())
                // Verifying JSON structure and values
                .andExpect(jsonPath("$.response.size").value(1))
                .andExpect(jsonPath("$.response.cards[0].cardId").value(CARD_ID))
                .andExpect(jsonPath("$.response.cards[0].cardNumber").value(CARD_NUMBER));
    }


    @Test
    @DisplayName("GET " + URL_BANK_CARD_DATA + " - Success, but empty list")
    void getBankCards_ShouldReturnEmptyList() throws Exception {

        // Stubbing: service returns an empty list for the CORRECT token
        when(mockService.getApiBankCards(CLEAN_TOKEN)).thenReturn(BankCardListResponse.of(Collections.emptyList()));

        // ACT & ASSERT
        mockMvc.perform(get(URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, FULL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.size").value(0))
                .andExpect(jsonPath("$.response.cards").isEmpty());
    }

    @Test
    @DisplayName("GET " + URL_BANK_CARD_DATA + "/{cardId} - Success")
    void getBankCardById_ShouldReturn200() throws Exception {
        // We tell the mock to return an empty Optional
        when(mockService.getApiBankCardById(CLEAN_TOKEN, CARD_ID)).thenReturn(Optional.of(DEFAULT_CARD_RESPONSE));

        // ACT & ASSERT
        mockMvc.perform(get(FULL_URL_BANK_CARD_DATA) // Requesting specific ID
                        .header(AUTHORIZATION, FULL_TOKEN))
                .andExpect(status().isOk()) // Expecting 200
                .andExpect(jsonPath("$.cardId").value(CARD_ID))
                .andExpect(jsonPath("$.cardNumber").value(CARD_NUMBER));
    }

    @Test
    @DisplayName("GET " + URL_BANK_CARD_DATA + "/{cardId} - Not Found")
    void getBankCardById_ShouldReturn404() throws Exception {

        // Stubbing: service returns Optional.empty()
        when(mockService.getApiBankCardById(CLEAN_TOKEN, CARD_ID)).thenReturn(Optional.empty());

        // ACT & ASSERT
        mockMvc.perform(get(FULL_URL_BANK_CARD_DATA) // Requesting specific ID
                        .header(AUTHORIZATION, FULL_TOKEN))
                .andExpect(status().isNotFound()); // Expecting 404 instead of 200
    }

    @Test
    @DisplayName("DELETE " + URL_BANK_CARD_MOCK + " - Success")
    void clearMockResponse_ShouldReturn200() throws Exception {

        when(mockService.clearMockResponse(CLEAN_TOKEN)).thenReturn(true);

        mockMvc.perform(delete(URL_BANK_CARD_MOCK)
                        .header(AUTHORIZATION, FULL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(true))
                .andExpect(jsonPath("$.message").value(MOCK_CLEARED));
    }

    @Test
    @DisplayName("DELETE " + URL_BANK_CARD_DATA + "/{cardId} - Success")
    void deleteApiBankCardById_ShouldReturn200() throws Exception {

        when(mockService.deleteApiBankCardById(CLEAN_TOKEN, CARD_ID)).thenReturn(Optional.of(CARD_ID));

        // ACT & ASSERT
        mockMvc.perform(delete(FULL_URL_BANK_CARD_DATA) // Requesting specific ID
                        .header(AUTHORIZATION, FULL_TOKEN))
                .andExpect(status().isOk()) // Expecting 200
                .andExpect(jsonPath("$.cardId").value(CARD_ID));
    }

    @Test
    @DisplayName("DELETE " + URL_BANK_CARD_DATA + "/{cardId} - Not Found")
    void deleteApiBankCardById_ShouldReturn404() throws Exception {

        // Stubbing: service returns Optional.empty()
        when(mockService.deleteApiBankCardById(CLEAN_TOKEN, CARD_ID))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        mockMvc.perform(delete(FULL_URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, FULL_TOKEN))
                .andExpect(status().isNotFound()); // Expecting 404 instead of 200
    }

    @Test
    @DisplayName("POST " + URL_BANK_CARD_MOCK + " - Success")
    void setMockResponse_ShouldReturnList() throws Exception {

        BankCardRequest cardDto = BankCardRequest.builder()
                .cardNumber(CARD_NUMBER)
                .cardType(CardType.DEBIT)
                .currency(Currency.EUR)
                .build();
        BankCardListRequest requestWrapper = BankCardListRequest.of(List.of(cardDto));

        BankCardResponse expectedCardResponse = BankCardResponse.builder()
                .cardId(CARD_ID)
                .cardNumber(CARD_NUMBER)
                .cardType(CardType.DEBIT)
                .currency(Currency.EUR)
                .build();
        BankCardListResponse listResponse = BankCardListResponse.of(List.of(expectedCardResponse));

        when(mockService.setMockResponse(eq(CLEAN_TOKEN), any(BankCardListRequest.class)))
                .thenReturn(listResponse);

        when(mockService.getApiBankCards(CLEAN_TOKEN)).thenReturn(DEFAULT_CARD_LIST_RESPONSE);

        mockMvc.perform(post(URL_BANK_CARD_MOCK)
                        .header(AUTHORIZATION, FULL_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWrapper)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    var actual = objectMapper.readValue(result.getResponse().getContentAsString(), BankCardListResponse.class);
                    var container = actual.getResponse();

                    assertThat(container.getCards())
                            .as("Mock validation")
                            .hasSize(1)
                            .first()
                            .satisfies(card -> {
                                assertThat(card.getCardId()).isEqualTo(CARD_ID);
                                assertThat(card.getCardNumber()).isEqualTo(CARD_NUMBER);
                                assertThat(card.getCardType()).isEqualTo(CardType.DEBIT);
                                assertThat(card.getCurrency()).isEqualTo(Currency.EUR);
                            });

                    assertThat(container.getSize()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("POST " + URL_BANK_CARD_MOCK + " - Handle Save Failure")
    void setMockResponse_ShouldReturnList_EvenIfSaveFails() throws Exception {
        BankCardListRequest requestWrapper = BankCardListRequest.of(Collections.emptyList());

        when(mockService.setMockResponse(eq(CLEAN_TOKEN), any(BankCardListRequest.class)))
                .thenReturn(BankCardListResponse.of(Collections.emptyList()));

        when(mockService.getApiBankCards(CLEAN_TOKEN)).thenReturn(BankCardListResponse.of(Collections.emptyList()));

        mockMvc.perform(post(URL_BANK_CARD_MOCK)
                        .header(AUTHORIZATION, FULL_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWrapper)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.size").value(0));
    }

    @Test
    @DisplayName("POST " + URL_BANK_CARD_MOCK + " - Null Body")
    void setMockResponse_ShouldHandleNullBody() throws Exception {

        mockMvc.perform(post(URL_BANK_CARD_MOCK)
                        .header(AUTHORIZATION, FULL_TOKEN)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST " + URL_BANK_CARD_MOCK + " - Missing Authorization Header")
    void setMockResponse_ShouldReturn400_WhenHeaderIsMissing() throws Exception {
        BankCardListRequest requestWrapper = BankCardListRequest.of(Collections.emptyList());

        mockMvc.perform(post(URL_BANK_CARD_MOCK)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWrapper)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST " + URL_BANK_CARD_MOCK + " - Invalid Token Format")
    void setMockResponse_ShouldHandleInvalidToken() throws Exception {
        String badHeader = "InvalidPrefix token123";
        BankCardListRequest requestWrapper = BankCardListRequest.of(Collections.emptyList());

        mockMvc.perform(post(URL_BANK_CARD_MOCK)
                        .header(AUTHORIZATION, badHeader)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWrapper)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST " + URL_TOKEN + " - Success")
    void generateAuthToken_ShouldReturnToken() throws Exception {
        // Stubbing the AuthenticationService
        when(authService.generateAuthToken(any(AuthRequest.class)))
                .thenReturn(AuthResponse.success("jwt-token", "3600", null, "issuer"));

        // ACT & ASSERT
        mockMvc.perform(post(URL_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AUTH_REQUEST)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Data.token").value("jwt-token"))
                .andExpect(jsonPath("$.Success").value(true))
                .andExpect(jsonPath("$.Message").value(SUCCESS.name()));
    }

    @Test
    @DisplayName("POST " + URL_TOKEN + " - Failed (Invalid Credentials)")
    void generateAuthToken_ShouldReturnUnauthorized_WhenCredentialsAreInvalid() throws Exception {
        when(authService.generateAuthToken(any(AuthRequest.class)))
                .thenThrow(new TokenValidationException(INVALID_OR_MISSING_CREDENTIALS));

        mockMvc.perform(post(URL_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(AUTH_REQUEST)))
                .andExpect(status().isUnauthorized());
    }
}
