package elya.json2object;

import com.google.gson.*;
import elya.card.BankCard;
import elya.objects.ApiEmulatorBankCard;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;

import static elya.json2object.ApiBankCardField.*;

@Component
public class ApiBankCardSerializerDeserializer extends SerializerDeserializerHelper implements JsonDeserializer<ApiEmulatorBankCard> {

    private ApiBankCardSerializerDeserializer() {
        super(BankCard.class);
    }

    @Override
    public ApiEmulatorBankCard deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement == null || jsonElement.isJsonNull() || !jsonElement.isJsonObject())
            return null;

        JsonObject object = jsonElement.getAsJsonObject();
        ApiEmulatorBankCard bankCard = new ApiEmulatorBankCard();

        if (isUsable(object, CARD_ID.getFieldName())) {
            bankCard.setCardId(object.get(CARD_ID.getFieldName()).getAsInt());
        }
        if (isUsable(object, CARD_NUMBER.getFieldName())) {
            bankCard.setCardNumber(object.get(CARD_NUMBER.getFieldName()).getAsInt());
        }
        if (isUsable(object, CARD_TYPE.getFieldName())) {
            bankCard.setCardType(object.get(CARD_TYPE.getFieldName()).getAsString());
        }
        if (isUsable(object, CARD_STATUS.getFieldName())) {
            bankCard.setCardStatus(object.get(CARD_STATUS.getFieldName()).getAsBoolean());
        }
        if (isUsable(object, CURRENCY.getFieldName())) {
            bankCard.setCurrency(object.get(CURRENCY.getFieldName()).getAsString());
        }
        if (isUsable(object, BALANCE.getFieldName())) {
            bankCard.setBalance(object.get(BALANCE.getFieldName()).getAsBigDecimal());
        }

        return bankCard;
    }
}
