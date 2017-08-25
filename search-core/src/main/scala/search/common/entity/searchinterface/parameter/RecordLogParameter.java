package search.common.entity.searchinterface.parameter;

/**
 * Created by soledede on 2016/4/18.
 */
public class RecordLogParameter {
    private String keyWords;
    private String appKey;
    private String clientIp;
    private String userAgent;
    private String sourceType;
    private String cookies;
    private String userId;

    public RecordLogParameter() {
    }

    public RecordLogParameter(String keyWords, String appKey, String clientIp, String userAgent, String sourceType, String cookies, String userId) {
        this.keyWords = keyWords;
        this.appKey = appKey;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.sourceType = sourceType;
        this.cookies = cookies;
        this.userId = userId;
    }

    public String getKeyWords() {
        return keyWords;
    }

    public void setKeyWords(String keyWords) {
        this.keyWords = keyWords;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
