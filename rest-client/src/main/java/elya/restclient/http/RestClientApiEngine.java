package elya.restclient.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import elya.restclient.exceptions.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.http.*;

import java.io.*;
import java.net.*;
import java.net.http.HttpRequest;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

import static elya.constants.enums.HttpHeaderValues.*;
import static elya.restclient.constants.logs.ErrorLogs.*;
import static elya.restclient.constants.messages.ExceptionMessages.*;
import static org.springframework.http.HttpHeaders.*;

/**
 * Core engine for executing HTTP requests using Java's native {@link HttpClient}.
 * <p>Keeps three kinds of failure apart, so a caller always knows which one happened:</p>
 * <ul>
 *   <li>no response was received — {@link ApiTransportException};</li>
 *   <li>the server answered with a non-2xx status — {@link ApiHttpStatusException};</li>
 *   <li>a successful response body is not valid JSON — {@link RestClientException}.</li>
 * </ul>
 */
@Slf4j
public class RestClientApiEngine implements IRestClientApi, IRestClientApiEngine {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Setter
    private String baseUrl;

    /**
     * Initializes the engine with default timeouts.
     *
     * @param baseUrl the target API root address.
     */
    public RestClientApiEngine(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Low-level dispatcher that converts abstract request data into an {@link HttpRequest}.
     *
     * @param method   the HTTP verb.
     * @param urlPath  the target endpoint (relative or absolute).
     * @param jsonBody optional JSON payload.
     * @param headers  custom HTTP headers.
     * @return the response exactly as the server sent it, whatever its status.
     * @throws ApiTransportException if no HTTP response was received.
     */
    @Override
    public RestClientApiResponse sendRequest(HttpMethod method,
                                             String urlPath,
                                             JsonNode jsonBody,
                                             Map<String, String> headers) {
        String fullUrl = urlPath.startsWith("http") ? urlPath : baseUrl + urlPath;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(fullUrl));

        headers.forEach(requestBuilder::header);

        HttpRequest.BodyPublisher bodyPublisher;
        if (jsonBody != null && !jsonBody.isMissingNode()) {
            requestBuilder.header(CONTENT_TYPE, APPLICATION_JSON.toString());
            try {
                bodyPublisher = HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(jsonBody));
            } catch (IOException e) {
                log.error(JSON_SERIALIZATION_FAILED, e.getMessage());
                throw new RestClientException(JSON_SERIALIZATION_FAILED, e);
            }
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }

        requestBuilder.method(method.name(), bodyPublisher);
        return executeRequest(requestBuilder.build());
    }

    @Override
    public JsonNode post(String urlPath, JsonNode jsonBody, Map<String, String> headers) {
        return handleJsonResponse(sendRequest(HttpMethod.POST, urlPath, jsonBody, headers));
    }

    @Override
    public JsonNode get(String urlPath, Map<String, String> headers) {
        return handleJsonResponse(sendRequest(HttpMethod.GET, urlPath, null, headers));
    }

    @Override
    public void delete(String urlPath) {
        delete(urlPath, Collections.emptyMap());
    }

    @Override
    public void delete(String urlPath, Map<String, String> headers) {
        ensureSuccessful(sendRequest(HttpMethod.DELETE, urlPath, null, headers));
    }

    /**
     * Parses the body of a successful response as JSON.
     *
     * @param response the raw client response.
     * @return the parsed body, or an empty object node when the body was empty.
     * @throws ApiHttpStatusException if the server answered with a non-2xx status.
     * @throws RestClientException    if the body of a successful response is not valid JSON.
     */
    private JsonNode handleJsonResponse(RestClientApiResponse response) {
        ensureSuccessful(response);

        String body = response.body();
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new RestClientException(RESPONSE_NOT_JSON, e);
        }
    }

    /**
     * Fails fast when the server did not answer with a 2xx status.
     * <p>Every verb routes through this method, so a 404 on DELETE reaches the
     * caller exactly the way a 404 on GET does. Keeping the classification in one
     * place is what stops the two paths from drifting apart again.</p>
     *
     * @param response the raw client response.
     * @throws ApiHttpStatusException if the server answered with a non-2xx status.
     */
    private void ensureSuccessful(RestClientApiResponse response) {
        if (!response.isSuccessful()) {
            throw new ApiHttpStatusException(response.statusCode(), response.body());
        }
    }

    /**
     * Synchronously dispatches the request and returns the response exactly as received.
     *
     * @throws ApiTransportException if the request failed before a response was received.
     */
    private RestClientApiResponse executeRequest(HttpRequest request) {
        try {
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return new RestClientApiResponse(httpResponse.statusCode(), httpResponse.body(), headersOf(httpResponse));
        } catch (IOException e) {
            throw new ApiTransportException(TRANSPORT_FAILURE.formatted(request.uri()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiTransportException(TRANSPORT_FAILURE.formatted(request.uri()), e);
        }
    }

    /**
     * Flattens the response headers, joining the values of a repeated header with {@code "; "}.
     */
    private static Map<String, String> headersOf(HttpResponse<?> httpResponse) {
        return httpResponse.headers().map().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.join("; ", e.getValue())
                ));
    }
}