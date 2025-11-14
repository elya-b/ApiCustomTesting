package elya.restclient.objects.token;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TokenData {
    @Expose
    @SerializedName("token")
    private String token;

    @Expose
    @SerializedName("ttl")
    private String ttl;

    @Expose
    @SerializedName("expires")
    private String expires;

    public String getToken() {
        return token;
    }

    public String getTtl() {
        return ttl;
    }

    public String getExpires() {
        return expires;
    }

    public TokenData(String token, String ttl, String expires) {
        this.token = token;
        this.ttl = ttl;
        this.expires = expires;
    }
}
