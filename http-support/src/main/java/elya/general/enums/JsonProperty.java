package elya.general.enums;

public enum JsonProperty {
    LOGIN("login"),
    PASSWORD("password");

    private final String jsonProperty;

    JsonProperty (String jsonProperty) {
        this.jsonProperty = jsonProperty;
    }

    public String getJsonProperty() {
        return jsonProperty;
    }

    @Override
    public String toString() {
        return jsonProperty;
    }
}
