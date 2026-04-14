package integration.auth;

import elya.authentication.Token;
import elya.constants.Role;
import elya.dto.auth.AuthRequest;
import elya.restclient.constants.logs.RestClientException;
import integration.AbstractApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static elya.constants.ApiEndpoints.URL_TOKEN;
import static elya.restclient.constants.logs.ExceptionMessage.GENERATE_TOKEN_EXCEPTION;
import static org.junit.jupiter.api.Assertions.*;

public class PostToken extends AbstractApiTest {

    @Test
    @DisplayName("POST " + URL_TOKEN + " - Should return 200 and token when credentials are valid")
    void POST_Token_ShouldReturnOkAndToken_WhenCredentialsAreValid() {
        AuthRequest authRequest = prepareLoginRequest(Role.QA);

        Token token = clientApi.generateAuthToken(authRequest);

        verify("Token should be generated successfully", () -> {
            assertAll("Token validation",
                    () -> assertEquals("3600", token.getTtl(), "Field TTL should be 3600"),
                    () -> assertNotNull(token.getToken(), "Token should not be null")
            );
        });
    }

    @Test
    @DisplayName("POST " + URL_TOKEN + " - Should throw RestClientException when credentials are invalid")
    void POST_Token_ShouldThrowException_WhenCredentialsAreInvalid() {
        AuthRequest authRequest = prepareLoginRequest("invalid_user", "invalid_pass");

        verify("Request should fail with RestClientException", () -> {
            var exception = assertThrows(RestClientException.class, () -> clientApi.generateAuthToken(authRequest));
            assertTrue(exception.getMessage().contains(GENERATE_TOKEN_EXCEPTION), "Error message mismatch");
        });
    }
}
