package elya.general.enums.responsemodel;

public enum ApiBankCards {
    RESPONSE("response"),
    SIZE("size"),
    CARDS("cards");

    private final String key;

    ApiBankCards(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return key;
    }
}
