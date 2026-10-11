package elya.emulator.tokens;

/**
 * Strategy interface for generating security tokens.
 * <p>Defines a contract for components responsible for creating session
 * identifiers based on user credentials. Implementing classes can use
 * various formats, such as JWT, UUID, or opaque strings.</p>
 */
public interface TokenProvider {

    /**
     * Generates a unique security token for the given user identity.
     *
     * @param login the identifier of the user (e.g., username or system login).
     * @return a string representation of the generated token.
     */
    String generateToken(String login);
}