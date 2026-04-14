package elya.emulator.interfaces;

import org.springframework.http.HttpStatus;

import java.util.Map;

import static elya.general.enums.StatusInfo.*;

public interface ApiEmulatorStatusInfoGenerator {

    default Map<String, String> generateHttpStatusInfo(HttpStatus httpStatus) {
        return Map.of(STATUS.toString(), String.valueOf(httpStatus.value()),
                SERIES.toString(), httpStatus.series().name(),
                REASON.toString(), httpStatus.getReasonPhrase());
    }
}
