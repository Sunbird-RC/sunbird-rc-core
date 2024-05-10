package dev.sunbirdrc.registry.exception;

public class IndexException extends Exception {

    private static final long serialVersionUID = -6315798195661762882L;

    public static class LabelNotFoundException extends CustomException {
        private static final long serialVersionUID = 6174717850058203376L;

        public LabelNotFoundException(String label) {
            super("Vertex label \"" + label + "\" doesn't exist");
        }
    }
}
