package in.divoc.api.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ErrorRepresentation;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
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
	private final IValidation iValidation;


	private static final String mockOtp = System.getenv().getOrDefault("MOCK_OTP", "true");

	public MobileNumberAuthenticator() {
		this.mockOTPService = new MockOTPServiceImpl();
		this.otpService = new OTPServiceImpl();
		this.notifyService = new NotifyService();
		this.iValidation = new ValidationService();
	}

	@Override
	public void action(AuthenticationFlowContext context) {
		System.err.println("action request ");
		MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
		String type = formData.getFirst(FORM_TYPE);
		System.err.println("action request " + type);
		if (type.equals(LOGIN_FORM)) {
			//TODO: rename form id
			String INVALID_REGISTRATION = "INVALID_REGISTRATION";
			String INVALID_USERNAME = "INVALID_USERNAME";
			String mobileNumber = formData.getFirst(MOBILE_NUMBER);
			if(iValidation.validate(mobileNumber)) {
				List<UserModel> users = context.getSession().users()
						.searchForUserByUserAttributeStream(context.getSession().getContext().getRealm(), MOBILE_NUMBER, mobileNumber).collect(Collectors.toList());
				if (users.size() > 0) {
					if (checkIfMaxResendOtpLimitReached(context)) return;
					generateOTPAndNotify(context, mobileNumber, users);
				} else {
					users = context.getSession().users()
							.searchForUserByUserAttributeStream(context.getSession().getContext().getRealm(), EMAIL, mobileNumber).collect(Collectors.toList());
					if (users.size() > 0) {
						generateOTPAndNotify(context, mobileNumber, users);
					} else {
						Response response = context.form().setError(System.getenv().getOrDefault(INVALID_REGISTRATION, "No user found with this username")).createForm(MOBILE_LOGIN_UI);
						context.failure(AuthenticationFlowError.INVALID_USER, response);
					}
				}
			}
			else {
				Response response = context.form().setError(System.getenv().getOrDefault(INVALID_USERNAME, "Please enter correct Username")).createForm(MOBILE_LOGIN_UI);
				context.failure(AuthenticationFlowError.INVALID_USER, response);
			}
		} else if (type.equals(VERIFY_OTP_FORM)) {
			String sessionKey = context.getAuthenticationSession().getAuthNote(OTP);
			if (sessionKey != null) {
				String secret = formData.getFirst(OTP);
				String VALID_OTP = "VALID_OTP";
				if (secret != null) {
					if (secret.equals(sessionKey)) {
						context.success();
					} else {
						Response response = context.form().setError(System.getenv().getOrDefault(VALID_OTP, "Please enter correct OTP")).createForm(VERIFY_OTP_UI);
						if(checkIfMaxOtpTriesReached(context)) {
							return;
						}
						context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, response);
					}
				} else {
					Response response = context.form().setError(System.getenv().getOrDefault(VALID_OTP, "Please enter correct OTP")).createForm(VERIFY_OTP_UI);
					if(checkIfMaxOtpTriesReached(context)) {
						return;
					}
					context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, response);
				}
			} else {
				context.challenge(context.form().createForm(MOBILE_LOGIN_UI));
			}
		}
	}

	private static boolean checkIfMaxResendOtpLimitReached(AuthenticationFlowContext context) {
		String MAX_RESEND_TRIES = "MAX_RESEND_TRIES";
		String RESEND_OTP_TRY_COUNT = "RESEND_OTP_TRY_COUNT";
		String resendTries = context.getAuthenticationSession().getAuthNote(RESEND_OTP_TRY_COUNT);
		System.out.println("RESEND RETRIES : " +  resendTries);
		int count = resendTries == null ? 0 : Integer.parseInt(resendTries);
		count++;
		if(count == Integer.parseInt(System.getenv().getOrDefault(MAX_RESEND_TRIES, "3")) + 1) {
			context.getAuthenticationSession().setAuthNote(RESEND_OTP_TRY_COUNT, null);
			context.failure(AuthenticationFlowError.INTERNAL_ERROR);
			return true;
		}
		context.getAuthenticationSession().setAuthNote(RESEND_OTP_TRY_COUNT, count + "" );
		return false;
	}

	private boolean checkIfMaxOtpTriesReached(AuthenticationFlowContext context) {
		String OTP_TRIES = "OTP_TRIES";
		String OTP_MAX_RETRY_LIMIT = "OTP_MAX_RETRY_LIMIT";
		String otpTries = context.getAuthenticationSession().getAuthNote(OTP_TRIES);
		System.out.println("OTP TRIES : " + otpTries);
		int count = (otpTries == null ? 0 : Integer.parseInt(otpTries));
		count++;
		int maxLimit = Integer.parseInt(System.getenv().getOrDefault(OTP_MAX_RETRY_LIMIT, "3"));
		if(count == maxLimit) {
			context.getAuthenticationSession().setAuthNote(OTP_TRIES, null);
			String MAX_RETRIES_LIMIT_MESSAGE = "MAX_RETRIES_LIMIT_MESSAGE";
			Response response = context.form().setError(System.getenv().getOrDefault(MAX_RETRIES_LIMIT_MESSAGE, "Max failed login limit reached")).createForm(MOBILE_LOGIN_UI);
			context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, response);
			return true;
		}
		context.getAuthenticationSession().setAuthNote(OTP_TRIES, "" + count);
		return false;
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