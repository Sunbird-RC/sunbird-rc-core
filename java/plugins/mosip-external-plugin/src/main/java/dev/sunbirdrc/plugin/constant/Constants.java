package dev.sunbirdrc.plugin.constant;

public class Constants {
    public static final String HUB_TOPIC = "hub.topic";
    public static final String HUB_MODE = "hub.mode";
    public static final String HUB_CALLBACK = "hub.callback";
    public static final String HUB_CHALLENGE = "hub.challenge";
    public static final String HUB_SECRET = "hub.secret";
    public static final String HUB_LEASE_SECONDS = "hub.lease_seconds";
    public static final String REGISTER = "register";
    public static final String SUBSCRIBE = "subscribe";
    public static final String REGISTER_URL = "/websub/publish";
    public static final String PRINT_PDF_URL = "/v1/print/print/callback/notifyPrint";
    public static final String SUBSCRIBE_URL = "/websub/hub";
    public static final String AUTH_URL = "/v1/authmanager/authenticate/clientidsecretkey";
    public static final String CREDENTIALS_URL = "/resident/v1/req/credential";
    public static final String OTP_URL = "/resident/v1/req/otp";
    public static final String LEASE_SECONDS = "86400";
    public static final String APP_ID = "appId";
    public static final String CLIENT_ID = "clientId";
    public static final String SECRET_KEY = "secretKey";
    public static final int SUBSCRIBE_RETRY_SECONDS = 1 * 60 * 60 * 1000;
    public static final int CACHE_TOKEN_SECONDS = 2 * 60 * 60 * 1000;
    public static final String MOSIP_IDENTITY_OTP_INTERNAL = "mosip.identity.otp.internal";
    public static final String UIN = "UIN";
    public static final String VERSION = "1.0";
    public static final String EMAIL = "EMAIL";
}
