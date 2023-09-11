package dev.sunbirdrc.registry.exception;

public class UniqueIdentifierException extends Exception{


    private static final long serialVersionUID = -6315798195661762883L;

    public class CreationException extends CustomException {
        private static final long serialVersionUID = 6174717850058203377L;

        public CreationException(String msg) {
            super("Unable to create unique ID: " + msg);
        }
    }

    public class UnreachableException extends CustomException {

        private static final long serialVersionUID = 5384120386096139084L;

        public UnreachableException(String message) {
            super("Unable to reach id-gen service: " + message);
        }
    }

    public class KeyNotFoundException extends CustomException {

        private static final long serialVersionUID = 8311355815972497248L;

        public KeyNotFoundException(String message) {
            super("Unable to get key: " + message);
        }
    }
}
