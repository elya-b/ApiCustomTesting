package elya;

import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class RestClientApiHelper {
    private static final Gson gson = new GsonBuilder().create();

    public static <T> T castFromJson(JsonElement jsonElement, Class<T> classOfT) {
        return gson.fromJson(jsonElement, classOfT);
    }

    public static JsonElement castToJson(String body) {
        return gson.toJsonTree(body);
    }
}
