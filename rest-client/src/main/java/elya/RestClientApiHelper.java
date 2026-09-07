package elya;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Utility class for JSON data processing using the Jackson library.
 * <p>Provides centralized methods for transforming {@link JsonNode} structures
 * into Java entities and vice-versa, ensuring consistent serialization behavior
 * across the entire REST client module.</p>
 */
@UtilityClass
public class RestClientApiHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Converts a {@link JsonNode} into an instance of the specified class.
     *
     * @param jsonNode the source JSON structure.
     * @param classOfT the target class for conversion.
     * @param <T>      the type of the resulting object.
     * @return an object of type T, or {@code null} if conversion is not possible.
     */
    public static <T> T castFromJson(JsonNode jsonNode, Class<T> classOfT) {
        return objectMapper.convertValue(jsonNode, classOfT);
    }

    /**
     * Converts a {@link JsonNode} into a list of objects using a {@link TypeReference}.
     * <p>This approach is essential for maintaining type safety when dealing with
     * generic collections during deserialization.</p>
     *
     * @param jsonNode      the source JSON node (typically an array).
     * @param typeReference the specific list type reference.
     * @param <T>           the type of elements in the list.
     * @return a list of objects of type T.
     */
    public static <T> List<T> castListFromJson(JsonNode jsonNode, TypeReference<List<T>> typeReference) {
        return objectMapper.convertValue(jsonNode, typeReference);
    }

    /**
     * Serializes a Java object into a {@link JsonNode} tree.
     *
     * @param object the source object to be transformed.
     * @return a {@link JsonNode} representation, or {@code null} if the input object is null.
     */
    public static JsonNode castToJson(Object object) {
        if (object == null) {
            return null;
        }
        return objectMapper.valueToTree(object);
    }

    /**
     * Validates if the provided {@link JsonNode} contains meaningful data.
     * <p>A node is considered valid if it is not null, not missing, is an object,
     * and contains at least one field.</p>
     *
     * @param json the JSON node to check.
     * @return {@code true} if the node contains usable content; {@code false} otherwise.
     */
    public static boolean hasContent(JsonNode json) {
        return json != null
                && !json.isMissingNode()
                && json.isObject()
                && !json.isEmpty();
    }
}