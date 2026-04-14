package elya.api;

import com.google.gson.JsonElement;

import java.util.Map;

public interface IRestClientApi {
    JsonElement post(String urlPath, JsonElement jsonBody, Map<String, String> headers);

    JsonElement get(String urlPath, Map<String, String> headers);

    boolean delete(String urlPath);
}
