package elya.emulator.exceptions;

import org.springframework.http.*;

import static elya.emulator.constants.messages.ExceptionMessages.*;

/**
 * Exception thrown when the session mock holds no card with the requested ID.
 * <p>Maps to {@code 404 Not Found}; the message names the missing card ID.</p>
 */
public class CardNotFoundException extends ApiEmulatorException {

    /**
     * Constructs the exception for the given card ID.
     *
     * @param cardId the ID of the card that was not found
     */
    public CardNotFoundException(Long cardId) {
        super(CARD_NOT_FOUND.formatted(cardId), HttpStatus.NOT_FOUND);
    }
}