package restclient.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.*;
import elya.restclient.exceptions.*;
import elya.restclient.http.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.*;

/**
 * Unit tests for {@link RestClientApiEngine} using an embedded JDK {@link com.sun.net.httpserver.HttpServer}.
 * <ul>
 *   <li>{@code get()} — returns a JsonNode for a successful response (200)</li>
 *   <li>{@code get()} — throws ApiHttpStatusException with status 500 for a server error</li>
 *   <li>{@code get()} — keeps a non-standard status code (520) in ApiHttpStatusException</li>
 *   <li>{@code get()} — keeps the real status and body when an error body is not JSON (502 HTML)</li>
 *   <li>{@code get()} — throws RestClientException with the parse error when a 200 body is not JSON</li>
 *   <li>{@code get()} — returns an empty ObjectNode when the response body is empty</li>
 *   <li>{@code get()} — forwards custom headers to the server</li>
 *   <li>{@code get()} — throws ApiTransportException when the host is unreachable</li>
 *   <li>{@code post()} — sends the body and returns the echoed JSON response</li>
 *   <li>{@code post()} — does not throw for a null body</li>
 *   <li>{@code post()} — throws ApiHttpStatusException with status 500 when the server returns 500</li>
 *   <li>{@code delete(url)} — does not throw for a 200 response</li>
 *   <li>{@code delete(url)} — throws ApiHttpStatusException with status 404 for a missing resource</li>
 *   <li>{@code delete(url, headers)} — does not throw when using the headers overload</li>
 *   <li>{@code delete(url)} — throws ApiTransportException when the host is unreachable</li>
 *   <li>{@code sendRequest()} — returns the HTTP status code</li>
 *   <li>{@code sendRequest()} — returns the response headers</li>
 *   <li>{@code sendRequest()} — returns the raw body</li>
 *   <li>{@code sendRequest()} — returns a body that is not JSON as is, without parsing it</li>
 *   <li>{@code sendRequest()} — isSuccessful() reflects the status code (200=true, 404/500=false)</li>
 *   <li>{@code sendRequest()} — does not throw for standard HTTP methods GET, POST, DELETE (parameterized)</li>
 *   <li>{@code setBaseUrl()} — subsequent requests use the updated base URL</li>
 *   <li>{@code get()} — works with an absolute URL, ignoring the configured baseUrl</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RestClientApiEngineTests {

    /** A real-world code outside Spring's HttpStatus enum (e.g. returned by Cloudflare). */
    private static final int NON_STANDARD_STATUS = 520;

    /** A port nothing listens on, so every request to it fails before a response arrives. */
    private static final String UNREACHABLE_URL = "http://localhost:1";

    private static final String PLAIN_TEXT_BODY = "plain text";

    private HttpServer server;
    private RestClientApiEngine engine;
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newFixedThreadPool(2));

        // GET /ok — 200 with JSON body
        server.createContext("/ok", ex -> {
            byte[] body = "{\"result\":\"success\"}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set(CONTENT_TYPE, "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });

        // POST /echo — 201, echoes request body back
        server.createContext("/echo", ex -> {
            byte[] reqBody = ex.getRequestBody().readAllBytes();
            ex.getResponseHeaders().set(CONTENT_TYPE, "application/json");
            ex.sendResponseHeaders(201, reqBody.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(reqBody); }
        });

        // DELETE /delete-ok — 200
        server.createContext("/delete-ok", ex -> {
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set(CONTENT_TYPE, "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });

        // DELETE /delete-404 — 404
        server.createContext("/delete-404", ex -> {
            byte[] body = "{\"error\":\"not found\"}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set(CONTENT_TYPE, "application/json");
            ex.sendResponseHeaders(404, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });

        // GET /empty — 200 with no body
        server.createContext("/empty", ex -> {
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().close();
        });

        // GET /server-error — 500
        server.createContext("/server-error", ex -> {
            byte[] body = "{\"error\":\"internal\"}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set(CONTENT_TYPE, "application/json");
            ex.sendResponseHeaders(500, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });

        // GET /non-standard — 520, a status code that Spring's HttpStatus enum does not know
        server.createContext("/non-standard", ex -> {
            byte[] body = "{\"error\":\"unknown\"}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set(CONTENT_TYPE, "application/json");
            ex.sendResponseHeaders(NON_STANDARD_STATUS, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });

        // GET /html-error — 502 with an HTML page, as a proxy in front of a server returns it
        server.createContext("/html-error", ex -> {
            byte[] body = "<html>Bad Gateway</html>".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set(CONTENT_TYPE, "text/html");
            ex.sendResponseHeaders(502, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });

        // GET /not-json — 200 with a plain text body
        server.createContext("/not-json", ex -> {
            byte[] body = PLAIN_TEXT_BODY.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set(CONTENT_TYPE, "text/plain");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });

        // GET /headers-check — returns the Authorization header value as JSON
        server.createContext("/headers-check", ex -> {
            String auth = ex.getRequestHeaders().getFirst(AUTHORIZATION);
            String json = "{\"received\":\"" + (auth != null ? auth : "") + "\"}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set(CONTENT_TYPE, "application/json");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        });

        server.start();
        int port = server.getAddress().getPort();
        baseUrl = "http://localhost:" + port;
        engine = new RestClientApiEngine(baseUrl);
    }

    @AfterAll
    void stopServer() {
        if (server != null) server.stop(0);
    }

    // --- GET TESTS ---

    @Test
    @DisplayName("get() - Should return parsed JsonNode for 200 response")
    void get_ShouldReturnJsonNode_ForSuccessfulResponse() {
        JsonNode result = engine.get("/ok", Collections.emptyMap());

        assertNotNull(result);
        assertFalse(result.isMissingNode());
        assertEquals("success", result.get("result").asText());
    }

    @Test
    @DisplayName("get() - Should throw ApiHttpStatusException with status 500 for a server error")
    void get_ShouldThrowWithStatus500_ForErrorResponse() {
        var exception = assertThrows(ApiHttpStatusException.class,
                () -> engine.get("/server-error", Collections.emptyMap()));

        assertAll(
                () -> assertEquals(500, exception.getStatusCode(), "Status code must be 500"),
                () -> assertTrue(exception.getResponseBody().contains("internal"),
                        "Body must be preserved, got: " + exception.getResponseBody())
        );
    }

    @Test
    @DisplayName("get() - Should keep a non-standard status code in ApiHttpStatusException")
    void get_ShouldKeepNonStandardStatusCode() {
        var exception = assertThrows(ApiHttpStatusException.class,
                () -> engine.get("/non-standard", Collections.emptyMap()));

        assertAll(
                () -> assertEquals(NON_STANDARD_STATUS, exception.getStatusCode(),
                        "A status code outside Spring's HttpStatus enum must reach the caller unchanged"),
                () -> assertTrue(exception.getResponseBody().contains("unknown"),
                        "Body must be preserved, got: " + exception.getResponseBody())
        );
    }

    @Test
    @DisplayName("get() - Should keep the real status and body when an error body is not JSON")
    void get_ShouldKeepRealStatusAndBody_WhenErrorBodyIsNotJson() {
        var exception = assertThrows(ApiHttpStatusException.class,
                () -> engine.get("/html-error", Collections.emptyMap()));

        assertAll(
                () -> assertEquals(502, exception.getStatusCode(),
                        "A non-JSON error body must not turn the real status into another one"),
                () -> assertTrue(exception.getResponseBody().contains("Bad Gateway"),
                        "The raw body must be preserved, got: " + exception.getResponseBody())
        );
    }

    @Test
    @DisplayName("get() - Should throw RestClientException with the parse error when a 200 body is not JSON")
    void get_ShouldThrowRestClientException_WhenSuccessfulBodyIsNotJson() {
        var exception = assertThrows(RestClientException.class,
                () -> engine.get("/not-json", Collections.emptyMap()));

        assertAll(
                () -> assertFalse(exception instanceof ApiHttpStatusException,
                        "A 200 response is not an HTTP status failure"),
                () -> assertInstanceOf(JsonProcessingException.class, exception.getCause(),
                        "The parse error must be kept as the cause")
        );
    }

    @Test
    @DisplayName("get() - Should return empty ObjectNode when body is empty")
    void get_ShouldReturnEmptyObjectNode_WhenBodyIsEmpty() {
        JsonNode result = engine.get("/empty", Collections.emptyMap());

        assertNotNull(result);
        assertTrue(result.isObject());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("get() - Should forward custom headers to the server")
    void get_ShouldForwardCustomHeaders() {
        String expectedToken = "Bearer test-token-123";
        JsonNode result = engine.get("/headers-check", Map.of(AUTHORIZATION, expectedToken));

        assertNotNull(result);
        assertEquals(expectedToken, result.get("received").asText(),
                "Server must receive the forwarded Authorization header");
    }

    @Test
    @DisplayName("get() - Should throw ApiTransportException when host is unreachable")
    void get_ShouldThrowTransportException_WhenHostIsUnreachable() {
        RestClientApiEngine badEngine = new RestClientApiEngine(UNREACHABLE_URL);

        var exception = assertThrows(ApiTransportException.class,
                () -> badEngine.get("/unreachable", Collections.emptyMap()));

        assertNotNull(exception.getCause(), "The network failure must be kept as the cause");
    }

    // --- POST TESTS ---

    @Test
    @DisplayName("post() - Should send body and return echoed JSON response")
    void post_ShouldSendBodyAndReturnResponse() throws Exception {
        JsonNode body = objectMapper.readTree("{\"key\":\"value\"}");

        JsonNode result = engine.post("/echo", body, Collections.emptyMap());

        assertNotNull(result);
        assertEquals("value", result.get("key").asText(),
                "Echoed response must contain the sent body");
    }

    @Test
    @DisplayName("post() - Should handle null body without throwing")
    void post_ShouldHandleNullBody() {
        assertDoesNotThrow(() -> engine.post("/echo", null, Collections.emptyMap()));
    }

    @Test
    @DisplayName("post() - Should throw ApiHttpStatusException with status 500 on server error")
    void post_ShouldThrowWithStatus500_OnServerError() throws Exception {
        JsonNode body = objectMapper.readTree("{\"test\":true}");

        var exception = assertThrows(ApiHttpStatusException.class,
                () -> engine.post("/server-error", body, Collections.emptyMap()));

        assertEquals(500, exception.getStatusCode(), "Status code must be 500");
    }

    // --- DELETE TESTS ---

    @Test
    @DisplayName("delete(url) - Should not throw for 200 response")
    void delete_ShouldNotThrow_ForSuccessfulResponse() {
        assertDoesNotThrow(() -> engine.delete("/delete-ok"));
    }

    @Test
    @DisplayName("delete(url) - Should throw ApiHttpStatusException with 404 for missing resource")
    void delete_ShouldThrow404_ForNotFoundResponse() {
        var exception = assertThrows(ApiHttpStatusException.class, () -> engine.delete("/delete-404"));

        assertEquals(404, exception.getStatusCode(), "Status code must be 404");
    }

    @Test
    @DisplayName("delete(url, headers) - Should accept headers overload without throwing")
    void delete_WithHeaders_ShouldNotThrow() {
        assertDoesNotThrow(() ->
                engine.delete("/delete-ok", Map.of(AUTHORIZATION, "Bearer token"))
        );
    }

    @Test
    @DisplayName("delete(url) - Should throw ApiTransportException when host is unreachable")
    void delete_ShouldThrowTransportException_WhenHostIsUnreachable() {
        RestClientApiEngine badEngine = new RestClientApiEngine(UNREACHABLE_URL);

        var exception = assertThrows(ApiTransportException.class, () -> badEngine.delete("/unreachable"));

        assertNotNull(exception.getCause(), "The network failure must be kept as the cause");
    }

    // --- sendRequest() TESTS ---

    @Test
    @DisplayName("sendRequest() - Should return the HTTP status code")
    void sendRequest_ShouldReturnStatusCode() {
        var response = engine.sendRequest(HttpMethod.GET, "/ok", null, Collections.emptyMap());

        assertEquals(200, response.statusCode());
    }

    @Test
    @DisplayName("sendRequest() - Should return the response headers")
    void sendRequest_ShouldReturnResponseHeaders() {
        var response = engine.sendRequest(HttpMethod.GET, "/ok", null, Collections.emptyMap());

        assertNotNull(response.headers());
        assertFalse(response.headers().isEmpty());
    }

    @Test
    @DisplayName("sendRequest() - Should return the raw body")
    void sendRequest_ShouldReturnRawBody() {
        var response = engine.sendRequest(HttpMethod.GET, "/ok", null, Collections.emptyMap());

        assertNotNull(response.body());
        assertTrue(response.body().contains("success"));
    }

    @Test
    @DisplayName("sendRequest() - Should return a body that is not JSON as is, without parsing it")
    void sendRequest_ShouldReturnBodyAsIs_WhenBodyIsNotJson() {
        var response = engine.sendRequest(HttpMethod.GET, "/not-json", null, Collections.emptyMap());

        assertAll(
                () -> assertEquals(200, response.statusCode(), "Status code must be 200"),
                () -> assertEquals(PLAIN_TEXT_BODY, response.body(), "Body must be returned unchanged")
        );
    }

    @Test
    @DisplayName("sendRequest() - isSuccessful() reflects status code (200=true, 404/500=false)")
    void sendRequest_IsSuccessful_ShouldReflectStatusCode() {
        var ok  = engine.sendRequest(HttpMethod.GET, "/ok",           null, Collections.emptyMap());
        var err = engine.sendRequest(HttpMethod.GET, "/server-error", null, Collections.emptyMap());
        var nf  = engine.sendRequest(HttpMethod.GET, "/delete-404",   null, Collections.emptyMap());

        assertTrue(ok.isSuccessful(),   "200 must be successful");
        assertFalse(err.isSuccessful(), "500 must not be successful");
        assertFalse(nf.isSuccessful(),  "404 must not be successful");
    }

    @ParameterizedTest(name = "[{index}] method={0}")
    @ValueSource(strings = {"GET", "POST", "DELETE"})
    @DisplayName("sendRequest() - Should not throw for standard HTTP methods")
    void sendRequest_ShouldNotThrow_ForStandardMethods(String method) {
        assertDoesNotThrow(() ->
                engine.sendRequest(HttpMethod.valueOf(method), "/ok", null, Collections.emptyMap())
        );
    }

    // --- setBaseUrl() TEST ---

    @Test
    @DisplayName("setBaseUrl() - Should use updated base URL for subsequent requests")
    void setBaseUrl_ShouldAffectSubsequentRequests() {
        RestClientApiEngine flexEngine = new RestClientApiEngine(UNREACHABLE_URL);
        flexEngine.setBaseUrl(baseUrl);

        JsonNode result = flexEngine.get("/ok", Collections.emptyMap());

        assertNotNull(result);
        assertEquals("success", result.get("result").asText(),
                "After setBaseUrl(), engine must use the new URL");
    }

    // --- ABSOLUTE URL TEST ---

    @Test
    @DisplayName("get() - Should work with absolute URL, ignoring baseUrl")
    void get_ShouldWorkWithAbsoluteUrl() {
        RestClientApiEngine anyEngine = new RestClientApiEngine(UNREACHABLE_URL);

        JsonNode result = anyEngine.get(baseUrl + "/ok", Collections.emptyMap());

        assertNotNull(result);
        assertEquals("success", result.get("result").asText(),
                "Absolute URL must bypass the configured baseUrl");
    }
}