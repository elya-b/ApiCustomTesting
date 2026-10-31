package elya.emulator.config;

/**
 * How long the emulator keeps its stored data (sessions and mocks).
 */
public enum StorageRetention {

    /**
     * Data lives only while the emulator runs: leftovers of a previous, possibly crashed, run
     * are discarded on startup, and the storage file is deleted on shutdown. The safe default.
     */
    PROCESS(false),

    /**
     * Data survives restarts: it is loaded on startup and the file is kept on shutdown.
     * <p>Session tokens are then kept on disk in plain text, so enable it only where that is
     * acceptable, e.g. for manual work with the emulator.</p>
     */
    PERSISTENT(true);

    private final boolean keepsDataAcrossRestarts;

    StorageRetention(boolean keepsDataAcrossRestarts) {
        this.keepsDataAcrossRestarts = keepsDataAcrossRestarts;
    }

    /**
     * Tells whether stored data must survive an emulator restart.
     *
     * @return {@code true} for {@link #PERSISTENT}, {@code false} for {@link #PROCESS}
     */
    public boolean keepsDataAcrossRestarts() {
        return keepsDataAcrossRestarts;
    }
}