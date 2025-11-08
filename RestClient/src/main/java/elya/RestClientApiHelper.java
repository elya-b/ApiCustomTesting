package elya;

import com.fasterxml.jackson.databind.ser.std.ClassSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;
import java.util.List;

public class RestClientApiHelper {
    private static final Gson gson = new GsonBuilder().create();

    public static <T> T castFromJson(JsonElement jsonElement, Class<T> classOfT) {
        return gson.fromJson(jsonElement, classOfT);
    }

    public static <T> List<T> castListFromJson(JsonElement jsonElement, Type listType, Gson customGson) {
        return customGson.fromJson(jsonElement, listType);
    }

    public static JsonElement castToJson(Object object) {
        if (object == null) {
            return null;
        }
        return gson.toJsonTree(object);
    }}
