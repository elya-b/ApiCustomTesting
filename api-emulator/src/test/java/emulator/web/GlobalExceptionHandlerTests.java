package emulator.web;

import elya.*;
import elya.emulator.services.*;
import elya.emulator.web.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.http.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.*;

import java.util.stream.*;

import static elya.constants.ApiEndpoints.*;
import static elya.emulator.constants.messages.ExceptionMessages.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Named.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests (WebMvcTest) for {@link elya.emulator.web.GlobalExceptionHandler}.
 * <p>Every error body is an RFC 9457 problem detail ({@code type}, {@code title},
 * {@code status}, {@code detail}, {@code instance}).</p>
 * <ul>
 *   <li>Unknown path — 404 with the full problem detail contract</li>
 *   <li>Unsupported HTTP method — 405 with the {@code Allow} header</li>
 *   <li>Unsupported Content-Type — 415</li>
 *   <li>Path variable of the wrong type — 400</li>
 *   <li>Unreadable request body — 400 with the reason per cause (parameterized)</li>
 *   <li>Bean validation failure — 400 with the field validation message</li>
 *   <li>Missing Authorization header — 401 with {@code WWW-Authenticate: Bearer}</li>
 *   <li>Unexpected exception — 500 without internal details</li>
 *   <li>Error content type — {@code application/problem+json} even for a client that accepts {@code application/json}</li>
 * </ul>
 */
@WebMvcTest(ApiEmulatorController.class)
@ContextConfiguration(classes = ApiEmulator.class)
public class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authService;

    @MockitoBean
    private MockService mockService;

    @MockitoBean
    private TokenManagerService tokenManagerService;

    // Request
    private static final String FULL_TOKEN = "Bearer token";
    private static final String CLEAN_TOKEN = "token";
    private static final String UNKNOWN_PATH = "/api/v1/elya-bank/unknown";
    private static final String NOT_A_NUMBER = "abc";
    private static final String PLAIN_TEXT_BODY = "cards";

    // Request bodies
    private static final String EMPTY_BODY = "";
    private static final String BROKEN_JSON = "{";
    private static final String JSON_ARRAY = "[]";
    private static final String VALID_CARD_NUMBER = "4444333322221111";
    private static final String SHORT_CARD_NUMBER = "123";
    private static final String NOT_A_NUMBER_JSON = "\"" + NOT_A_NUMBER + "\"";
    private static final String CARD_TYPE_JSON_VALUE = "\"debit\"";
    private static final String CARD_TYPE_CONSTANT_NAME = "\"DEBIT\"";
    private static final String CARD_JSON_TEMPLATE = """
            {"cards":[{"cardNumber":%s,"cardType":%s,"currency":"EUR"}]}""";

    // Expected response (RFC 9457 problem detail)
    private static final String TYPE_FIELD = "$.type";
    private static final String TITLE_FIELD = "$.title";
    private static final String STATUS_FIELD = "$.status";
    private static final String DETAIL_FIELD = "$.detail";
    private static final String INSTANCE_FIELD = "$.instance";
    private static final String BLANK_PROBLEM_TYPE = "about:blank";
    private static final String BEARER_CHALLENGE = "Bearer";
    private static final String CARD_TYPE_PATH = "cards[0].cardType";
    private static final String CARD_NUMBER_PATH = "cards[0].cardNumber";
    private static final String CARD_NUMBER_LENGTH_MESSAGE = "Card number must be 16 digits";
    private static final String INTERNAL_DETAIL = "connection pool exhausted";

    @Test
    @DisplayName("GET unknown path - Should return 404 as an RFC 9457 problem detail")
    void unknownPath_ShouldReturn404ProblemDetail_WhenNoHandlerMatches() throws Exception {
        mockMvc.perform(get(UNKNOWN_PATH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(TYPE_FIELD).value(BLANK_PROBLEM_TYPE))
                .andExpect(jsonPath(TITLE_FIELD).value(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .andExpect(jsonPath(STATUS_FIELD).value(HttpStatus.NOT_FOUND.value()))
                .andExpect(jsonPath(DETAIL_FIELD).value(RESOURCE_NOT_FOUND))
                .andExpect(jsonPath(INSTANCE_FIELD).value(UNKNOWN_PATH));
    }

    @Test
    @DisplayName("PUT " + URL_BANK_CARD_DATA + " - Should return 405 with the Allow header")
    void unsupportedMethod_ShouldReturn405WithAllowHeader() throws Exception {
        mockMvc.perform(put(URL_BANK_CARD_DATA))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(ALLOW, containsString(HttpMethod.GET.name())))
                .andExpect(jsonPath(STATUS_FIELD).value(HttpStatus.METHOD_NOT_ALLOWED.value()));
    }

    @Test
    @DisplayName("POST " + URL_BANK_CARD_DATA + " - Should return 415 for a non-JSON body")
    void unsupportedMediaType_ShouldReturn415_WhenContentTypeIsNotJson() throws Exception {
        mockMvc.perform(post(URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, FULL_TOKEN)
                        .contentType(TEXT_PLAIN)
                        .content(PLAIN_TEXT_BODY))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath(STATUS_FIELD).value(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()));

        verifyNoInteractions(mockService);
    }

    @Test
    @DisplayName("GET " + URL_BANK_CARD_DATA + "/{cardId} - Should return 400 when cardId is not a number")
    void typeMismatch_ShouldReturn400_WhenCardIdIsNotANumber() throws Exception {
        mockMvc.perform(get(URL_BANK_CARD_DATA + "/" + NOT_A_NUMBER)
                        .header(AUTHORIZATION, FULL_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(STATUS_FIELD).value(HttpStatus.BAD_REQUEST.value()));

        verifyNoInteractions(mockService);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unreadableBodies")
    @DisplayName("POST " + URL_BANK_CARD_DATA + " - Should return 400 with the reason the body is unreadable")
    void unreadableBody_ShouldReturn400WithReason(String body, String expectedDetail) throws Exception {
        mockMvc.perform(post(URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, FULL_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(STATUS_FIELD).value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath(DETAIL_FIELD).value(expectedDetail));

        verifyNoInteractions(mockService);
    }

    @Test
    @DisplayName("POST " + URL_BANK_CARD_DATA + " - Should return 400 with the field validation message")
    void validation_ShouldReturn400WithFieldMessage_WhenCardNumberIsTooShort() throws Exception {
        mockMvc.perform(post(URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, FULL_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .content(cardJson(SHORT_CARD_NUMBER, CARD_TYPE_JSON_VALUE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL_FIELD).value(CARD_NUMBER_LENGTH_MESSAGE));

        verifyNoInteractions(mockService);
    }

    @Test
    @DisplayName("GET " + URL_BANK_CARD_DATA + " - Should return 401 with the Bearer challenge")
    void unauthorized_ShouldReturn401WithBearerChallenge_WhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(get(URL_BANK_CARD_DATA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(WWW_AUTHENTICATE, BEARER_CHALLENGE))
                .andExpect(jsonPath(STATUS_FIELD).value(HttpStatus.UNAUTHORIZED.value()))
                .andExpect(jsonPath(DETAIL_FIELD).value(INVALID_AUTHORIZATION_HEADER));
    }

    @Test
    @DisplayName("GET " + URL_BANK_CARD_DATA + " - Should return 500 without internal details")
    void unexpectedException_ShouldReturn500WithoutInternalDetails() throws Exception {
        when(mockService.getApiBankCards(CLEAN_TOKEN)).thenThrow(new IllegalStateException(INTERNAL_DETAIL));

        mockMvc.perform(get(URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, FULL_TOKEN))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath(STATUS_FIELD).value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .andExpect(jsonPath(DETAIL_FIELD).value(UNEXPECTED_INTERNAL_ERROR))
                .andExpect(content().string(not(containsString(INTERNAL_DETAIL))));
    }

    @Test
    @DisplayName("POST " + URL_BANK_CARD_DATA + " - Should answer a JSON client with application/problem+json")
    void errorResponse_ShouldUseProblemJsonContentType_WhenClientAcceptsJson() throws Exception {
        mockMvc.perform(post(URL_BANK_CARD_DATA)
                        .header(AUTHORIZATION, FULL_TOKEN)
                        .contentType(APPLICATION_JSON)
                        .accept(APPLICATION_JSON)
                        .content(BROKEN_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON));
    }

    private static Stream<Arguments> unreadableBodies() {
        return Stream.of(
                Arguments.of(named("empty body", EMPTY_BODY), REQUEST_BODY_MISSING),
                Arguments.of(named("broken JSON", BROKEN_JSON), MALFORMED_REQUEST_BODY),
                Arguments.of(named("array instead of object", JSON_ARRAY), MALFORMED_REQUEST_BODY),
                Arguments.of(named("enum constant name instead of its JSON value",
                                cardJson(VALID_CARD_NUMBER, CARD_TYPE_CONSTANT_NAME)),
                        INVALID_FIELD_VALUE.formatted(CARD_TYPE_PATH)),
                Arguments.of(named("text in a numeric field",
                                cardJson(NOT_A_NUMBER_JSON, CARD_TYPE_JSON_VALUE)),
                        INVALID_FIELD_VALUE.formatted(CARD_NUMBER_PATH)));
    }

    private static String cardJson(String cardNumber, String cardType) {
        return CARD_JSON_TEMPLATE.formatted(cardNumber, cardType);
    }
}