package elya.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
    ADMIN("ADMIN"),
    QA("QA"),
    USER("USER");

    private final String value;

    public static Role fromString(String role) {
        for (Role r : Role.values()) {
            if (r.value.equalsIgnoreCase(role)) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + role);
    }
}
