package elya;

import org.springframework.http.HttpStatus;

import java.util.Map;

public interface ApiHttpStatusesGenerator {

    default Map<String, String> generateHttpStatus(HttpStatus httpStatus) {
        return Map.of("status", String.valueOf(httpStatus.value()),
                        "series", httpStatus.series().toString(),
                            "reason", httpStatus.getReasonPhrase());
    }
}
