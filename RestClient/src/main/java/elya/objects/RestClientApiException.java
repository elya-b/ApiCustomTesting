package elya.objects;

public class RestClientApiException extends RuntimeException {
    private final int statusCode;
    private final String statusReason;
    private final String responseBody;

    public RestClientApiException(int statusCode, String statusReason, String responseBody) {
        super(String.format("API call failed with status %d (%s). Body snippet: %s",
                statusCode, statusReason, responseBody.substring(0, Math.min(responseBody.length(), 100))));
        this.statusCode = statusCode;
        this.statusReason = statusReason;
        this.responseBody = responseBody;
    }

    public int getStatusCode() { return statusCode; }
    public String getStatusReason() { return statusReason; }
    public String getResponseBody() { return responseBody; }
}
