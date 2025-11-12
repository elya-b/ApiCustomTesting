package elya.interfaces;

import org.springframework.http.HttpStatus;

import java.util.Map;

import static elya.enums.StatusInfo.*;

public interface ApiEmulatorStatusInfoGenerator {

    default Map<String, String> generateHttpStatusInfo(HttpStatus httpStatus) {
        return Map.of(STATUS.getStatusInfo(), String.valueOf(httpStatus.value()),
                SERIES.getStatusInfo(), httpStatus.series().name(),
                REASON.getStatusInfo(), httpStatus.getReasonPhrase());
    }
}
