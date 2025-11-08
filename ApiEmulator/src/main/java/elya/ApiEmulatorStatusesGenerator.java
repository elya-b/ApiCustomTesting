package elya;

import org.springframework.http.HttpStatus;

import java.util.Map;

public interface ApiEmulatorStatusesGenerator {

    default Map<String, String> generateHttpStatus(HttpStatus httpStatus) {
        return Map.of("status", String.valueOf(httpStatus.value()),
                        "series", httpStatus.series().name(),
                            "reason", httpStatus.getReasonPhrase());
    }
}
