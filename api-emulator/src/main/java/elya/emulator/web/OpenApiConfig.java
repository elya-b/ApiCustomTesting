package elya.emulator.web;

import elya.emulator.web.auth.*;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.*;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.customizers.*;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.web.method.*;

import java.util.*;
import java.util.regex.*;

import static elya.constants.ApiEndpoints.*;
import static elya.emulator.constants.messages.ExceptionMessages.*;
import static org.springframework.http.MediaType.*;

/**
 * OpenAPI (Swagger) documentation of the API Emulator.
 * <p>The error contract is documented in one place: {@link #errorResponsesCustomizer()} derives
 * the security requirement from {@link SessionToken} and describes every error response as an
 * RFC 9457 problem detail, exactly as {@link GlobalExceptionHandler} produces it.</p>
 */
@Configuration
public class OpenApiConfig {

    /** Name of the security scheme for the session token. */
    private static final String BEARER_AUTH = "bearerAuth";

    /** Name of the schema component that documents every error body. */
    private static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";

    /** Reference to {@link #PROBLEM_DETAIL_SCHEMA} used by every error response. */
    private static final String PROBLEM_DETAIL_REF = "#/components/schemas/" + PROBLEM_DETAIL_SCHEMA;

    /** HTTP status codes of error responses: any 4xx or 5xx. */
    private static final Pattern ERROR_STATUS = Pattern.compile("[45]\\d{2}");

    /** Card ID used in the problem detail example. */
    private static final long EXAMPLE_CARD_ID = 1L;

    /**
     * Declares the bearer security scheme and the shared problem detail schema.
     * <p>No global security requirement is set: an operation requires the token only when
     * its handler takes a {@link SessionToken}, see {@link #errorResponsesCustomizer()}.</p>
     *
     * @return the base {@link OpenAPI} definition
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("Paste the token from POST /auth/token without the 'Bearer ' prefix"))
                        .addSchemas(PROBLEM_DETAIL_SCHEMA, problemDetailSchema()));
    }

    /**
     * Documents the error contract of every operation.
     * <ul>
     *   <li>A handler with a {@link SessionToken} parameter requires the bearer token and may answer {@code 401}</li>
     *   <li>Every operation may answer {@code 500}</li>
     *   <li>Every {@code 4xx}/{@code 5xx} response is an {@code application/problem+json} problem detail;
     *       this replaces the success schema springdoc assigns to error responses by default</li>
     * </ul>
     *
     * @return the customizer applied to every operation
     */
    @Bean
    public OperationCustomizer errorResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (requiresSessionToken(handlerMethod)) {
                operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
                responses.putIfAbsent(String.valueOf(HttpStatus.UNAUTHORIZED.value()),
                        new ApiResponse().description("Missing, invalid or expired session token"));
            }
            responses.putIfAbsent(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                    new ApiResponse().description("Unexpected server error"));
            responses.forEach((code, response) -> {
                if (ERROR_STATUS.matcher(code).matches()) {
                    response.setContent(problemDetailContent());
                }
            });
            return operation;
        };
    }

    /**
     * Checks whether the handler is protected, i.e. takes the authenticated session token.
     *
     * @param handlerMethod the documented handler
     * @return {@code true} if a parameter is annotated with {@link SessionToken}
     */
    private static boolean requiresSessionToken(HandlerMethod handlerMethod) {
        return Arrays.stream(handlerMethod.getMethodParameters())
                .anyMatch(parameter -> parameter.hasParameterAnnotation(SessionToken.class));
    }

    /**
     * Builds the content of an error response: a reference to the shared problem detail schema.
     * <p>A new instance per response, as the OpenAPI model objects are mutable.</p>
     *
     * @return the {@code application/problem+json} content
     */
    private static Content problemDetailContent() {
        return new Content().addMediaType(APPLICATION_PROBLEM_JSON_VALUE,
                new MediaType().schema(new Schema<>().$ref(PROBLEM_DETAIL_REF)));
    }

    /**
     * Describes the RFC 9457 problem detail returned by {@link GlobalExceptionHandler}.
     * <p>Declared explicitly: a schema derived from {@link org.springframework.http.ProblemDetail}
     * by reflection would also list its internal {@code properties} map.</p>
     *
     * @return the problem detail schema with an example of a missing card
     */
    private static Schema<?> problemDetailSchema() {
        return new ObjectSchema()
                .description("Error body as defined by RFC 9457 (Problem Details for HTTP APIs)")
                .addProperty("type", new StringSchema().format("uri").example("about:blank"))
                .addProperty("title", new StringSchema().example(HttpStatus.NOT_FOUND.getReasonPhrase()))
                .addProperty("status", new IntegerSchema().example(HttpStatus.NOT_FOUND.value()))
                .addProperty("detail", new StringSchema().example(CARD_NOT_FOUND.formatted(EXAMPLE_CARD_ID)))
                .addProperty("instance", new StringSchema().format("uri")
                        .example(URL_BANK_CARD_DATA + "/" + EXAMPLE_CARD_ID));
    }
}