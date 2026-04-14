package elya;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Utility class for JSON data processing using the Jackson library.
 * Provides methods for converting JsonNode objects to Java entities and vice-versa.
 */
public class RestClientApiHelper {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converts a JsonNode into an object of the specified class.
     *
     * @param jsonNode  the source JSON node
     * @param classOfT  the target class for deserialization
     * @param <T>       the type of the resulting object
     * @return          an object of type T derived from the JSON
     */
    public static <T> T castFromJson(JsonNode jsonNode, Class<T> classOfT) {
        return objectMapper.convertValue(jsonNode, classOfT);
    }

    /**
     * Converts a JsonNode into a list of objects of the specified type.
     * Utilizes TypeReference to correctly handle generic types.
     *
     * @param jsonNode      the source JSON node (expects an array)
     * @param typeReference the reference for the list type (e.g., new TypeReference<List<MyDto>>(){})
     * @param <T>           the type of list elements
     * @return              a list of objects of type T
     */
    public static <T> List<T> castListFromJson(JsonNode jsonNode, TypeReference<List<T>> typeReference) {
        return objectMapper.convertValue(jsonNode, typeReference);
    }

    /**
     * Converts a Java object into a JsonNode tree.
     *
     * @param object the object to be converted
     * @return       a JsonNode representation of the object, or null if the source object is null
     */
    public static JsonNode castToJson(Object object) {
        if (object == null) {
            return null;
        }
        return objectMapper.valueToTree(object);
    }

    /**
     * Validates if the JsonNode contains useful content.
     * A node is considered valid if it exists, is an object, and is not empty.
     *
     * @param json the JSON node to validate
     * @return     true if the node contains data, otherwise false
     */
    public static boolean hasContent(JsonNode json) {
        return json != null
                && !json.isMissingNode()
                && json.isObject()
                && !json.isEmpty();
    }
}
