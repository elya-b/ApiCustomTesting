package elya.json2object;

import com.google.gson.JsonObject;

import java.lang.reflect.Type;

public class SerializerDeserializerHelper {

    private final Type type;

    protected SerializerDeserializerHelper(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    protected boolean isUsable(JsonObject object, String fieldName) {
        return object.has(fieldName) && !object.get(fieldName).isJsonNull();
    }
}
