package elya.interfaces;

/**
 * Unified interface for REST API operations.
 * <p>Acts as a comprehensive contract that aggregates read, write, and remove
 * capabilities by extending {@link IReadOnlyClient}, {@link IWriteClient},
 * and {@link IRemoveClient}.</p>
 */
public interface IRestClientApi extends IReadOnlyClient, IWriteClient, IRemoveClient {
    // This interface aggregates methods from its parents to provide a full REST suite.
}