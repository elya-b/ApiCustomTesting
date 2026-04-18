package elya.services;

import elya.ApiEmulatorHttpStatusInfoGenerator;
import elya.apicontracts.IBankCardApi;
import elya.apicontracts.IMockControlApi;
import elya.card.BankCard;
import elya.dto.bankcard.BankCardListRequest;
import elya.dto.bankcard.BankCardListResponse;
import elya.dto.bankcard.BankCardResponse;
import elya.repository.MockRepository;
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
     * Updates the persistent mock response by appending new cards to the existing list.
     * <p>The service automatically calculates the next available {@code cardId}
     * based on the current maximum ID in the user's session to prevent collisions.</p>
     *
     * @param token   valid session token for identification
     * @param request the {@link BankCardListRequest} containing new cards to add
     * @return        the updated {@link BankCardListResponse} containing all currently mocked cards
     */
    @Override
    public BankCardListResponse setMockResponse(String token, BankCardListRequest request) {
        log.info(PROCESSING_MOCK_UPDATE_REQUEST, token);

        if (request == null || request.getCards() == null) {
            mockRepository.save(token, EMPTY_RESPONSE);
            return EMPTY_RESPONSE;
        }

        BankCardListResponse currentWrapper = getApiBankCards(token);
        List<BankCardResponse> allCardsList = new ArrayList<>(currentWrapper.getResponse().getCards());

        long nextId = allCardsList.stream()
                .mapToLong(BankCardResponse::getCardId)
                .max()
                .orElse(0L) + 1;

        for (var req : request.getCards()) {
            BankCard domain = req.toDomain();
            allCardsList.add(BankCardResponse.fromDomain(domain, nextId++));
        }

        BankCardListResponse finalResponse = BankCardListResponse.of(allCardsList);
        mockRepository.save(token, finalResponse);
        return finalResponse;
    }

    /**
     * Removes the entire custom mock response associated with the token.
     * * @param token unique session identifier
     * @return      true if the record was found and successfully cleared from storage
     */
    @Override
    public boolean clearMockResponse(String token) {
        log.info(CLEARING_MOCK_RESPONSE_FOR_TOKEN, token);
        return mockRepository.clear(token);
    }

    /**
     * Removes a specific card from the mocked response by its ID.
     * <p>If the card is found, it is removed from the list, and the updated state
     * is synchronized with the {@link MockRepository}.</p>
     *
     * @param token  Authorization session token
     * @param cardId unique identifier of the card to be removed
     * @return       an {@link Optional} containing the deleted card's ID if found,
     * otherwise an empty Optional
     */
    @Override
    public Optional<Long> deleteApiBankCardById(String token, Long cardId) {
        log.info(ATTEMPTING_TO_DELETE_CARD_ID_FOR_TOKEN, cardId, token);

        BankCardListResponse currentCards = getApiBankCards(token);
        boolean removed = currentCards.getResponse().getCards()
                .removeIf(card -> card.getCardId().equals(cardId));

        if (removed) {
            mockRepository.save(token, currentCards);

            log.info(CARD_WITH_ID_DELETED_SUCCESSFULLY, cardId);
            return Optional.of(cardId);
        }

        log.warn(CARD_NOT_FOUND_BY_ID_FOR_DELETION, cardId);
        return Optional.empty();
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