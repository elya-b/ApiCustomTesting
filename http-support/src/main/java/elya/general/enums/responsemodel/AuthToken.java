package elya.general.enums.responsemodel;

public enum AuthToken {
    TOKEN("token"),
    TYPE("type"),
    EXPIRES_IN("expiresIn");

    private final String responseKey;

    AuthToken(String responseKey) {
        this.responseKey = responseKey;
    }

    @Override
    public String toString() {
        return responseKey;
    }
}
