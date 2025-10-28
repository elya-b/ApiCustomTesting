package elya.objects;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Token {
    @Expose
    @SerializedName("Data")
    private TokenData data;

    @Expose
    @SerializedName("Success")
    private TokenData success;

    @Expose
    @SerializedName("Message")
    private TokenData message;

    public TokenData getData() {
        return data;
    }

    public TokenData getSuccess() {
        return success;
    }

    public TokenData getMessage() {
        return message;
    }
}
