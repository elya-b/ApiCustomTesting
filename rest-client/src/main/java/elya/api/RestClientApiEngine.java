package elya.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import elya.ApiEmulatorHttpStatusInfoGenerator;
import elya.interfaces.IRestClientApi;
import elya.interfaces.IRestClientApiEngine;
import elya.restclient.constants.logs.RestClientException;
import elya.restclient.objects.response.RestClientApiResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static elya.constants.enums.HttpHeaderValues.APPLICATION_JSON;
import static elya.restclient.constants.logs.ErrorLogs.*;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

/**
 * Core engine for executing HTTP requests using Java's native {@link HttpClient}.
 * <p>Handles the entire request lifecycle: URI construction, header mapping,
 * payload serialization, and comprehensive response processing including
 * status metadata generation.</p>
 */
@Slf4j
public class RestClientApiEngine implements IRestClientApi, IRestClientApiEngine {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiEmulatorHttpStatusInfoGenerator statusGenerator;

    @Setter
    private String baseUrl;

    /**
     * Initializes the engine with default timeouts and status generation logic.
     *
     * @param baseUrl         the target API root address.
     * @param statusGenerator service to enrich responses with descriptive HTTP metadata.
     */
    public RestClientApiEngine(String baseUrl, ApiEmulatorHttpStatusInfoGenerator statusGenerator) {
        this.baseUrl = baseUrl;
        this.statusGenerator = statusGenerator;
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
     * @return a populated {@link RestClientApiResponse}.
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
    public boolean delete(String urlPath) {
        return delete(urlPath, Collections.emptyMap());
    }

    @Override
    public boolean delete(String urlPath, Map<String, String> headers) {
        RestClientApiResponse response = sendRequest(HttpMethod.DELETE, urlPath, null, headers);
        return response.isSuccessful();
    }

    /**
     * Extracts the JSON tree from the response, ensuring a non-null node is always returned.
     */
    private JsonNode handleJsonResponse(RestClientApiResponse response) {
        JsonNode root = response.getResponseAsJson();
        log.info("isSuccessful={}, statuses={}, body={}", response.isSuccessful(), response.getStatuses(), response.getResponseAsString());
        if (response.isSuccessful() && root != null) {
            return root;
        }
        return objectMapper.createObjectNode();
    }

    /**
     * Synchronously dispatches the request and transforms the {@link HttpResponse}
     * into a domain-specific {@link RestClientApiResponse}.
     */
    private RestClientApiResponse executeRequest(HttpRequest request) {
        RestClientApiResponse clientResponse = new RestClientApiResponse();

        try {
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            try {
                clientResponse.setStatuses(statusGenerator.generateHttpStatusInfo(HttpStatus.valueOf(statusCode)));
            } catch (Exception e) {
                log.warn("Received non-standard HTTP status code: {}", statusCode);
            }

            String body = httpResponse.body();
            clientResponse.setResponseAsString(body);

            Map<String, String> headersMap = httpResponse.headers().map().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> String.join("; ", e.getValue())
                    ));
            clientResponse.setHeaders(headersMap);

            if (body != null && !body.isBlank()) {
                clientResponse.setResponseAsJson(objectMapper.readTree(body));
            } else {
                clientResponse.setResponseAsJson(objectMapper.createObjectNode());
            }

        } catch (IOException | InterruptedException e) {
            log.error(HTTP_REQUEST_FAILED, request.uri(), e);
            clientResponse.setStatuses(statusGenerator.generateHttpStatusInfo(HttpStatus.SERVICE_UNAVAILABLE));
            clientResponse.setResponseAsString(e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception e) {
            log.error(UNKNOWN_HTTP_STATUS_CODE, request.uri(), e.getMessage());
            clientResponse.setStatuses(statusGenerator.generateHttpStatusInfo(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        return clientResponse;
    }
}