package elya.json2object;

public enum ApiBankCardField {
    CARD_ID("CARD_ID"),
    CARD_NUMBER("CARD_NUMBER"),
    CARD_TYPE("CARD_TYPE"),
    CARD_STATUS("CARD_STATUS"),
    CURRENCY("CURRENCY"),
    BALANCE("BALANCE");


    private final String fieldName;

    ApiBankCardField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
