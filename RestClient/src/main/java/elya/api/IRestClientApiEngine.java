package elya.api;

import com.google.gson.JsonElement;
import elya.constants.HttpMethod;
import elya.objects.RestClientApiResponse;

import java.util.Map;

public interface IRestClientApiEngine {
    RestClientApiResponse sendRequest(
            HttpMethod method,
            String urlPath,
            JsonElement jsonBody,
            Map<String, String> headers
    );

    JsonElement post(String urlPath, JsonElement jsonBody, Map<String, String> headers);

    JsonElement get(String urlPath, Map<String, String> headers);

    boolean delete(String urlPath);
}
