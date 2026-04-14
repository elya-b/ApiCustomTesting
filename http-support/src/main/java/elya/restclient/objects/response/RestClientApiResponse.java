package elya.restclient.objects.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

import static elya.constants.enums.StatusInfo.STATUS;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RestClientApiResponse {
    private Map<String, Object> statuses;
    private String responseAsString;
    private JsonNode responseAsJson;
    private Map<String, String> headers = new HashMap<>();

    public boolean isSuccessful() {
        if (statuses != null && statuses.get(STATUS.toString()) instanceof Number codeObj) {
            int code = codeObj.intValue();
            return code >= 200 && code < 300;
        }

        return false;
    }
}
