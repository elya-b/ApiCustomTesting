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

import static elya.emulator.constants.logs.ApiInfoLogs.MOCK_RESPONSE_SERVICE_START;
import static elya.emulator.constants.logs.ApiInfoLogs.MOCK_RESPONSE_SERVICE_STOP;

/**
 * Service for managing bank card mock data and providing it to the emulator.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockService implements IBankCardApi, IMockControlApi, ApiEmulatorHttpStatusInfoGenerator {

    private final MockRepository mockRepository;
    private final TokenManagerService tokenManagerService;
    private static final BankCardListResponse EMPTY_RESPONSE = BankCardListResponse.of(Collections.emptyList());

    @PostConstruct
    public void init() {
        log.info(MOCK_RESPONSE_SERVICE_START);
    }

    @PreDestroy
    public void cleanup() {
        log.info(MOCK_RESPONSE_SERVICE_STOP);
    }

    /**
     * Updates the persistent mock response by appending new cards to the existing list.
     * Automatically calculates the next available cardId based on the current maximum ID.
     *
     * @param token   session token for identification
     * @param request the simplified bank card data from the user
     * @return        true if the merged mock data was successfully saved
     */
    @Override
    public BankCardListResponse setMockResponse(String token, BankCardListRequest request) {
        log.info("Processing mock update request for token: [{}]", token);

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
     * Removes the custom mock response associated with the token.
     * @param token session token
     * @return      true if the data was cleared successfully
     */
    @Override
    public boolean clearMockResponse(String token) {
        log.info("Clearing mock response for token: [{}]", token);
        return mockRepository.clear(token);
    }

    /**
     * Removes a specific card from the mocked response by its ID.
     * If the card is found and removed, the updated list is saved back to the repository.
     *
     * @param token  Authorization session token
     * @param cardId Unique identifier of the card to be removed
     * @return       true if the card was found and the list was updated, false otherwise
     */
    @Override
    public Optional<Long> deleteApiBankCardById(String token, Long cardId) {
        log.info("Attempting to delete cardId: [{}] for token: [{}]", cardId, token);

        BankCardListResponse currentCards = getApiBankCards(token);
        boolean removed = currentCards.getResponse().getCards()
                .removeIf(card -> card.getCardId().equals(cardId));

        if (removed) {
            mockRepository.save(token, currentCards);

            log.info("Card with ID [{}] successfully deleted", cardId);
            return Optional.of(cardId);
        }

        log.warn("Card with ID [{}] not found for deletion", cardId);
        return Optional.empty();
    }

    /**
     * Retrieves the mocked bank cards for the emulator API.
     * Returns an empty response structure if no mock is set.
     *
     * @param token Authorization token
     * @return      List of mocked bank cards
     */
    @Override
    public BankCardListResponse getApiBankCards(String token) {
        tokenManagerService.validateAuthToken(token);
        return mockRepository.find(token).orElse(EMPTY_RESPONSE);
    }

    /**
     * Retrieves a specific mocked bank card by its ID for the emulator API.
     *
     * @param token  Authorization token
     * @param cardId Unique identifier of the card
     * @return       An Optional containing the BankCardResponse if found, or empty otherwise
     */
    @Override
    public Optional<BankCardResponse> getApiBankCardById(String token, Long cardId) {
        log.info("Searching for cardId: [{}] for token: [{}]", cardId, token);
        return getApiBankCards(token).getResponse().getCards()
                .stream()
                .filter(card -> card.getCardId().equals(cardId))
                .findFirst();
    }
}
