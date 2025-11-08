package elya.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import elya.objects.RestClientApiResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RestClientApiEngine {
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

    public RestClientApiResponse sendRequest(String method, String urlPath, JsonElement jsonBody, Map<String, String> headers) {
        String fullUrl = urlPath.startsWith("http") ? urlPath : baseUrl + urlPath;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(fullUrl));

        headers.forEach(requestBuilder::header);

        switch (method.toUpperCase()) {
            case "POST":
                String jsonStringBody = jsonBody != null ? gson.toJson(jsonBody) : "{}";
                requestBuilder.header("Content-Type", "application/json");
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonStringBody));
                break;
            case "GET":
                requestBuilder.GET();
                break;
            case "DELETE":
                requestBuilder.DELETE();
                break;
            default:
                throw new UnsupportedOperationException("Method " + method + " is not supported.");
        }

        return executeRequest(requestBuilder.build());
    }

    private RestClientApiResponse executeRequest(HttpRequest request) {
        RestClientApiResponse clientResponse = new RestClientApiResponse();

        try {
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            String body = httpResponse.body();

            clientResponse.setResponseAsString(body);

            clientResponse.setStatus(Map.of("code", String.valueOf(statusCode),
                    "status", statusCode >= 200 && statusCode < 300 ? "SUCCESS" : "ERROR"));

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
            clientResponse.setStatus(Map.of("code", "503", "status", "NETWORK_ERROR"));
            clientResponse.setResponseAsString(e.getMessage());
        }

        return clientResponse;
    }
}
