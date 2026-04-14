package elya;

import org.springframework.http.HttpStatus;

import java.util.Map;

import static elya.constants.enums.StatusInfo.*;

public interface ApiEmulatorHttpStatusInfoGenerator {
    default Map<String, Object> generateHttpStatusInfo(HttpStatus httpStatus) {
        return Map.of(STATUS.toString(), httpStatus.value(),
                SERIES.toString(), httpStatus.series(),
                REASON.toString(), httpStatus.getReasonPhrase());
    }
}
