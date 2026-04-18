package elya;

import org.springframework.http.HttpStatus;

import java.util.Map;

import static elya.constants.enums.StatusInfo.*;

/**
 * Interface providing utility logic for generating standardized HTTP status information.
 * <p>Designed to be implemented by services or controllers that need to transform
 * {@link HttpStatus} objects into a consistent Map-based representation for API responses.</p>
 */
public interface ApiEmulatorHttpStatusInfoGenerator {

    /**
     * Generates a map containing detailed metadata about the provided HTTP status.
     * <p>The resulting map includes:
     * <ul>
     * <li><b>status</b>: The integer value of the status code.</li>
     * <li><b>series</b>: The enumeration of the HTTP status series (e.g., SUCCESSFUL).</li>
     * <li><b>reason</b>: The standard HTTP reason phrase.</li>
     * </ul></p>
     *
     * @param httpStatus the {@link HttpStatus} to be processed.
     * @return a {@link Map} containing status, series, and reason phrase.
     */
    default Map<String, Object> generateHttpStatusInfo(HttpStatus httpStatus) {
        return Map.of(
                STATUS.toString(), httpStatus.value(),
                SERIES.toString(), httpStatus.series(),
                REASON.toString(), httpStatus.getReasonPhrase()
        );
    }
}