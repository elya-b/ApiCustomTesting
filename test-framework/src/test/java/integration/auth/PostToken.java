package integration.auth;

import elya.allure.Priority;
import elya.allure.PriorityLevel;
import elya.authentication.Token;
import elya.constants.Role;
import elya.dto.auth.AuthRequest;
import elya.restclient.constants.logs.RestClientException;
import integration.AbstractApiTest;
import io.qameta.allure.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static elya.constants.ApiEndpoints.*;
import static elya.restclient.constants.logs.ExceptionMessage.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the POST /token endpoint (authentication).
 */
@Epic("Authentication API")
@Feature("POST /token — Generate Auth Token")
public class PostToken extends AbstractApiTest {

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
    @DisplayName("POST " + URL_TOKEN + " - Should throw RestClientException when credentials are invalid")
    void POST_Token_ShouldThrowException_WhenCredentialsAreInvalid() {
        AuthRequest authRequest = prepareLoginRequest("invalid_user", "invalid_pass");

        verify("Request should fail with RestClientException", () -> {
            var exception = assertThrows(RestClientException.class, () -> clientApi.generateAuthToken(authRequest));
            assertTrue(exception.getMessage().contains(GENERATE_TOKEN_EXCEPTION), "Error message mismatch");
        });
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
    void POST_Token_ShouldFail_WhenLoginIsBlank() {
        AuthRequest authRequest = prepareLoginRequest("", "any_password");

        verify("Blank login must be rejected", () ->
                assertThrows(RestClientException.class,
                        () -> clientApi.generateAuthToken(authRequest),
                        "Blank login must cause RestClientException")
        );
    }

    @Test
    @Story("Input validation")
    @Severity(SeverityLevel.NORMAL)
    @Priority(PriorityLevel.MEDIUM)
    @DisplayName("POST " + URL_TOKEN + " - Should return 400 when password is blank")
    void POST_Token_ShouldFail_WhenPasswordIsBlank() {
        AuthRequest authRequest = prepareLoginRequest("any_login", "");

        verify("Blank password must be rejected", () ->
                assertThrows(RestClientException.class,
                        () -> clientApi.generateAuthToken(authRequest),
                        "Blank password must cause RestClientException")
        );
    }
}