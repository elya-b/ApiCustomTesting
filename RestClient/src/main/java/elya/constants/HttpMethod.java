package elya.constants;

public enum HttpMethod {
    GET("GET"),
    POST("POST"),
    DELETE("DELETE"),
    PUT("PUT"),
    PATCH("PATCH"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS");

    private final String method;

    HttpMethod(String method) {
        this.method = method;
    }

    public String getMethodName() {
        return method;
    }
}
