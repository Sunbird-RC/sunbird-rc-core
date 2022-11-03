package in.divoc.api.authenticator;

import org.jboss.logging.Logger;

class MockOTPServiceImpl implements OTPService {
    private static final Logger logger = Logger.getLogger(MockOTPServiceImpl.class);

    public String generateOTP() {
        return System.getenv().getOrDefault("MOCK_OTP_VALUE", "1234");
    }
}
