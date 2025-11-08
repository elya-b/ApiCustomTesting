package elya.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elya.ApiEmulatorStatusesGenerator;
import elya.constants.HttpMethod;
import elya.objects.RestClientApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RestClientApiEngine implements IRestClientApiEngine, ApiEmulatorStatusesGenerator {
    private final HttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;

    public RestClientApiEngine(String baseUrl) {
        this.baseUrl = baseUrl;
        this.gson = new Gson();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public RestClientApiResponse sendRequest(HttpMethod method, String urlPath, JsonElement jsonBody, Map<String, String> headers) {
        String fullUrl = urlPath.startsWith("http") ? urlPath : baseUrl + urlPath;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(fullUrl));

        headers.forEach(requestBuilder::header);

        if (jsonBody != null) {
            requestBuilder.header("Content-Type", "application/json");
        }

        HttpRequest.BodyPublisher bodyPublisher = jsonBody != null
                ? HttpRequest.BodyPublishers.ofString(gson.toJson(jsonBody))
                : HttpRequest.BodyPublishers.noBody();

        requestBuilder.method(method.getMethodName(), bodyPublisher);

        return executeRequest(requestBuilder.build());
    }

    @Override
    public JsonElement post(String urlPath, JsonElement jsonBody, Map<String, String> headers) {
        RestClientApiResponse response = sendRequest(HttpMethod.POST, urlPath, jsonBody, headers);

        if (response.isSuccessful() && response.getResponseAsJson() != null) {
            return response.getResponseAsJson();
        } else {
            return null;
        }
    }

    @Override
    public JsonElement get(String urlPath, Map<String, String> headers) {
        RestClientApiResponse response = sendRequest(HttpMethod.GET, urlPath, null, headers);

        if (response.isSuccessful() && response.getResponseAsJson() != null) {
            return response.getResponseAsJson();
        } else {
            return null;
        }
    }

    @Override
    public boolean delete(String urlPath) {
        RestClientApiResponse response = sendRequest(HttpMethod.DELETE, urlPath, null, Collections.emptyMap());

        return response.isSuccessful();
    }

    private RestClientApiResponse executeRequest(HttpRequest request) {
        RestClientApiResponse clientResponse = new RestClientApiResponse();

        try {
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            String body = httpResponse.body();

            clientResponse.setStatus(generateHttpStatus(HttpStatus.valueOf(statusCode)));

            clientResponse.setResponseAsString(body);

            Map<String, String> headersMap = httpResponse.headers().map().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> e.getValue().stream().collect(Collectors.joining("; "))));
            clientResponse.setHeaders(headersMap);

            if (body != null && !body.trim().isEmpty()) {
                clientResponse.setResponseAsJson(JsonParser.parseString(body));
            } else {
                clientResponse.setResponseAsJson(new JsonObject());
            }

        } catch (IOException | InterruptedException e) {
            log.error("HTTP request failed for URL: {}", request.uri(), e);

            clientResponse.setStatus(generateHttpStatus(HttpStatus.SERVICE_UNAVAILABLE));
            clientResponse.setResponseAsString(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Unknown HTTP status code received for URL: {}. Code: {}", request.uri(), e.getMessage());
            clientResponse.setStatus(generateHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        return clientResponse;
    }
}
