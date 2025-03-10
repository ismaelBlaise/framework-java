package exception;

public class CustomException extends Exception {

    private int errorCode;

    public CustomException(int errorCode) {
        super(); 
        this.errorCode = errorCode;
    }

    public CustomException(String message) {
        super(message); 
    }

    public CustomException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return super.toString() + " [Code d'erreur: " + errorCode + "]";
    }
}
