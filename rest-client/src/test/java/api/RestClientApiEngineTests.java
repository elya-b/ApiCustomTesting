package api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import elya.ApiEmulatorHttpStatusInfoGenerator;
import elya.api.RestClientApiEngine;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

/**
 * Unit tests for {@link elya.api.RestClientApiEngine} using an embedded JDK {@link com.sun.net.httpserver.HttpServer}.
 * <ul>
 *   <li>{@code get()} — returns a JsonNode for a successful response (200)</li>
 *   <li>{@code get()} — returns an empty ObjectNode for a server error (500)</li>
 *   <li>{@code get()} — returns an empty ObjectNode when the response body is empty</li>
 *   <li>{@code get()} — forwards custom headers to the server</li>
 *   <li>{@code get()} — returns an empty ObjectNode when the host is unreachable</li>
 *   <li>{@code post()} — sends the body and returns the echoed JSON response</li>
 *   <li>{@code post()} — does not throw for a null body</li>
 *   <li>{@code post()} — returns an empty ObjectNode when the server returns 500</li>
 *   <li>{@code delete(url)} — returns true for a 200 response</li>
 *   <li>{@code delete(url)} — returns false for a 404 response</li>
 *   <li>{@code delete(url, headers)} — does not throw when using the headers overload</li>
 *   <li>{@code delete(url)} — returns false when the host is unreachable</li>
 *   <li>{@code sendRequest()} — populates the statuses map with the HTTP status code</li>
 *   <li>{@code sendRequest()} — populates the response headers</li>
 *   <li>{@code sendRequest()} — populates responseAsString with the raw body</li>
 *   <li>{@code sendRequest()} — populates responseAsJson with the parsed tree</li>
 *   <li>{@code sendRequest()} — isSuccessful() reflects the status code (200=true, 404/500=false)</li>
 *   <li>{@code sendRequest()} — does not throw for standard HTTP methods GET, POST, DELETE (parameterized)</li>
 *   <li>{@code setBaseUrl()} — subsequent requests use the updated base URL</li>
 *   <li>{@code get()} — works with an absolute URL, ignoring the configured baseUrl</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RestClientApiEngineTests {

    private HttpServer server;
    private RestClientApiEngine engine;
    private String baseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiEmulatorHttpStatusInfoGenerator statusGenerator =
            new ApiEmulatorHttpStatusInfoGenerator() {};

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
        engine = new RestClientApiEngine(baseUrl, statusGenerator);
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
    @DisplayName("get() - Should return empty ObjectNode for 500 error response")
    void get_ShouldReturnEmptyNode_ForErrorResponse() {
        JsonNode result = engine.get("/server-error", Collections.emptyMap());

        assertNotNull(result);
        assertTrue(result.isObject());
        assertTrue(result.isEmpty(), "Must return empty node for non-2xx status");
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
    @DisplayName("get() - Should return empty ObjectNode when host is unreachable")
    void get_ShouldReturnEmptyNode_WhenHostIsUnreachable() {
        RestClientApiEngine badEngine = new RestClientApiEngine("http://localhost:1", statusGenerator);

        JsonNode result = badEngine.get("/unreachable", Collections.emptyMap());

        assertNotNull(result, "Must return a non-null node even when connection fails");
        assertTrue(result.isObject());
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
    @DisplayName("post() - Should return empty ObjectNode when server returns 500")
    void post_ShouldReturnEmptyNode_OnServerError() throws Exception {
        JsonNode body = objectMapper.readTree("{\"test\":true}");

        JsonNode result = engine.post("/server-error", body, Collections.emptyMap());

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Must return empty node for non-2xx status");
    }

    // --- DELETE TESTS ---

    @Test
    @DisplayName("delete(url) - Should return true for 200 response")
    void delete_ShouldReturnTrue_ForSuccessfulResponse() {
        assertTrue(engine.delete("/delete-ok"));
    }

    @Test
    @DisplayName("delete(url) - Should return false for 404 response")
    void delete_ShouldReturnFalse_ForNotFoundResponse() {
        assertFalse(engine.delete("/delete-404"));
    }

    @Test
    @DisplayName("delete(url, headers) - Should accept headers overload without throwing")
    void delete_WithHeaders_ShouldNotThrow() {
        assertDoesNotThrow(() ->
                engine.delete("/delete-ok", Map.of(AUTHORIZATION, "Bearer token"))
        );
    }

    @Test
    @DisplayName("delete(url) - Should return false when host is unreachable")
    void delete_ShouldReturnFalse_WhenHostIsUnreachable() {
        RestClientApiEngine badEngine = new RestClientApiEngine("http://localhost:1", statusGenerator);

        assertFalse(badEngine.delete("/unreachable"),
                "delete() must return false when connection fails");
    }

    // --- sendRequest() TESTS ---

    @Test
    @DisplayName("sendRequest() - Should populate statuses map with HTTP status code")
    void sendRequest_ShouldPopulateStatuses() {
        var response = engine.sendRequest(HttpMethod.GET, "/ok", null, Collections.emptyMap());

        assertNotNull(response.getStatuses());
        assertEquals(200, response.getStatuses().get("status"));
    }

    @Test
    @DisplayName("sendRequest() - Should populate response headers")
    void sendRequest_ShouldPopulateResponseHeaders() {
        var response = engine.sendRequest(HttpMethod.GET, "/ok", null, Collections.emptyMap());

        assertNotNull(response.getHeaders());
        assertFalse(response.getHeaders().isEmpty());
    }

    @Test
    @DisplayName("sendRequest() - Should populate responseAsString with raw body")
    void sendRequest_ShouldPopulateResponseAsString() {
        var response = engine.sendRequest(HttpMethod.GET, "/ok", null, Collections.emptyMap());

        assertNotNull(response.getResponseAsString());
        assertTrue(response.getResponseAsString().contains("success"));
    }

    @Test
    @DisplayName("sendRequest() - Should populate responseAsJson with parsed tree")
    void sendRequest_ShouldPopulateResponseAsJson() {
        var response = engine.sendRequest(HttpMethod.GET, "/ok", null, Collections.emptyMap());

        assertNotNull(response.getResponseAsJson());
        assertFalse(response.getResponseAsJson().isEmpty());
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
        RestClientApiEngine flexEngine = new RestClientApiEngine("http://localhost:1", statusGenerator);
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
        RestClientApiEngine anyEngine = new RestClientApiEngine("http://localhost:1", statusGenerator);

        JsonNode result = anyEngine.get(baseUrl + "/ok", Collections.emptyMap());

        assertNotNull(result);
        assertEquals("success", result.get("result").asText(),
                "Absolute URL must bypass the configured baseUrl");
    }
}