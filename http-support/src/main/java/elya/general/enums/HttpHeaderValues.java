package elya.general.enums;

public enum HttpHeaderValues {
    BEARER ("Bearer "),
    APPLICATION_JSON ("application/json");

    private final String httpHeaderValue;

    HttpHeaderValues (String httpHeaderValue) {
        this.httpHeaderValue = httpHeaderValue;
    }

    @Override
    public String toString() {
        return httpHeaderValue;
    }
}
