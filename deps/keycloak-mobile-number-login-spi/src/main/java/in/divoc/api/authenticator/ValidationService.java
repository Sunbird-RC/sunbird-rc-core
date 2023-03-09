package in.divoc.api.authenticator;

public class ValidationService implements IValidation {
    @Override
    public boolean validate(String mobileNumber) {
        return mobileNumber.length() == 10 || mobileNumber.length() == 14;
    }
}
