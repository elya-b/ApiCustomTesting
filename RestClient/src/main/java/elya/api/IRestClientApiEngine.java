package elya.api;

import com.google.gson.JsonElement;
import elya.objects.RestClientApiResponse;
import org.springframework.http.HttpMethod;

import java.util.Map;

public interface IRestClientApiEngine {
    RestClientApiResponse sendRequest(
            HttpMethod method,
            String urlPath,
            JsonElement jsonBody,
            Map<String, String> headers
    );
}
