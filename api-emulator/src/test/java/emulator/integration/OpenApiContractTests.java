package emulator.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static elya.constants.ApiEndpoints.URL_BANK_CARD_DATA;
import static elya.constants.ApiEndpoints.URL_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for the generated OpenAPI document ({@code /v3/api-docs}).
 * <p>The document is checked against what the emulator actually does, so the Swagger UI
 * cannot silently drift from the code (as the {@code "DEBIT"} example once did).</p>
 * <ul>
 *   <li>Token endpoint — documented as public, without a security requirement</li>
 *   <li>Bank card endpoints — require the bearer token and document 401</li>
 *   <li>Every operation — documents 500</li>
 *   <li>Every error response — an RFC 9457 problem detail ({@code application/problem+json})</li>
 *   <li>Enum fields — list their JSON values, and the example is one of them (parameterized)</li>
 * </ul>
 */
@SpringBootTest(classes = elya.ApiEmulator.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OpenApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String API_DOCS_PATH = "/v3/api-docs";
    private static final String BEARER_AUTH = "bearerAuth";
    private static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";
    private static final String PROBLEM_DETAIL_REF = "#/components/schemas/" + PROBLEM_DETAIL_SCHEMA;
    private static final String UNAUTHORIZED = "401";
    private static final String INTERNAL_SERVER_ERROR = "500";
    private static final Pattern ERROR_STATUS = Pattern.compile("[45]\\d{2}");
    private static final List<String> CARD_TYPE_VALUES = List.of("debit", "credit");
    private static final List<String> CURRENCY_VALUES = List.of("USD", "EUR", "JPY");

    @Test
    @DisplayName("POST " + URL_TOKEN + " - Should be documented as public")
    void tokenEndpoint_ShouldNotRequireAuthentication() throws Exception {
        JsonNode tokenOperation = apiDocs().path("paths").path(URL_TOKEN).path("post");

        assertAll("Token endpoint must be public",
                () -> assertFalse(tokenOperation.isMissingNode(), "POST " + URL_TOKEN + " must be documented"),
                () -> assertFalse(tokenOperation.has("security"), "POST " + URL_TOKEN + " must not require a token"));
    }

    @Test
    @DisplayName(URL_BANK_CARD_DATA + " - Every operation should require the bearer token and document 401")
    void bankCardOperations_ShouldRequireBearerTokenAndDocument401() throws Exception {
        List<ApiOperation> bankCardOperations = operations(apiDocs()).stream()
                .filter(operation -> operation.path().startsWith(URL_BANK_CARD_DATA))
                .toList();

        assertThat(bankCardOperations).as("Bank card operations must be documented").isNotEmpty();
        assertSoftly(softly -> bankCardOperations.forEach(operation -> {
            softly.assertThat(operation.node().path("security").findValues(BEARER_AUTH))
                    .as("%s must require %s", operation, BEARER_AUTH)
                    .isNotEmpty();
            softly.assertThat(operation.node().path("responses").has(UNAUTHORIZED))
                    .as("%s must document 401", operation)
                    .isTrue();
        }));
    }

    @Test
    @DisplayName("All operations - Should document 500")
    void everyOperation_ShouldDocument500() throws Exception {
        List<ApiOperation> operations = operations(apiDocs());

        assertSoftly(softly -> operations.forEach(operation ->
                softly.assertThat(operation.node().path("responses").has(INTERNAL_SERVER_ERROR))
                        .as("%s must document 500", operation)
                        .isTrue()));
    }

    @Test
    @DisplayName("All error responses - Should be RFC 9457 problem details")
    void everyErrorResponse_ShouldUseProblemDetailSchema() throws Exception {
        JsonNode apiDocs = apiDocs();
        List<ApiOperation> operations = operations(apiDocs);

        assertSoftly(softly -> {
            softly.assertThat(apiDocs.path("components").path("schemas").has(PROBLEM_DETAIL_SCHEMA))
                    .as("The %s schema must be declared", PROBLEM_DETAIL_SCHEMA)
                    .isTrue();
            operations.forEach(operation -> operation.node().path("responses").properties().stream()
                    .filter(response -> ERROR_STATUS.matcher(response.getKey()).matches())
                    .forEach(response -> {
                        JsonNode content = response.getValue().path("content");
                        softly.assertThat(content.size())
                                .as("%s %s must have only one media type", operation, response.getKey())
                                .isEqualTo(1);
                        softly.assertThat(content.path(APPLICATION_PROBLEM_JSON_VALUE).path("schema").path("$ref").asText())
                                .as("%s %s must be a problem detail", operation, response.getKey())
                                .isEqualTo(PROBLEM_DETAIL_REF);
                    }));
        });
    }

    @ParameterizedTest(name = "[{index}] {0}.{1}")
    @MethodSource("enumFields")
    @DisplayName("Enum fields - Should list the JSON values and use one of them as the example")
    void enumField_ShouldListJsonValuesAndValidExample(String schemaName, String fieldName,
                                                       List<String> expectedValues) throws Exception {
        JsonNode field = apiDocs().path("components").path("schemas")
                .path(schemaName).path("properties").path(fieldName);
        List<String> values = new ArrayList<>();
        field.path("enum").forEach(value -> values.add(value.asText()));
        String example = field.path("example").asText();

        assertAll(schemaName + "." + fieldName + " must match its JSON contract",
                () -> assertEquals(expectedValues, values, "Enum must list the JSON values"),
                () -> assertTrue(values.contains(example),
                        "Example '" + example + "' must be one of " + values));
    }

    private static Stream<Arguments> enumFields() {
        return Stream.of(
                Arguments.of("BankCardRequest", "cardType", CARD_TYPE_VALUES),
                Arguments.of("BankCardRequest", "currency", CURRENCY_VALUES),
                Arguments.of("BankCardResponse", "cardType", CARD_TYPE_VALUES),
                Arguments.of("BankCardResponse", "currency", CURRENCY_VALUES));
    }

    private JsonNode apiDocs() throws Exception {
        String json = mockMvc.perform(get(API_DOCS_PATH))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private static List<ApiOperation> operations(JsonNode apiDocs) {
        List<ApiOperation> operations = new ArrayList<>();
        apiDocs.path("paths").properties().forEach(path ->
                path.getValue().properties().forEach(method ->
                        operations.add(new ApiOperation(path.getKey(), method.getKey(), method.getValue()))));
        return operations;
    }

    /**
     * One documented operation: an HTTP method on a path.
     *
     * @param path   the documented path
     * @param method the HTTP method in lower case, as OpenAPI writes it
     * @param node   the operation object
     */
    private record ApiOperation(String path, String method, JsonNode node) {

        @Override
        public String toString() {
            return method.toUpperCase(Locale.ROOT) + " " + path;
        }
    }
}