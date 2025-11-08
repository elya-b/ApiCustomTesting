package elya.objects;

import elya.ApiEmulatorStatusesGenerator;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class RestClientApiErrorDetails {
    private final Map<String, String> statusDetails;
    private final String responseBody;

    public RestClientApiErrorDetails(RestClientApiResponse response, ApiEmulatorStatusesGenerator generator) {
        int statusCode = Integer.parseInt(response.getStatus().get("code"));
        HttpStatus httpStatus = HttpStatus.resolve(statusCode);

        this.statusDetails = (httpStatus != null)
                ? generator.generateHttpStatus(httpStatus)
                : Map.of("status", String.valueOf(statusCode), "reason", "Unknown Status");

        this.responseBody = response.getResponseAsString();
    }

    public Map<String, String> getStatusDetails() {
        return statusDetails;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
