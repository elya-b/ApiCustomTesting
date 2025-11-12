package elya.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import elya.RestClientApiHelper;
import elya.constants.ApiEmulatorConstants;
import elya.objects.ApiEmulatorBankCard;
import elya.constants.HttpHeaders;
import elya.json2object.ApiBankCardSerializerDeserializer;
import elya.objects.Token;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class RestClientApi {

    private final IRestClientApiEngine engine;
    private final Gson gson;
    private final Type bankCardListType = new TypeToken<List<ApiEmulatorBankCard>>() {}.getType();

    public RestClientApi(IRestClientApiEngine engine, ApiBankCardSerializerDeserializer cardDeserializer) {
        this.engine = engine;

        this.gson = new GsonBuilder()
                .registerTypeAdapter(ApiEmulatorBankCard.class, cardDeserializer)
                .create();
    }

    public Token generateAuthToken(String login, String password) {
        JsonObject body = new JsonObject();
        body.addProperty("login", login);
        body.addProperty("password", password);

        JsonElement responseJson = engine.post(ApiEmulatorConstants.URL_TOKEN, body, Collections.emptyMap());

        if (responseJson != null) {
            return RestClientApiHelper.castFromJson(responseJson, Token.class);
        }

        log.error("Failed to generate token.");
        return null;
    }

    public List<ApiEmulatorBankCard> getApiBankCards(String token) {
        Map<String, String> headers = Map.of(HttpHeaders.AUTHORIZATION.getName(), token.startsWith("Bearer ") ? token : "Bearer " + token);

        JsonElement responseJson = engine.get(ApiEmulatorConstants.URL_BANK_CARD_DATA, headers);

        if (responseJson != null) {
            try {
                JsonElement cardsArray = responseJson.getAsJsonObject()
                        .getAsJsonObject("response")
                        .getAsJsonArray("cards");

                List<ApiEmulatorBankCard> cards = RestClientApiHelper.castListFromJson(cardsArray, bankCardListType, this.gson);

                if (cards == null) {
                    log.error("Failed to parse bank cards response: result is null or not an array.");
                    return Collections.emptyList();
                }
                return cards;
            } catch (Exception e) {
                log.error("Failed to parse bank cards response: unexpected JSON structure.", e);
                return Collections.emptyList();
            }
        }

        log.error("Failed to get bank cards data.");
        return Collections.emptyList();
    }

    public boolean setMockResponse(Map<String, Object> mockResponse) {
        JsonElement jsonBody = RestClientApiHelper.castToJson(mockResponse);

        if (engine.post(ApiEmulatorConstants.URL_BANK_CARD_MOCK_RESPONSE, jsonBody, Collections.emptyMap()) != null) {
            return true;
        }

        log.error("Failed to set mock response.");
        return false;
    }

    public boolean clearMockResponse() {
        if (engine.delete(ApiEmulatorConstants.URL_BANK_CARD_MOCK_RESPONSE)) {
            return true;
        }

        log.error("Failed to clear mock response.");
        return false;
    }
}
