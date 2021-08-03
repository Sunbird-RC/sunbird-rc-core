package in.divoc.api.authenticator;

import org.jboss.logging.Logger;

import java.util.Random;

class OtpService {
    private static final Logger logger = Logger.getLogger(OtpService.class);

    String generateOTP(String mobileNumber) {
//        Random rand = new Random();
//        String otp = String.format("%04d", rand.nextInt(10000));
//        logger.infov("OTP {0} is generated for mobile number {1}", otp, mobileNumber);
        String otp = "1234";
        return otp;
    }
}
