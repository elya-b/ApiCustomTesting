package elya.emulator.constants.messages;

import lombok.experimental.UtilityClass;

/**
 * Dictionary of standard success messages returned in API responses.
 * <p>Centralizing these strings ensures that the client receives consistent
 * feedback across different endpoints of the emulator.</p>
 */
@UtilityClass
public class ResponseMessages {

    /** Message returned to the client when a mocked response configuration is successfully removed. */
    public static final String MOCK_CLEARED = "Mock response is cleared!";
}