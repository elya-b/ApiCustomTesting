package elya.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration of system roles used for authorization and access control.
 * <p>Each role defines a level of authority within the emulator, allowing for
 * fine-grained permission management across different endpoints.</p>
 */
@Getter
@AllArgsConstructor
public enum Role {

    /** Administrative role with full access to all emulator features and configurations. */
    ADMIN("ADMIN"),

    /** Quality Assurance role, typically used for testing and verification purposes. */
    QA("QA"),

    /** Standard user role with limited access to basic emulator functionality. */
    USER("USER");

    /** The string representation of the role as used in configuration or JWT claims. */
    private final String value;

    /**
     * Converts a string value to its corresponding {@link Role} constant.
     * <p>The matching is case-insensitive. Throws an exception if no match is found.</p>
     *
     * @param role the string representation of the role (e.g., "admin" or "USER").
     * @return the matching {@link Role} constant.
     * @throws IllegalArgumentException if the input is null or does not match any known role.
     */
    public static Role fromString(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role value cannot be null");
        }
        for (Role r : Role.values()) {
            if (r.value.equalsIgnoreCase(role)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + role);
    }
}