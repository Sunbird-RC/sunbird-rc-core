package in.divoc.api.authenticator;

public class ValidationService implements IValidation {
    @Override
    public boolean validate(String mobileNumber) {
        final String MOBILE_NUMBER_LENGTH = "MOBILE_NUMBER_LENGTH";
        return mobileNumber.length() == Integer.parseInt(System.getenv().getOrDefault(MOBILE_NUMBER_LENGTH, "10"));
    }
}
