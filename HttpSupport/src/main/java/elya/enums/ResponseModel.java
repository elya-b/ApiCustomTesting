package elya.enums;

public enum ResponseModel {
    RESPONSE("response"),
    SIZE("size"),
    CARDS("cards");

    private final String responseKey;

    ResponseModel(String responseKey) {
        this.responseKey = responseKey;
    }

    @Override
    public String toString() {
        return responseKey;
    }
}
