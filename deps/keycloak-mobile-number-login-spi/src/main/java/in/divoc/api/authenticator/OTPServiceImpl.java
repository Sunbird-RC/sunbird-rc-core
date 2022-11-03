package in.divoc.api.authenticator;

import org.jboss.logging.Logger;

import java.util.Random;

class OTPServiceImpl implements OTPService {
    private static final Logger logger = Logger.getLogger(OTPServiceImpl.class);

    public String generateOTP() {
        Random rand = new Random();
        return String.format("%04d", rand.nextInt(10000));
    }
}
