package elya.interfaces;
/**
 * Unified interface for REST API operations, combining read, write, and remove capabilities.
 */
public interface IRestClientApi extends IReadOnlyClient, IWriteClient, IRemoveClient {

}
