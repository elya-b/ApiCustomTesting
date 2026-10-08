package integration.auth;

import elya.allure.Priority;
import elya.allure.PriorityLevel;
import elya.authentication.Token;
import elya.constants.Role;
import elya.dto.auth.AuthRequest;
import elya.restclient.exceptions.ApiHttpStatusException;
import integration.AbstractApiTest;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static elya.constants.ApiEndpoints.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the POST /token endpoint (authentication).
 */
@Epic("Authentication API")
@Feature("POST /token — Generate Auth Token")
public class PostTokenIT extends AbstractApiTest {

    @Test
    @Story("Successful token generation")
    @Severity(SeverityLevel.BLOCKER)
    @Priority(PriorityLevel.CRITICAL)
    @DisplayName("POST " + URL_TOKEN + " - Should return 200 and token when credentials are valid")
    void POST_Token_ShouldReturnOkAndToken_WhenCredentialsAreValid() {
        AuthRequest authRequest = prepareLoginRequest(Role.QA);

        Token token = clientApi.generateAuthToken(authRequest);
        attachJson("Token response", token);

        verify("Token should be generated successfully", () -> {
            Allure.step("TTL equals 3600",          () -> assertEquals("3600", token.getTtl(), "Field TTL should be 3600"));
            Allure.step("Token string is not null", () -> assertNotNull(token.getToken(), "Token should not be null"));
        });
    }

    @Test
    @Story("Authentication failure")
    @Severity(SeverityLevel.CRITICAL)
    @Priority(PriorityLevel.HIGH)
    @DisplayName("POST " + URL_TOKEN + " - Should return 401 when credentials are invalid")
    void POST_Token_ShouldReturn401_WhenCredentialsAreInvalid() {
        AuthRequest authRequest = prepareLoginRequest("invalid_user", "invalid_pass");

        var exception = assertThrows(ApiHttpStatusException.class,
                () -> clientApi.generateAuthToken(authRequest));
        attachJson("Error response", exception.getResponseBody());

        verify("Wrong credentials must be rejected by the service with 401", () -> assertAll(
                () -> assertEquals(401, exception.getStatusCode(), "Status code must be 401"),
                () -> assertTrue(exception.getResponseBody().contains("Invalid or missing credentials"),
                        "Body must name the reason, got: " + exception.getResponseBody())
        ));
    }

    @Test
    @Story("Successful token generation")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.HIGH)
    @DisplayName("POST " + URL_TOKEN + " - Should return token with non-null issuer and expires fields")
    void POST_Token_ShouldReturnFullTokenDetails_WhenCredentialsAreValid() {
        AuthRequest authRequest = prepareLoginRequest(Role.QA);

        Token token = clientApi.generateAuthToken(authRequest);
        attachJson("Token response", token);

        verify("All token fields should be populated", () ->
                assertAll("Token field validation",
                        () -> assertNotNull(token.getToken(),   "Token string must not be null"),
                        () -> assertNotNull(token.getTtl(),     "TTL must not be null"),
                        () -> assertNotNull(token.getExpires(), "Expires must not be null"),
                        () -> assertNotNull(token.getIssuer(),  "Issuer must not be null")
                )
        );
    }

    @Test
    @Story("Token uniqueness")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("POST " + URL_TOKEN + " - Different users should receive different tokens")
    void POST_Token_ShouldReturnDifferentTokens_ForDifferentUsers() {
        Token adminToken = clientApi.generateAuthToken(prepareLoginRequest(Role.ADMIN));
        Token qaToken    = clientApi.generateAuthToken(prepareLoginRequest(Role.QA));

        attachJson("Admin token", adminToken);
        attachJson("QA token", qaToken);

        verify("Each user must receive a unique token", () ->
                assertNotEquals(adminToken.getToken(), qaToken.getToken(),
                        "Tokens for different users must be distinct")
        );
    }

    @Test
    @Story("Input validation")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("POST " + URL_TOKEN + " - Should return 400 when login is blank")
    void POST_Token_ShouldReturn400_WhenLoginIsBlank() {
        AuthRequest authRequest = prepareLoginRequest("", "any_password");

        var exception = assertThrows(ApiHttpStatusException.class,
                () -> clientApi.generateAuthToken(authRequest));
        attachJson("Error response", exception.getResponseBody());

        verify("A blank login must be rejected by bean validation, not by the service", () -> assertAll(
                () -> assertEquals(400, exception.getStatusCode(), "Blank login must give 400, not 401"),
                () -> assertTrue(exception.getResponseBody().contains("Invalid credentials or missing fields"),
                        "Body must carry the @NotBlank message, got: " + exception.getResponseBody())
        ));
    }

    @Test
    @Story("Input validation")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("POST " + URL_TOKEN + " - Should return 400 when password is blank")
    void POST_Token_ShouldReturn400_WhenPasswordIsBlank() {
        AuthRequest authRequest = prepareLoginRequest("any_login", "");

        var exception = assertThrows(ApiHttpStatusException.class,
                () -> clientApi.generateAuthToken(authRequest));
        attachJson("Error response", exception.getResponseBody());

        verify("A blank password must be rejected by bean validation, not by the service", () -> assertAll(
                () -> assertEquals(400, exception.getStatusCode(), "Blank password must give 400, not 401"),
                () -> assertTrue(exception.getResponseBody().contains("Invalid credentials or missing fields"),
                        "Body must carry the @NotBlank message, got: " + exception.getResponseBody())
        ));
    }
}