package elya.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import elya.ApiBankCard;
import elya.RestClientApiHelper;
import elya.constants.ApiConstants;
import elya.json2object.ApiBankCardSerializerDeserializer;
import elya.objects.RestClientApiResponse;
import elya.objects.Token;
import lombok.Value;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RestClientApi {

    private final RestClientApiEngine engine;
    private final Gson gson;
    private final Type bankCardListType = new TypeToken<List<ApiBankCard>>() {}.getType();

    public RestClientApi(String baseUrl, ApiBankCardSerializerDeserializer cardDeserializer) {
        this.engine = new RestClientApiEngine(baseUrl);

        this.gson = new GsonBuilder()
                .registerTypeAdapter(ApiBankCard.class, cardDeserializer)
                .create();
    }

    public Token generateAuthToken(String login, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("login", login);
        body.addProperty("password", password);

        RestClientApiResponse response = engine.sendRequest("POST", ApiConstants.URL_TOKEN, body, Collections.emptyMap());

        if (response.isSuccessful() && response.getResponseAsJson() != null) {
            return this.gson.fromJson(response.getResponseAsJson(), Token.class);
        }

        log.error("Failed to generate token. Status: {}", response.getStatus().get("code"));
        return null;
    }

    public List<ApiBankCard> getApiBankCards(String token) {
        Map<String, String> headers = Map.of("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);

        RestClientApiResponse response = engine.sendRequest("GET", ApiConstants.URL_BANK_CARD_DATA, null, headers);

        if (response.isSuccessful() && response.getResponseAsJson() != null) {
            try {
                JsonElement cardsArray = response.getResponseAsJson()
                        .getAsJsonObject()
                        .getAsJsonObject("response")
                        .getAsJsonArray("cards");

                return gson.fromJson(cardsArray, bankCardListType);
            } catch (Exception e) {
                log.error("Failed to parse bank cards response: unexpected JSON structure.", e);
                return Collections.emptyList();
            }
        }

        log.error("Failed to get bank cards data. Status: {}", response.getStatus().get("code"));
        return Collections.emptyList();
    }

    public boolean setMockResponse(Map<String, Object> mockResponse) {
        JsonElement jsonBody = gson.toJsonTree(mockResponse);

        RestClientApiResponse response = engine.sendRequest("POST", ApiConstants.URL_BANK_CARD_MOCK_RESPONSE, jsonBody, Collections.emptyMap());

        if (response.isSuccessful()) {
            return true;
        }

        log.error("Failed to set mock response. Status: {}", response.getStatus().get("code"));
        return false;
    }

    public boolean clearMockResponse() {
        RestClientApiResponse response = engine.sendRequest("DELETE", ApiConstants.URL_BANK_CARD_MOCK_RESPONSE, null, Collections.emptyMap());

        if (response.isSuccessful()) {
            return true;
        }

        log.error("Failed to clear mock response. Status: {}", response.getStatus().get("code"));
        return false;
    }
}
