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
 * Core engine for executing HTTP requests using Java's native HttpClient.
 * Implements centralized request handling with detailed response metadata.
 */
@Slf4j
@Component
public class RestClientApiEngine implements IRestClientApi, IRestClientApiEngine {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiEmulatorHttpStatusInfoGenerator statusGenerator;
    @Setter
    private String baseUrl;

    /**
     * Initializes the engine with base connection settings and status generator.
     *
     * @param baseUrl         target server base address
     * @param statusGenerator service for HTTP status metadata generation
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
     * Low-level method to construct and dispatch HTTP requests.
     * Automatically handles JSON serialization and header mapping.
     *
     * @param method   HTTP verb (GET, POST, etc.)
     * @param urlPath  target endpoint path
     * @param jsonBody optional payload for the request
     * @param headers  map of HTTP headers
     * @return         wrapped RestClientApiResponse containing all execution data
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
        if (jsonBody != null) {
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

        requestBuilder.method(method.toString(), bodyPublisher);
        return executeRequest(requestBuilder.build());
    }

    /**
     * Executes a POST request and returns the serialized JSON response.
     *
     * @param urlPath  endpoint path
     * @param jsonBody payload to send
     * @param headers  request headers
     * @return         JsonNode containing the response body
     */
    @Override
    public JsonNode post(String urlPath, JsonNode jsonBody, Map<String, String> headers) {
        return handleJsonResponse(sendRequest(HttpMethod.POST, urlPath, jsonBody, headers));
    }

    /**
     * Executes a GET request and returns the serialized JSON response.
     *
     * @param urlPath endpoint path
     * @param headers request headers
     * @return        JsonNode containing the response body
     */
    @Override
    public JsonNode get(String urlPath, Map<String, String> headers) {
        return handleJsonResponse(sendRequest(HttpMethod.GET, urlPath, null, headers));
    }

    /**
     * Executes a standard DELETE request without additional headers.
     *
     * @param urlPath endpoint path
     * @return        true if the operation was successful
     */
    @Override
    public boolean delete(String urlPath) {
        return delete(urlPath, Collections.emptyMap());
    }

    /**
     * Executes an authorized DELETE request with custom headers.
     *
     * @param urlPath endpoint path
     * @param headers map containing security tokens or metadata
     * @return        true if the server responded with a success code
     */
    @Override
    public boolean delete(String urlPath, Map<String, String> headers) {
        RestClientApiResponse response = sendRequest(HttpMethod.DELETE, urlPath, null, headers);
        return response.isSuccessful();
    }

    /**
     * Extracts and validates the JSON body from the API response.
     *
     * @param response raw execution results
     * @return         valid JsonNode or an empty object node if failed
     */
    private JsonNode handleJsonResponse(RestClientApiResponse response) {
        JsonNode root = response.getResponseAsJson();
        if (response.isSuccessful() && root != null) {
            return root;
        }
        return objectMapper.createObjectNode();
    }

    /**
     * Performs the actual HTTP exchange and populates the response object.
     *
     * @param request the configured HttpRequest to be executed
     * @return        populated RestClientApiResponse with data from the server
     */
    private RestClientApiResponse executeRequest(HttpRequest request) {
        RestClientApiResponse clientResponse = new RestClientApiResponse();

        try {
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            clientResponse.setStatuses(statusGenerator.generateHttpStatusInfo(HttpStatus.valueOf(statusCode)));

            String body = httpResponse.body();
            clientResponse.setResponseAsString(body);

            Map<String, String> headersMap = httpResponse.headers().map().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> e.getValue().stream().collect(Collectors.joining("; "))));
            clientResponse.setHeaders(headersMap);

            if (body != null && !body.trim().isEmpty()) {
                clientResponse.setResponseAsJson(objectMapper.readTree(body));
            } else {
                clientResponse.setResponseAsJson(objectMapper.createObjectNode());
            }

        } catch (IOException | InterruptedException e) {
            log.error(HTTP_REQUEST_FAILED, request.uri(), e);
            clientResponse.setStatuses(statusGenerator.generateHttpStatusInfo(HttpStatus.SERVICE_UNAVAILABLE));
            clientResponse.setResponseAsString(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error(UNKNOWN_HTTP_STATUS_CODE, request.uri(), e.getMessage());
            clientResponse.setStatuses(statusGenerator.generateHttpStatusInfo(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        return clientResponse;
    }
}