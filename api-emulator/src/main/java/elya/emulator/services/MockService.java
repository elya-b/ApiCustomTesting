package elya.emulator.services;

import elya.ApiEmulatorHttpStatusInfoGenerator;
import elya.apicontracts.IBankCardApi;
import elya.apicontracts.IMockControlApi;
import elya.card.BankCard;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardResponse;
import elya.emulator.repository.MockRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static elya.emulator.constants.logs.ApiInfoLogs.*;
import static elya.emulator.constants.logs.ApiWarnLogs.*;

/**
 * Service for managing bank card mock data and providing it to the emulator.
 * Handles the logic for seeding, retrieving, and granular deletion of mocked bank cards
 * while maintaining data persistence via {@link MockRepository}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockService implements IBankCardApi, IMockControlApi, ApiEmulatorHttpStatusInfoGenerator {

    private final MockRepository mockRepository;
    private final TokenManagerService tokenManagerService;
    private static final BankCardListResponse EMPTY_RESPONSE = BankCardListResponse.of(Collections.emptyList());

    /**
     * Logs service initialization on startup.
     */
    @PostConstruct
    public void init() {
        log.info(MOCK_RESPONSE_SERVICE_START);
    }

    /**
     * Logs service destruction on shutdown.
     */
    @PreDestroy
    public void cleanup() {
        log.info(MOCK_RESPONSE_SERVICE_STOP);
    }

    /**
     * Appends new cards to the session's mock response.
     * <p>The token is validated first. The append itself goes through
     * {@link MockRepository#update}, so the next {@code cardId} is computed
     * from the value actually being replaced: two concurrent requests cannot
     * hand out the same ID or overwrite each other's cards.</p>
     *
     * @param token   valid session token for identification
     * @param request the {@link BankCardListRequest} containing new cards to add;
     *                {@code null}, or a request whose card list is {@code null},
     *                resets the mock to empty
     * @return        the updated {@link BankCardListResponse} containing all currently mocked cards
     */
    @Override
    public BankCardListResponse setMockResponse(String token, BankCardListRequest request) {
        tokenManagerService.validateAuthToken(token);
        log.info(PROCESSING_MOCK_UPDATE_REQUEST, token);

        if (request == null || request.getCards() == null) {
            BankCardListResponse empty = BankCardListResponse.of(List.of());
            mockRepository.save(token, empty);
            return empty;
        }

        return mockRepository.update(token, current -> {
            List<BankCardResponse> cards = current
                    .map(response -> new ArrayList<>(response.getResponse().getCards()))
                    .orElseGet(ArrayList::new);

            long nextId = cards.stream().mapToLong(BankCardResponse::getCardId).max().orElse(0L) + 1;
            for (var req : request.getCards()) {
                cards.add(BankCardResponse.fromDomain(req.toDomain(), nextId++));
            }
            return Optional.of(BankCardListResponse.of(cards));
        }).orElseThrow();
    }

    /**
     * Removes the entire custom mock response associated with the token.
     * * @param token unique session identifier
     * @return      true if the record was found and successfully cleared from storage
     */
    @Override
    public boolean isResponseClear(String token) {
        log.info(CLEARING_MOCK_RESPONSE_FOR_TOKEN, token);
        return mockRepository.clear(token);
    }

    /**
     * Removes a specific card from the mocked response by its ID.
     * <p>Runs through {@link MockRepository#update}: the response is rebuilt from a
     * copy of the current list, so {@code size} always matches the cards, and a
     * concurrent writer cannot be overwritten. Nothing is written when the card
     * or the whole mock is absent.</p>
     *
     * @param token  Authorization session token
     * @param cardId unique identifier of the card to be removed
     * @return       an {@link Optional} containing the deleted card's ID if found,
     * otherwise an empty Optional
     */
    @Override
    public Optional<Long> deleteApiBankCardById(String token, Long cardId) {
        tokenManagerService.validateAuthToken(token);
        log.info(ATTEMPTING_TO_DELETE_CARD_ID_FOR_TOKEN, cardId, token);

        Optional<BankCardListResponse> written = mockRepository.update(token, current ->
                current.flatMap(response -> {
                    List<BankCardResponse> remaining = new ArrayList<>(response.getResponse().getCards());
                    if (!remaining.removeIf(card -> card.getCardId().equals(cardId))) {
                        return Optional.<BankCardListResponse>empty();
                    }
                    return Optional.of(BankCardListResponse.of(remaining));
                }));

        if (written.isEmpty()) {
            log.warn(CARD_NOT_FOUND_BY_ID_FOR_DELETION, cardId);
            return Optional.empty();
        }
        log.info(CARD_WITH_ID_DELETED_SUCCESSFULLY, cardId);
        return Optional.of(cardId);
    }

    /**
     * Retrieves all mocked bank cards for the current session.
     * <p>This method triggers token validation via {@link TokenManagerService}.
     * If no mock is found for the token, an empty response structure is returned.</p>
     *
     * @param token Authorization token for session lookup
     * @return      a {@link BankCardListResponse} containing the cards (may be empty)
     */
    @Override
    public BankCardListResponse getApiBankCards(String token) {
        tokenManagerService.validateAuthToken(token);
        return mockRepository.find(token).orElse(EMPTY_RESPONSE);
    }

    /**
     * Retrieves a specific mocked bank card by its ID from the current session.
     *
     * @param token  Authorization token
     * @param cardId unique identifier of the card
     * @return       an {@link Optional} containing the {@link BankCardResponse} if found,
     * otherwise empty
     */
    @Override
    public Optional<BankCardResponse> getApiBankCardById(String token, Long cardId) {
        log.info(SEARCHING_FOR_CARD_ID_FOR_TOKEN, cardId, token);
        return getApiBankCards(token).getResponse().getCards()
                .stream()
                .filter(card -> card.getCardId().equals(cardId))
                .findFirst();
    }
}