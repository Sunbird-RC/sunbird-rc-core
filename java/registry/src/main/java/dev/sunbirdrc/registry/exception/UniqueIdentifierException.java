package dev.sunbirdrc.registry.exception;

public class UniqueIdentifierException extends Exception{


    private static final long serialVersionUID = -6315798195661762883L;

    public UniqueIdentifierException(CustomException e) {
        super(e);
    }

    public class CreationException extends CustomException {
        private static final long serialVersionUID = 6174717850058203377L;

        public CreationException(String msg) {
            super("Unable to create unique ID: " + msg);
        }
    }

    public static class UnreachableException extends CustomException {

        private static final long serialVersionUID = 5384120386096139086L;

        public UnreachableException(String message) {
            super("Unable to reach id-gen service: " + message);
        }
    }

    public static class GenerateException extends CustomException {

        private static final long serialVersionUID = 8311355815972497247L;

        public GenerateException(String message) {
            super("Unable to generate id: " + message);
        }
    }

    public static class IdFormatException extends CustomException {

        private static final long serialVersionUID = 8311355815972497248L;

        public IdFormatException(String message) {
            super("Unable to save UniqueIdentifierField format: " + message);
        }
    }

    public static class FieldConfigNotFoundException extends CustomException {

        private static final long serialVersionUID = 8311355815972497249L;

        public FieldConfigNotFoundException(String message) {
            super("Unable to find UniqueIdentifierField configuration in schema configuration: " + message);
        }
    }
}