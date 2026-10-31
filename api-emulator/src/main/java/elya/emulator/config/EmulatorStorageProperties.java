package elya.emulator.config;

import org.springframework.boot.context.properties.*;
import org.springframework.boot.context.properties.bind.*;

import java.util.*;

/**
 * Storage settings bound from {@code api.emulator.storage.*}.
 * <p>Each store ({@code sessions}, {@code mocks}) has a JSON file path and a {@link StorageRetention}.
 * Omitted values fall back to safe defaults: the historical file name and
 * {@link StorageRetention#PROCESS}, so tokens never outlive the emulator unless explicitly asked.</p>
 *
 * @param sessions settings of the session (token) store
 * @param mocks    settings of the mock response store
 */
@ConfigurationProperties(prefix = "api.emulator.storage")
public record EmulatorStorageProperties(@DefaultValue Store sessions, @DefaultValue Store mocks) {

    /** Default file of the session store, relative to the working directory. */
    public static final String DEFAULT_SESSIONS_PATH = "session.json";

    /** Default file of the mock store, relative to the working directory. */
    public static final String DEFAULT_MOCKS_PATH = "mock_response.json";

    /**
     * Applies the per-store default file paths, which a shared nested type cannot declare itself.
     */
    public EmulatorStorageProperties {
        sessions = sessions.withDefaultPath(DEFAULT_SESSIONS_PATH);
        mocks = mocks.withDefaultPath(DEFAULT_MOCKS_PATH);
    }

    /**
     * Settings of one store.
     * <p>The path stays a {@code String} and is turned into a {@link java.nio.file.Path} by the code
     * that opens the file: Spring's String-to-Path conversion goes through resource resolution first.</p>
     *
     * @param path      the JSON file of the store; a per-store default applies when omitted or blank
     * @param retention how long the stored data lives; defaults to {@link StorageRetention#PROCESS}
     */
    public record Store(String path, StorageRetention retention) {

        /**
         * Applies the default retention.
         */
        public Store {
            retention = Objects.requireNonNullElse(retention, StorageRetention.PROCESS);
        }

        private Store withDefaultPath(String defaultPath) {
            return (path == null || path.isBlank()) ? new Store(defaultPath, retention) : this;
        }
    }
}