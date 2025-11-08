package elya.constants;

public enum HttpHeaders {
    AUTHORIZATION("Authorization"),

    ;

    private final String headerName;

    HttpHeaders(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderName() {
        return headerName;
    }
}
