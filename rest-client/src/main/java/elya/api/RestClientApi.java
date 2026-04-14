package elya.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import elya.RestClientApiHelper;
import elya.emulator.objects.ApiEmulatorBankCard;
import elya.json2object.ApiBankCardSerializerDeserializer;
import elya.restclient.objects.token.Token;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static elya.general.constants.ApiEndpoints.*;
import static elya.constants.logs.ErrorLogs.*;
import static elya.general.enums.HttpHeaderValues.*;
import static elya.general.enums.JsonProperty.*;
import static elya.general.enums.responsemodel.ApiBankCards.*;
import static org.springframework.http.HttpHeaders.*;

@Slf4j
public class RestClientApi {

    private final IRestClientApi engine;
    private final Gson gson;
    private final Type bankCardListType = new TypeToken<List<ApiEmulatorBankCard>>() {}.getType();

    public RestClientApi(IRestClientApi engine, ApiBankCardSerializerDeserializer cardDeserializer) {
        this.engine = engine;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(ApiEmulatorBankCard.class, cardDeserializer)
                .create();
    }

    public Token generateAuthToken(String login, String password) {
        JsonObject body = new JsonObject();
        body.addProperty(LOGIN.toString(), login);
        body.addProperty(PASSWORD.toString(), password);

        JsonElement responseJson = engine.post(URL_TOKEN, body, Collections.emptyMap());

        if (responseJson != null) {
            return RestClientApiHelper.castFromJson(responseJson, Token.class);
        }

        log.error(FAILED_TO_GENERATE_TOKEN);
        return null;
    }

    public List<ApiEmulatorBankCard> getApiBankCards(String token) {
        Map<String, String> headers = Map.of(
                AUTHORIZATION, token.startsWith(BEARER.toString()) ? token : BEARER + token);

        JsonElement responseJson = engine.get(URL_BANK_CARD_DATA, headers);

        if (responseJson != null) {
            try {
                JsonElement cardsArray = responseJson.getAsJsonObject()
                        .getAsJsonObject(RESPONSE.toString())
                        .getAsJsonArray(CARDS.toString());

                List<ApiEmulatorBankCard> cards = RestClientApiHelper.castListFromJson(cardsArray, bankCardListType, this.gson);

                if (cards == null) {
                    log.error(FAILED_TO_PARSE_BANK_CARDS_RESPONSE_NULL_OR_NOT_ARRAY);
                    return Collections.emptyList();
                }
                return cards;
            } catch (Exception e) {
                log.error(FAILED_TO_GENERATE_TOKEN_UNEXPECTED_JSON, e);
                return Collections.emptyList();
            }
        }

        log.error(FAILED_TO_GET_BANK_CARDS_DATA);
        return Collections.emptyList();
    }

    public boolean setMockResponse(Map<String, Object> mockResponse) {
        JsonElement jsonBody = RestClientApiHelper.castToJson(mockResponse);

        if (engine.post(URL_BANK_CARD_MOCK_RESPONSE, jsonBody, Collections.emptyMap()) != null) {
            return true;
        }

        log.error(FAILED_TO_SET_MOCK_RESPONSE);
        return false;
    }

    public boolean clearMockResponse() {
        if (engine.delete(URL_BANK_CARD_MOCK_RESPONSE)) {
            return true;
        }

        log.error(FAILED_TO_CLEAR_MOCK_RESPONSE);
        return false;
    }
}
