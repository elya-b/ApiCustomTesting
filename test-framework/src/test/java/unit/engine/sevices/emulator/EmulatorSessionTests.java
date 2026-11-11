package unit.engine.sevices.emulator;

import com.fasterxml.jackson.databind.*;
import elya.card.*;
import elya.card.constants.Currency;
import elya.card.constants.*;
import elya.dto.bankcard.*;
import elya.emulator.*;
import elya.engine.services.emulator.*;
import elya.restclient.http.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.io.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import static elya.constants.ApiEndpoints.*;
import static elya.constants.enums.HttpHeaderValues.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.*;

/**
 * Unit tests (Mockito) for {@link EmulatorSession}.
 * <ul>
 *   <li>{@code close()} stops the emulator and deletes the empty storage directory</li>
 *   <li>{@code close()} does not fail when the storage directory still holds files</li>
 *   <li>{@code close()} does not fail when the storage directory was never created</li>
 *   <li>{@code addBankCards()} posts the cards with the Bearer token and returns the confirmed cards</li>
 *   <li>{@code getClientApi()} sends its requests through the session's own client</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EmulatorSessionTests {

    private static final String URL = "http://localhost:54321";
    private static final String TOKEN = "token";

    @TempDir
    Path storageRoot;

    @Mock
    private ApiEmulatorRunner runner;
    @Mock
    private IRestClientApi restClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("close() - Should stop the emulator and delete the storage directory")
    void close_ShouldStopEmulatorAndDeleteStorageDirectory() throws IOException {
        Path directory = Files.createDirectory(storageRoot.resolve("session"));

        session(directory).close();

        verify(runner).stop();
        assertFalse(Files.exists(directory), "The empty storage directory must be deleted");
    }

    @Test
    @DisplayName("close() - Should not fail when the storage directory still holds files")
    void close_ShouldNotFail_WhenStorageDirectoryStillHoldsFiles() throws IOException {
        Path directory = Files.createDirectory(storageRoot.resolve("session"));
        Files.writeString(directory.resolve("session.json"), "{}");

        assertDoesNotThrow(() -> session(directory).close());

        verify(runner).stop();
        assertTrue(Files.exists(directory), "A directory with files must be kept");
    }

    @Test
    @DisplayName("close() - Should not fail when the storage directory was never created")
    void close_ShouldNotFail_WhenStorageDirectoryWasNeverCreated() {
        assertDoesNotThrow(() -> session(storageRoot.resolve("missing")).close());

        verify(runner).stop();
    }

    @Test
    @DisplayName("addBankCards() - Should post the cards with the Bearer token and return the confirmed cards")
    void addBankCards_ShouldReturnConfirmedCards() {
        BankCard card = BankCard.builder(4444555566667777L, CardType.DEBIT, Currency.USD).build();
        BankCardListResponse response = BankCardListResponse.of(List.of(BankCardResponse.fromDomain(card, 1L)));
        when(restClient.post(eq(URL_BANK_CARD_DATA), any(), eq(Map.of(AUTHORIZATION, BEARER + TOKEN))))
                .thenReturn(objectMapper.valueToTree(response));

        List<BankCard> confirmed = session(storageRoot).addBankCards(TOKEN, List.of(card));

        assertAll("Verify the confirmed cards",
                () -> assertEquals(1, confirmed.size(), "One card was added"),
                () -> assertEquals(1L, confirmed.getFirst().getCardId(), "The emulator assigns the ID"),
                () -> assertEquals(card.getCardNumber(), confirmed.getFirst().getCardNumber(), "Card number mismatch"));
    }

    @Test
    @DisplayName("getClientApi() - Should send requests through the session's own client")
    void getClientApi_ShouldUseSessionClient() {
        when(restClient.get(eq(URL_BANK_CARD_DATA), anyMap()))
                .thenReturn(objectMapper.valueToTree(BankCardListResponse.of(List.of())));

        List<BankCard> cards = session(storageRoot).getClientApi().getApiBankCards(TOKEN);

        assertTrue(cards.isEmpty(), "No cards were added");
        verify(restClient).get(eq(URL_BANK_CARD_DATA), anyMap());
    }

    private EmulatorSession session(Path storageDirectory) {
        return new EmulatorSession(URL, TOKEN, restClient, runner, objectMapper, storageDirectory);
    }
}
