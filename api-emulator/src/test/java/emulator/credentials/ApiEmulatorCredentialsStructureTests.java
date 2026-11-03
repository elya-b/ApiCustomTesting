package emulator.credentials;

import elya.emulator.credentials.*;
import elya.emulator.credentials.ApiEmulatorCredentialsStructure.*;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link ApiEmulatorCredentialsStructure}.
 * <ul>
 *   <li>Keeps the password out of {@code toString()} of a single credential</li>
 *   <li>Keeps the passwords out of {@code toString()} of the whole users list</li>
 * </ul>
 */
class ApiEmulatorCredentialsStructureTests {

    private static final String PASSWORD = "s3cr3t-Pa55";

    @Test
    @DisplayName("Should leave the password out of the credential's toString")
    void shouldExcludePasswordFromCredentialToString() {
        Credential credential = credential("qa", PASSWORD, Role.QA);

        assertThat(credential.toString())
                .contains("login=qa", "role=QA")
                .doesNotContain(PASSWORD);
    }

    @Test
    @DisplayName("Should leave passwords out of the users list's toString")
    void shouldExcludePasswordsFromStructureToString() {
        ApiEmulatorCredentialsStructure structure = new ApiEmulatorCredentialsStructure();
        structure.setUsers(List.of(credential("admin", PASSWORD, Role.ADMIN)));

        assertThat(structure.toString())
                .contains("login=admin")
                .doesNotContain(PASSWORD);
    }

    private static Credential credential(String login, String password, Role role) {
        Credential credential = new Credential();
        credential.setLogin(login);
        credential.setPassword(password);
        credential.setRole(role);
        return credential;
    }
}