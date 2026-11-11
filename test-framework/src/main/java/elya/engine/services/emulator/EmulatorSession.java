package elya.engine.services.emulator;

import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;
import elya.card.*;
import elya.dto.bankcard.*;
import elya.emulator.*;
import elya.restclient.api.*;
import elya.restclient.http.*;
import lombok.*;
import lombok.extern.slf4j.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static elya.constants.ApiEndpoints.*;
import static elya.constants.enums.HttpHeaderValues.*;
import static elya.engine.services.emulator.constants.logs.InfoLogs.*;
import static elya.engine.services.emulator.constants.logs.WarnLogs.*;
import static elya.enums.responsemodel.ApiBankCards.*;
import static org.springframework.http.HttpHeaders.*;

/**
 * One running emulator that belongs to one test.
 * <p>Holds everything a test needs to talk to its emulator: the base URL, the session token
 * obtained at startup and a client bound to that URL. Nothing is shared with other sessions,
 * so tests that run in parallel cannot see or change each other's emulator.</p>
 * <p>Sessions are created by {@link EmulatorLifecycleManager#start}. {@link #close()} stops the
 * emulator and removes its storage directory.</p>
 */
@Slf4j
public final class EmulatorSession implements AutoCloseable {

    /** Base URL of this session's emulator, e.g. {@code http://localhost:54321}. */
    @Getter
    private final String url;

    /** Session token obtained when the emulator started. */
    @Getter
    private final String authToken;

    /** Typed client under test, bound to this session's emulator. */
    @Getter
    private final RestClientApi clientApi;

    private final IRestClientApi restClient;
    private final ApiEmulatorRunner runner;
    private final ObjectMapper objectMapper;
    private final Path storageDirectory;

    /**
     * Creates a session for an emulator that has already started and accepted the login.
     * <p>Normally called by {@link EmulatorLifecycleManager}; tests use the manager instead.</p>
     *
     * @param url              base URL of the emulator
     * @param authToken        session token obtained at startup
     * @param restClient       low-level client bound to {@code url}
     * @param runner           runner that owns the emulator process
     * @param objectMapper     mapper for request and response bodies
     * @param storageDirectory directory that holds this emulator's storage files
     */
    public EmulatorSession(String url,
                           String authToken,
                           IRestClientApi restClient,
                           ApiEmulatorRunner runner,
                           ObjectMapper objectMapper,
                           Path storageDirectory) {
        this.url = url;
        this.authToken = authToken;
        this.restClient = restClient;
        this.runner = runner;
        this.objectMapper = objectMapper;
        this.storageDirectory = storageDirectory;
        this.clientApi = new RestClientApi(
                new AuthClient(restClient),
                new BankCardClient(restClient),
                new MockClient(restClient));
    }

    /**
     * Seeds this session's emulator with bank cards.
     * <p>Sends the request through the low-level client rather than through {@link #getClientApi()},
     * so the arrange step of a test does not depend on the client under test.</p>
     *
     * @param token session token of the user who owns the cards
     * @param cards domain cards to add
     * @return the cards as confirmed by the emulator, with their assigned IDs
     */
    public List<BankCard> addBankCards(String token, List<BankCard> cards) {
        List<BankCardRequest> requests = cards.stream()
                .map(BankCardRequest::fromDomain)
                .toList();
        Map<String, String> headers = Map.of(AUTHORIZATION, BEARER + token);

        JsonNode responseNode = restClient.post(URL_BANK_CARD_DATA,
                objectMapper.valueToTree(BankCardListRequest.of(requests)), headers);

        JsonNode cardsNode = responseNode.path(RESPONSE.toString()).path(CARDS.toString());
        List<BankCardResponse> responses = objectMapper.convertValue(cardsNode, new TypeReference<>() {});
        return responses.stream()
                .map(BankCardResponse::getBankCard)
                .toList();
    }

    /**
     * Stops the emulator and removes its storage directory.
     * <p>Safe to call more than once. A directory that cannot be removed, for example because
     * it still holds files kept with {@code persistent} retention, is reported, not thrown:
     * the result of the test does not depend on it.</p>
     */
    @Override
    public void close() {
        runner.stop();
        deleteStorageDirectory();
        log.info(SESSION_CLOSED, url);
    }

    /**
     * Removes the storage directory if it exists and is empty.
     */
    private void deleteStorageDirectory() {
        try {
            Files.deleteIfExists(storageDirectory);
        } catch (IOException e) {
            log.warn(STORAGE_DIRECTORY_NOT_DELETED, storageDirectory, e.toString());
        }
    }
}
