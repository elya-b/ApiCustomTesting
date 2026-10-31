package elya.emulator.storage;

import elya.dto.bankcard.*;
import elya.emulator.config.*;
import elya.emulator.objects.*;
import org.springframework.context.annotation.*;

import java.nio.file.*;

/**
 * Creates the JSON stores of the repositories from {@link EmulatorStorageProperties}.
 * <p>Repositories receive their store by constructor, so where and how long the data is kept
 * is decided here, in one place, and the repositories do not depend on it.</p>
 */
@Configuration
public class StorageConfig {

    /**
     * Store of the active sessions, configured by {@code api.emulator.storage.sessions.*}.
     *
     * @param properties the bound storage settings
     * @return the session store
     */
    @Bean
    public JsonSnapshotStore<TokenRecord> sessionStore(EmulatorStorageProperties properties) {
        return store(properties.sessions(), TokenRecord.class);
    }

    /**
     * Store of the mock responses, configured by {@code api.emulator.storage.mocks.*}.
     *
     * @param properties the bound storage settings
     * @return the mock store
     */
    @Bean
    public JsonSnapshotStore<BankCardListResponse> mockStore(EmulatorStorageProperties properties) {
        return store(properties.mocks(), BankCardListResponse.class);
    }

    private static <V> JsonSnapshotStore<V> store(EmulatorStorageProperties.Store settings, Class<V> valueType) {
        return new JsonSnapshotStore<>(Path.of(settings.path()), valueType, settings.retention());
    }
}