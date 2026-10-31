package elya.emulator.services;

import elya.apicontracts.*;
import elya.dto.bankcard.*;
import elya.emulator.repository.*;
import jakarta.annotation.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.*;

import java.util.*;

import static elya.emulator.constants.logs.EmulatorInfoLogs.*;
import static elya.emulator.logging.SensitiveDataMasker.*;

/**
 * Service for managing bank card mock data and providing it to the emulator.
 * <p>Handles seeding, retrieval and granular deletion of mocked bank cards, persisted via
 * {@link MockRepository}. Authentication is not its concern: every token it receives has
 * already been validated by {@link elya.emulator.web.auth.SessionTokenInterceptor}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockService implements IBankCardApi, IMockControlApi {

    private final MockRepository mockRepository;
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
     * <p>The append goes through {@link MockRepository#update}, so the next {@code cardId}
     * is computed from the value actually being replaced: two concurrent requests cannot
     * hand out the same ID or overwrite each other's cards.</p>
     *
     * @param token   the authenticated session token
     * @param request the {@link BankCardListRequest} containing new cards to add;
     *                {@code null}, or a request whose card list is {@code null},
     *                resets the mock to empty
     * @return        the updated {@link BankCardListResponse} containing all currently mocked cards
     */
    @Override
    public BankCardListResponse setMockResponse(String token, BankCardListRequest request) {
        log.info(PROCESSING_MOCK_UPDATE_REQUEST, maskToken(token));

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
     * Removes the entire mock response stored for the session.
     *
     * @param token the session token.
     * @return {@code true} if a mock response existed and was removed,
     *         {@code false} if there was nothing to clear.
     */
    @Override
    public boolean clearMockResponse(String token) {
        log.info(CLEARING_MOCK_RESPONSE_FOR_TOKEN, maskToken(token));
        return mockRepository.clear(token);
    }

    /**
     * Removes a specific card from the mocked response by its ID.
     * <p>Runs through {@link MockRepository#update}: the response is rebuilt from a
     * copy of the current list, so {@code size} always matches the cards, and a
     * concurrent writer cannot be overwritten. Nothing is written when the card
     * or the whole mock is absent.</p>
     *
     * @param token  the authenticated session token
     * @param cardId unique identifier of the card to be removed
     * @return       an {@link Optional} containing the deleted card's ID if found,
     * otherwise an empty Optional
     */
    @Override
    public Optional<Long> deleteApiBankCardById(String token, Long cardId) {
        log.info(ATTEMPTING_TO_DELETE_CARD_ID_FOR_TOKEN, cardId, maskToken(token));

        Optional<BankCardListResponse> written = mockRepository.update(token, current ->
                current.flatMap(response -> {
                    List<BankCardResponse> remaining = new ArrayList<>(response.getResponse().getCards());
                    if (!remaining.removeIf(card -> card.getCardId().equals(cardId))) {
                        return Optional.<BankCardListResponse>empty();
                    }
                    return Optional.of(BankCardListResponse.of(remaining));
                }));

        if (written.isEmpty()) {
            return Optional.empty();
        }
        log.info(CARD_WITH_ID_DELETED_SUCCESSFULLY, cardId);
        return Optional.of(cardId);
    }

    /**
     * Retrieves all mocked bank cards for the current session.
     * <p>If no mock is found for the token, an empty response structure is returned.</p>
     *
     * @param token the authenticated session token
     * @return      a {@link BankCardListResponse} containing the cards (may be empty)
     */
    @Override
    public BankCardListResponse getApiBankCards(String token) {
        return mockRepository.find(token).orElse(EMPTY_RESPONSE);
    }

    /**
     * Retrieves a specific mocked bank card by its ID from the current session.
     *
     * @param token  the authenticated session token
     * @param cardId unique identifier of the card
     * @return       an {@link Optional} containing the {@link BankCardResponse} if found,
     * otherwise empty
     */
    @Override
    public Optional<BankCardResponse> getApiBankCardById(String token, Long cardId) {
        log.info(SEARCHING_FOR_CARD_ID_FOR_TOKEN, cardId, maskToken(token));
        return getApiBankCards(token).getResponse().getCards()
                .stream()
                .filter(card -> card.getCardId().equals(cardId))
                .findFirst();
    }
}