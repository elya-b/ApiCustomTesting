package elya.restclient.objects.response;

import com.google.gson.JsonElement;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RestClientApiResponse {
    private Map<String, String> statuses;
    private String responseAsString;
    private JsonElement responseAsJson;
    private Map<String, String> headers = new HashMap<>();

    public boolean isSuccessful() {
        String codeString = statuses.get("code");
        if (codeString == null) return false;

        try {
            int code = Integer.parseInt(codeString);
            return code >= 200 && code < 300;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
