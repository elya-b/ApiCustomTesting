package emulator.integration;

import elya.dto.bankcard.*;
import elya.emulator.config.*;
import elya.emulator.objects.*;
import elya.emulator.repository.*;
import elya.emulator.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;
import org.springframework.boot.context.properties.*;
import org.springframework.boot.test.context.runner.*;
import org.springframework.context.annotation.*;

import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the emulator storage in a Spring context.
 * <p>Each test runs its own lightweight context ({@link ApplicationContextRunner}) with only the
 * storage settings and beans: no web layer and no unrelated settings such as credentials.
 * Closing a context runs {@code @PreDestroy}; running the same configuration again simulates a restart.</p>
 * <ul>
 *   <li>Binding — defaults: historical file names and {@code PROCESS} retention</li>
 *   <li>Binding — configured paths and retention are bound</li>
 *   <li>Binding — an invalid retention fails the context at startup</li>
 *   <li>Lifecycle — PERSISTENT: data saved before a restart is restored after it</li>
 *   <li>Lifecycle — PROCESS: storage files are deleted when the context closes</li>
 *   <li>Lifecycle — PROCESS: files left by a crashed run are discarded on startup</li>
 * </ul>
 */
public class StorageIntegrationTests {

    private static final String TOKEN = "token";
    private static final TokenRecord SESSION = new TokenRecord("admin_user", Long.MAX_VALUE);
    private static final BankCardListResponse MOCK = BankCardListResponse.of(List.of());
    private static final String CUSTOM_SESSIONS_PATH = "target/custom/session.json";
    private static final String CUSTOM_MOCKS_PATH = "target/custom/mocks.json";

    /**
     * Binds only {@link EmulatorStorageProperties}: storage needs no credentials or token settings,
     * so the tests do not load {@link EmulatorConfig} and its unrelated required properties.
     */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(EmulatorStorageProperties.class)
    static class StoragePropertiesConfig {
    }

    @Nested
    @DisplayName("Binding of api.emulator.storage.*")
    class Binding {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(StoragePropertiesConfig.class);

        @Test
        @DisplayName("Defaults - Should use the historical file names and PROCESS retention")
        void defaults_ShouldUseHistoricalPathsAndProcessRetention() {
            contextRunner.run(context -> {
                EmulatorStorageProperties properties = context.getBean(EmulatorStorageProperties.class);

                assertAll("Storage defaults",
                        () -> assertEquals(new EmulatorStorageProperties.Store("session.json", StorageRetention.PROCESS),
                                properties.sessions(), "Sessions defaults"),
                        () -> assertEquals(new EmulatorStorageProperties.Store("mock_response.json", StorageRetention.PROCESS),
                                properties.mocks(), "Mocks defaults"));
            });
        }

        @Test
        @DisplayName("Overrides - Should bind the configured paths and retention")
        void overrides_ShouldBindConfiguredPathsAndRetention() {
            contextRunner
                    .withPropertyValues(
                            "api.emulator.storage.sessions.path=" + CUSTOM_SESSIONS_PATH,
                            "api.emulator.storage.sessions.retention=persistent",
                            "api.emulator.storage.mocks.path=" + CUSTOM_MOCKS_PATH,
                            "api.emulator.storage.mocks.retention=persistent"
                    )
                    .run(context -> {
                        EmulatorStorageProperties properties = context.getBean(EmulatorStorageProperties.class);

                        assertAll("Configured storage settings",
                                () -> assertEquals(new EmulatorStorageProperties.Store(CUSTOM_SESSIONS_PATH, StorageRetention.PERSISTENT),
                                        properties.sessions(), "Sessions settings"),
                                () -> assertEquals(new EmulatorStorageProperties.Store(CUSTOM_MOCKS_PATH, StorageRetention.PERSISTENT),
                                        properties.mocks(), "Mocks settings"));
                    });
        }

        @Test
        @DisplayName("Invalid retention - Should fail the context at startup")
        void invalidRetention_ShouldFailContext() {
            contextRunner
                    .withPropertyValues("api.emulator.storage.sessions.retention=forever")
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Nested
    @DisplayName("Lifecycle of the stores")
    class Lifecycle {

        @TempDir
        Path tempDir;

        private Path sessionsFile;
        private Path mocksFile;

        @BeforeEach
        void setUp() {
            sessionsFile = tempDir.resolve("session.json");
            mocksFile = tempDir.resolve("mock_response.json");
        }

        @Test
        @DisplayName("PERSISTENT - Should restore the data after a restart")
        void persistent_ShouldRestoreDataAfterRestart() {
            ApplicationContextRunner emulator = storageContext(StorageRetention.PERSISTENT);

            emulator.run(context -> {
                context.getBean(SessionRepository.class).save(TOKEN, SESSION);
                context.getBean(MockRepository.class).save(TOKEN, MOCK);
            });

            emulator.run(context -> assertAll("Data must survive the restart",
                    () -> assertTrue(context.getBean(SessionRepository.class).find(TOKEN).isPresent(),
                            "Session must be restored"),
                    () -> assertTrue(context.getBean(MockRepository.class).find(TOKEN).isPresent(),
                            "Mock must be restored")));
        }

        @Test
        @DisplayName("PROCESS - Should delete the storage files on shutdown")
        void process_ShouldDeleteFilesOnShutdown() {
            storageContext(StorageRetention.PROCESS).run(context -> {
                context.getBean(SessionRepository.class).save(TOKEN, SESSION);
                context.getBean(MockRepository.class).save(TOKEN, MOCK);

                assertAll("Files must exist while the emulator runs",
                        () -> assertTrue(Files.exists(sessionsFile), "Session file must exist"),
                        () -> assertTrue(Files.exists(mocksFile), "Mock file must exist"));
            });

            assertAll("No data may remain on disk after shutdown",
                    () -> assertFalse(Files.exists(sessionsFile), "Session file must be deleted"),
                    () -> assertFalse(Files.exists(mocksFile), "Mock file must be deleted"));
        }

        @Test
        @DisplayName("PROCESS - Should discard the files left by a crashed run")
        void process_ShouldDiscardLeftoversOnStartup() {
            new JsonSnapshotStore<>(sessionsFile, TokenRecord.class, StorageRetention.PERSISTENT)
                    .save(Map.of(TOKEN, SESSION));
            new JsonSnapshotStore<>(mocksFile, BankCardListResponse.class, StorageRetention.PERSISTENT)
                    .save(Map.of(TOKEN, MOCK));

            storageContext(StorageRetention.PROCESS).run(context -> assertAll("A new run must start clean",
                    () -> assertTrue(context.getBean(SessionRepository.class).find(TOKEN).isEmpty(),
                            "Leftover session must not be restored"),
                    () -> assertTrue(context.getBean(MockRepository.class).find(TOKEN).isEmpty(),
                            "Leftover mock must not be restored"),
                    () -> assertFalse(Files.exists(sessionsFile), "Leftover session file must be deleted"),
                    () -> assertFalse(Files.exists(mocksFile), "Leftover mock file must be deleted")));
        }

        private ApplicationContextRunner storageContext(StorageRetention retention) {
            return new ApplicationContextRunner()
                    .withUserConfiguration(StoragePropertiesConfig.class, StorageConfig.class)
                    .withBean(SessionRepository.class)
                    .withBean(MockRepository.class)
                    .withPropertyValues(
                            "api.emulator.storage.sessions.path=" + sessionsFile,
                            "api.emulator.storage.sessions.retention=" + retention,
                            "api.emulator.storage.mocks.path=" + mocksFile,
                            "api.emulator.storage.mocks.retention=" + retention
                    );
        }
    }
}