package elya.json2object;

import com.google.gson.*;
import elya.ApiBankCard;
import elya.card.BankCard;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;

@Component
public class ApiBankCardSerializerDeserializer extends SerializerDeserializerHelper implements JsonDeserializer<ApiBankCard> {

    private ApiBankCardSerializerDeserializer() {
        super(BankCard.class);
    }


    @Override
    public ApiBankCard deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement == null || jsonElement.isJsonNull() || !jsonElement.isJsonObject())
            return null;

        JsonObject object = jsonElement.getAsJsonObject();
        ApiBankCard bankCard = new ApiBankCard();

        if (isUsable(object, ApiBankCardField.CARD_ID.getFieldName())) {
            bankCard.setCardId(object.get(ApiBankCardField.CARD_ID.getFieldName()).getAsInt());
        }
        if (isUsable(object, ApiBankCardField.CARD_NUMBER.getFieldName())) {
            bankCard.setCardNumber(object.get(ApiBankCardField.CARD_NUMBER.getFieldName()).getAsInt());
        }
        if (isUsable(object, ApiBankCardField.CARD_TYPE.getFieldName())) {
            bankCard.setCardType(object.get(ApiBankCardField.CARD_TYPE.getFieldName()).getAsString());
        }
        if (isUsable(object, ApiBankCardField.CARD_STATUS.getFieldName())) {
            bankCard.setCardStatus(object.get(ApiBankCardField.CARD_STATUS.getFieldName()).getAsBoolean());
        }
        if (isUsable(object, ApiBankCardField.CURRENCY.getFieldName())) {
            bankCard.setCurrency(object.get(ApiBankCardField.CURRENCY.getFieldName()).getAsString());
        }
        if (isUsable(object, ApiBankCardField.BALANCE.getFieldName())) {
            bankCard.setBalance(object.get(ApiBankCardField.BALANCE.getFieldName()).getAsBigDecimal());
        }

        return bankCard;
    }
}
