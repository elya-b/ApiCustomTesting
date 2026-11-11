package elya.engine.services.emulator.constants.logs;

import lombok.experimental.*;

/**
 * Static dictionary for emulator lifecycle warnings.
 * <p>Used when a cleanup step fails: the test result does not depend on it,
 * so the problem is reported instead of failing the test.</p>
 */
@UtilityClass
public class WarnLogs {

    /** Logged when a session cannot remove its storage directory. Takes the directory and the reason. */
    public static final String STORAGE_DIRECTORY_NOT_DELETED = "SESSION: storage directory [{}] was not deleted: {}";
}
