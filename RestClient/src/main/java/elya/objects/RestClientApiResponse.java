package elya.objects;

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
    private Map<String, String> status;
    private String responseAsString;
    private JsonElement responseAsJson;
    private Map<String, String> headers = new HashMap<>();

    public boolean isSuccessful() {
        return "200".equals(status.get("code"));
    }
}
