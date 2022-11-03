package in.divoc.api.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static in.divoc.api.authenticator.Constants.*;

public class MobileNumberAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {
	private static final Logger logger = Logger.getLogger(MobileNumberAuthenticator.class);
	private final OTPService mockOTPService;
	private final OTPService otpService;
	private final NotifyService notifyService;

	private static final String mockOtp = System.getenv().getOrDefault("MOCK_OTP", "true");

	public MobileNumberAuthenticator() {
		this.mockOTPService = new MockOTPServiceImpl();
		this.otpService = new OTPServiceImpl();
		this.notifyService = new NotifyService();
	}

	@Override
	public void action(AuthenticationFlowContext context) {
		System.err.println("action request ");
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		String type = formData.getFirst(FORM_TYPE);
		System.err.println("action request " + type);
		if (type.equals(LOGIN_FORM)) {
			//TODO: rename form id
			String mobileNumber = formData.getFirst(MOBILE_NUMBER);
			List<UserModel> users = context.getSession().users()
					.searchForUserByUserAttributeStream(context.getSession().getContext().getRealm(), MOBILE_NUMBER, mobileNumber).collect(Collectors.toList());
			if (users.size() > 0) {
				generateOTPAndNotify(context, mobileNumber, users);
			} else {
				users = context.getSession().users()
						.searchForUserByUserAttributeStream(context.getSession().getContext().getRealm(), EMAIL, mobileNumber).collect(Collectors.toList());
				if (users.size() > 0) {
					generateOTPAndNotify(context, mobileNumber, users);
				} else {
					context.failure(AuthenticationFlowError.INVALID_USER);
				}
			}
		} else if (type.equals(VERIFY_OTP_FORM)) {
			String sessionKey = context.getAuthenticationSession().getAuthNote(OTP);
			if (sessionKey != null) {
				String secret = formData.getFirst(OTP);
				if (secret != null) {
					if (secret.equals(sessionKey)) {
						context.success();
					} else {
						context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
					}
				} else {
					context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
				}
			} else {
				context.challenge(context.form().createForm(MOBILE_LOGIN_UI));
			}
		}
	}

	private void generateOTPAndNotify(AuthenticationFlowContext context, String mobileNumber, List<UserModel> users) {
		UserModel user = users.get(0);
		String otp;
		if (Boolean.parseBoolean(mockOtp)) {
			otp = mockOTPService.generateOTP();
		} else {
			otp = otpService.generateOTP();
		}
		new Thread(() -> {
			callNotify(user, otp, EMAIL, "mailto:");
			callNotify(user, otp, MOBILE_NUMBER, "tel:");
		}).start();
		context.getAuthenticationSession().setAuthNote(OTP, otp);
		context.setUser(user);
		context.challenge(context.form().createForm(VERIFY_OTP_UI));
	}

	private void callNotify(UserModel user, String otp, String key, String prefix) {
		List<String> mobileNumberNodes = user.getAttributes().getOrDefault(key, Collections.emptyList());
		if (mobileNumberNodes.size() > 0) {
			try {
				notifyService.notify(prefix + mobileNumberNodes.get(0), otp);
			} catch (IOException e) {
				logger.error("Failed to send notification to " + mobileNumberNodes.get(0));
			}
		}
	}

	@Override
	public void authenticate(AuthenticationFlowContext context) {
		context.challenge(context.form().createForm(MOBILE_LOGIN_UI));
	}

	@Override
	public boolean requiresUser() {
		return false;
	}

	@Override
	public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
		return true;
	}

	@Override
	public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
	}

	@Override
	public void close() {
	}

}