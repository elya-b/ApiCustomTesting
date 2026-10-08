package elya.restclient.objects.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

import static elya.constants.enums.StatusInfo.STATUS;

/**
 * Universal container for HTTP responses within the REST client.
 * <p>Wraps raw response data, parsed JSON structures, HTTP headers, and
 * status information into a single object for easy processing across
 * different service layers.</p>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RestClientApiResponse {

    /** Map containing status-related information, such as HTTP codes. */
    private Map<String, Object> statuses;

    /** The raw response body as a string. */
    private String responseAsString;

    /** The response body parsed into a Jackson {@link JsonNode} for easy traversal. */
    private JsonNode responseAsJson;

    /** HTTP headers received from the server. */
    private Map<String, String> headers = new HashMap<>();

    /**
     * Extracts the HTTP status code from the status metadata.
     *
     * @return the status code, or {@code -1} if the response carries no usable status.
     */
    public int getStatusCode() {
        if (statuses != null && statuses.get(STATUS.toString()) instanceof Number codeObj) {
            return codeObj.intValue();
        }
        return -1;
    }

    /**
     * Determines if the request was successful based on the HTTP status code.
     * <p>A request is considered successful if the status code is within
     * the 200-299 range.</p>
     *
     * @return {@code true} if the status code indicates success; {@code false} otherwise.
     */
    public boolean isSuccessful() {
        int code = getStatusCode();
        return code >= 200 && code < 300;
    }
}